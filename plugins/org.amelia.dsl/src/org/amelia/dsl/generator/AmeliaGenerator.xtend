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
import java.util.List
import org.amelia.dsl.amelia.AmeliaPackage
import org.amelia.dsl.outputconfiguration.AmeliaOutputConfigurationProvider
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.xtext.generator.IFileSystemAccess
import org.eclipse.xtext.generator.IGenerator
import org.eclipse.xtext.resource.IContainer
import org.eclipse.xtext.resource.IEObjectDescription
import org.eclipse.xtext.resource.impl.ResourceDescriptionsProvider
import org.amelia.dsl.lib.Subsystem
import org.amelia.dsl.lib.SubsystemGraph
import org.eclipse.emf.ecore.EObject
import org.eclipse.xtext.naming.IQualifiedNameProvider

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
		val subsystems = getEObjectDescriptions(resource, AmeliaPackage.eINSTANCE.subsystem)
		val s = Subsystem.canonicalName
		val content = '''
			package amelia;
			
			public class AmeliaMain {
				public static void main(String[] args) throws InterruptedException {
					«FOR subsystem : subsystems»
						«s» «subsystem.qualifiedName.toString("_")» = new «s»("«subsystem.qualifiedName.toString»", new «subsystem.qualifiedName»());
					«ENDFOR»
					«FOR subsystem : subsystems»
						«val eObject = subsystem.getEObject(resource) as org.amelia.dsl.amelia.Subsystem»
						«IF eObject != null && eObject.includes != null»
							«val includes = eObject.includes.includeDeclarations.map[ i |
								(i.includedType as org.amelia.dsl.amelia.Subsystem).fullyQualifiedName.toString("_")
							]»
							«subsystem.qualifiedName.toString("_")».dependsOn(«includes.join(", ")»);
						«ENDIF»
					«ENDFOR»
					«SubsystemGraph.canonicalName» graph = «SubsystemGraph.canonicalName».getInstance();
					graph.addSubsystems(«subsystems.map[r|r.qualifiedName.toString("_")].join(", ")»);
					graph.execute(true);
				}
			}
		'''
		
		fsa.generateFile("amelia/AmeliaMain.java", AmeliaOutputConfigurationProvider::AMELIA_OUTPUT, content)
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
