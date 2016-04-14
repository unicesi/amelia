/*
 * Copyright © 2015 Universidad Icesi
 * 
 * This file is part of the Amelia DSL.
 * 
 * The Amelia DSL is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * The Amelia DSL is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with The Amelia DSL. If not, see <http://www.gnu.org/licenses/>.
 */
package org.amelia.dsl.generator

import com.google.inject.Inject
import java.util.HashMap
import java.util.List
import org.amelia.dsl.amelia.AmeliaPackage
import org.amelia.dsl.amelia.DependDeclaration
import org.amelia.dsl.amelia.Subsystem
import org.amelia.dsl.lib.Subsystem.Deployment
import org.amelia.dsl.lib.SubsystemGraph
import org.amelia.dsl.outputconfiguration.AmeliaOutputConfigurationProvider
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.xtext.generator.IFileSystemAccess
import org.eclipse.xtext.generator.IGenerator
import org.eclipse.xtext.naming.IQualifiedNameProvider
import org.eclipse.xtext.resource.IContainer
import org.eclipse.xtext.resource.IEObjectDescription
import org.eclipse.xtext.resource.impl.ResourceDescriptionsProvider
import org.eclipse.xtext.xbase.lib.Functions.Function0
import org.amelia.dsl.amelia.DeploymentDeclaration

/**
 * Generates code from your model files on save.
 * 
 * See https://www.eclipse.org/Xtext/documentation/303_runtime_concepts.html#code-generation
 * 
 * @author Miguel Jiménez - Initial contribution and API
 */
class AmeliaGenerator implements IGenerator {
	
	@Inject ResourceDescriptionsProvider resourceDescriptionsProvider
	
	@Inject IContainer.Manager containerManager
	
	@Inject extension IQualifiedNameProvider

	override void doGenerate(Resource resource, IFileSystemAccess fsa) {
		val subsystemDescriptions = getEObjectDescriptions(resource, AmeliaPackage.eINSTANCE.subsystem)
		val subsystems = subsystemDescriptions.map[d|d.getEObject(resource) as Subsystem].filter[e|!e.fragment]
//		val mainDescriptions = getEObjectDescriptions(resource, AmeliaPackage.eINSTANCE.mainDeclaration)
//		val mains = mainDescriptions.map[d|d.getEObject(resource) as MainDeclaration]
//		if (!mains.empty) {
//			for (MainDeclaration main : mains) {
//				val fqn = ResourceUtils.fromURItoFQN(main.eResource.URI)
//				val content = getCustomImplementation(main, fqn, subsystems)
//				fsa.generateFile(fqn.replaceAll("\\.", "/") + ".java", AmeliaOutputConfigurationProvider::AMELIA_OUTPUT, content)
//			}
//		} else {			
//			val content = getDefaultMainImpl(subsystems)
//			fsa.generateFile("Amelia.java", AmeliaOutputConfigurationProvider::AMELIA_OUTPUT, content)
//		}
		val content = getDefaultMainImpl(subsystems)
		fsa.generateFile("Amelia.java", AmeliaOutputConfigurationProvider::AMELIA_OUTPUT, content)
	}
	
	def getCustomImplementation(DeploymentDeclaration deployment, String fqn, Iterable<Subsystem> subsystems) {
		val className = if(fqn.contains(".")) fqn.substring(fqn.lastIndexOf(".")) else fqn
		val ss = org.amelia.dsl.lib.Subsystem.canonicalName
		val hm = HashMap.canonicalName
		val dm = Deployment.canonicalName
		val suffix = System.nanoTime + ""
		'''
			public class «className» {
				
				private «hm»<String, «ss»> subsystems«suffix» 
					= new «hm»<String, «ss»>();
				private «hm»<Class<? extends «dm»>, «Function0.canonicalName»<? extends «dm»>> initializers«suffix» 
					= new «hm»<Class<? extends «dm»>, «Function0.canonicalName»<? extends «dm»>>();
				
				public static void main(final String[] args«suffix») throws Exception {
						«className» main = new «className»();
						main.custom();
				}
				
				public void custom() throws Exception {
					
				}
				
				private void map(Class<? extends «dm»> clazz, «Function0.canonicalName»<? extends «dm»> initializer) {
					initializers«suffix».put(clazz, initializer);
				}
				
				private boolean deploy(final boolean stopExecutedComponents) throws InterruptedException {
					«SubsystemGraph.canonicalName» graph = «SubsystemGraph.canonicalName».getInstance();
					for (Class<? extends «dm»> clazz : initializers«suffix».keySet()) {
						«ss» subsystem = new «ss»(clazz.getCanonicalName(), initializers«suffix».get(clazz).apply());
						subsystems«suffix».put(clazz.getCanonicalName(), subsystem);
					}
					«FOR subsystem : subsystems»
						«val dependencies = subsystem.extensions.declarations.filter(DependDeclaration)»
						«IF !dependencies.empty»
							subsystems«suffix».get("org.example.Client").dependsOn(subsystems«suffix».get("org.example.Server"));
							«dependencies.join(
								'''subsystems«suffix».get("«subsystem.fullyQualifiedName»").dependsOn(''',
								", ",
								");",
								[s|'''subsystems.get("«s.fullyQualifiedName»")''']
							)»
						«ENDIF»
					«ENDFOR»
					«subsystems.join("graph.addSubsystems(", ", ", ");", [s|'''subsystems«suffix».get("«s.fullyQualifiedName»")'''])»
					return graph.execute(stopExecutedComponents);
				}
			}
		'''
	}
	
	def getDefaultMainImpl(Iterable<Subsystem> subsystems) {
		val s = org.amelia.dsl.lib.Subsystem.canonicalName
		'''
			public class Amelia {
				public static void main(String[] args) throws InterruptedException {
					«FOR subsystem : subsystems»
						«s» «subsystem.fullyQualifiedName.toString("_")» = new «s»("«subsystem.fullyQualifiedName»", new «subsystem.fullyQualifiedName»());
					«ENDFOR»
					«FOR subsystem : subsystems»
						«IF subsystem != null && subsystem.extensions != null»
							«val dependencies = subsystem.extensions.declarations.filter(DependDeclaration).map[ i |
								if (i.element instanceof Subsystem) (i.element as Subsystem).fullyQualifiedName.toString("_")
							]»
							«IF !subsystem.extensions.declarations.filter(DependDeclaration).empty»
								«subsystem.fullyQualifiedName.toString("_")».dependsOn(«dependencies.join(", ")»);
							«ENDIF»
						«ENDIF»
					«ENDFOR»
					«SubsystemGraph.canonicalName» graph = «SubsystemGraph.canonicalName».getInstance();
					graph.addSubsystems(«subsystems.map[r|r.fullyQualifiedName.toString("_")].join(", ")»);
					graph.execute(true);
				}
			}
		'''
	}
	
	def List<IEObjectDescription> getEObjectDescriptions(Resource resource, EClass eClass) {
	    val descriptions = newArrayList;
	    val resourceDescriptions = resourceDescriptionsProvider.getResourceDescriptions(resource);
	    val resourceDescription = resourceDescriptions.getResourceDescription(resource.getURI());
	    for (c : containerManager.getVisibleContainers(resourceDescription, resourceDescriptions)) {
	        for (ob : c.getExportedObjectsByType(eClass)) {
	            descriptions.add(ob);
	        }
	    }
	    return descriptions;
	}
	
	def EObject getEObject(IEObjectDescription description, Resource resource) {
		val resourceSet = resource.getResourceSet()
		return resourceSet.getEObject(description.getEObjectURI(), true)
	}
}
