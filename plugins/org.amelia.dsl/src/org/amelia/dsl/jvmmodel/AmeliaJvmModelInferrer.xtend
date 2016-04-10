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
package org.amelia.dsl.jvmmodel

import com.google.inject.Inject
import java.util.List
import org.amelia.dsl.amelia.IncludeDeclaration
import org.amelia.dsl.amelia.OnHostBlockExpression
import org.amelia.dsl.amelia.RuleDeclaration
import org.amelia.dsl.amelia.Subsystem
import org.amelia.dsl.amelia.VariableDeclaration
import org.amelia.dsl.lib.DescriptorGraph
import org.amelia.dsl.lib.descriptors.CommandDescriptor
import org.amelia.dsl.lib.descriptors.Host
import org.amelia.dsl.lib.util.Arrays
import org.amelia.dsl.outputconfiguration.AmeliaOutputConfigurationProvider
import org.amelia.dsl.outputconfiguration.OutputConfigurationAdapter
import org.eclipse.xtext.common.types.JvmVisibility
import org.eclipse.xtext.naming.IQualifiedNameProvider
import org.eclipse.xtext.naming.QualifiedName
import org.eclipse.xtext.xbase.compiler.output.ITreeAppendable
import org.eclipse.xtext.xbase.jvmmodel.AbstractModelInferrer
import org.eclipse.xtext.xbase.jvmmodel.IJvmDeclaredTypeAcceptor
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1
import org.eclipse.xtext.common.types.JvmField
import org.amelia.dsl.amelia.ConfigBlockExpression
import java.util.ArrayList

/**
 * <p>Infers a JVM model from the source model.</p> 
 * 
 * <p>The JVM model should contain all elements that would appear in the Java code 
 * which is generated from the source model. Other models link against the JVM model rather than the source model.</p>
 * 
 * @author Miguel Jiménez - Initial contribution and API
 */
class AmeliaJvmModelInferrer extends AbstractModelInferrer {

	/**
	 * convenience API to build and initialize JVM types and their members.
	 */
	@Inject extension JvmTypesBuilder

	@Inject extension IQualifiedNameProvider

	def dispatch void infer(Subsystem subsystem, IJvmDeclaredTypeAcceptor acceptor, boolean isPreIndexingPhase) {
		val clazz = subsystem.toClass(subsystem.fullyQualifiedName)
		if (clazz == null)
			return;
		clazz.eAdapters.add(new OutputConfigurationAdapter(AmeliaOutputConfigurationProvider::AMELIA_OUTPUT))
		acceptor.accept(clazz) [
			if (!isPreIndexingPhase) {
				val suffix = System.nanoTime + ""
				val params = newArrayList
				val fields = newArrayList
				val constructors = newArrayList
				val methods = newArrayList
				val getters = newArrayList
				var currentHostBlock = 0
				val hasConfigBlock = newArrayList(false)
				
				documentation = subsystem.documentation
				if (!subsystem.fragment)
					superTypes += typeRef(org.amelia.dsl.lib.Subsystem.Deployment)
				
				for (e : subsystem.body.expressions) {
					switch (e) {
						VariableDeclaration: {
							// Transform variable declarations into fields
							fields += e.toField(e.name, e.type ?: inferredType) [
								documentation = e.documentation
								initializer = e.right
								if (e.param) {
									final = !e.writeable && e.right == null
								} else {
									final = !e.writeable && e.right != null
								}
							]
							if (e.param)
								params += e
						}
						ConfigBlockExpression: {
							// There is only one configuration block
							hasConfigBlock.add(0, true)
							methods += e.toMethod("configure", typeRef(void)) [
								exceptions += typeRef(Exception)
								parameters +=
									e.toParameter("dependencies", typeRef(List, typeRef(org.amelia.dsl.lib.Subsystem)))
								body = e
							]
						}
						OnHostBlockExpression: {
							// Transform rules inside on-host blocks into array fields
							for (rule : e.rules) {
								fields += rule.toField(rule.name, typeRef(CommandDescriptor).addArrayTypeDimension) [
									initializer = '''new «CommandDescriptor»[«rule.commands.length»]'''
									final = true
									visibility = JvmVisibility.PUBLIC
								]
								var currentCommand = 0
								for (command : rule.commands) {
									getters += rule.toMethod("init" + rule.name.toFirstUpper + currentCommand++, typeRef(CommandDescriptor)) [
										visibility = JvmVisibility::PRIVATE
										body = command
									]
								}
							}
							
							// Helper methods. Replace this when Xtext allows to compile XExpressions in specific places
							var currentHost = 0
							for (host : e.hosts) {
								getters += host.toMethod("getHost" + currentHostBlock + currentHost++, typeRef(Host)) [
									body = host
								]
							}
							currentHostBlock++
						}
					}
				}
				// Transform fragment includes into fields (recursive)
				fields += getIncludesAsFields(subsystem, suffix)

				// Setup rules' commands
				if (subsystem.fragment) {
					constructors += subsystem.toConstructor[
						body = subsystem.setup(null, suffix)
					]
				} else {
					// Empty constructor to avoid compilation errors in the default (Amelia) main when there are parameters
					if (!params.empty) {
						constructors += subsystem.toConstructor[
							if (hasConfigBlock.get(0)) {
								body = [
									append('''this.dependencies«suffix» = new ''')
									append(ArrayList).append("<").append(org.amelia.dsl.lib.Subsystem).append(">();")
								]
							}
						]
						constructors += subsystem.toConstructor [
							for (param : params) {
								if (param.type != null || param.right != null)
									parameters += param.toParameter(param.name, param.type ?: param.right.inferredType)
							}
							body = [
								append("this();").newLine
								for (param : params)
									append('''this.«param.name» = «param.name»;''')
							]
						]
					}
					methods += subsystem.toMethod("deploy", typeRef(void)) [
						val subsystemParam = "subsystem"
						val dependenciesParam = "dependencies" + suffix
						exceptions += typeRef(Exception)
						parameters += subsystem.toParameter(subsystemParam, typeRef(String))
						parameters +=
							subsystem.toParameter(dependenciesParam,
								typeRef(List, typeRef(org.amelia.dsl.lib.Subsystem)))
						body = subsystem.setup(subsystemParam, suffix)
					]
				}
				
				// Method to return all rules from included subsystems
				getters += subsystem.toMethod("getAllRules", typeRef(CommandDescriptor).addArrayTypeDimension) [
					body = [
						val rules = subsystem.body.expressions.filter(OnHostBlockExpression).map[h|h.rules].flatten.map [r|
							r.name
						].toList
						if (subsystem.extensions != null) {
							val includes = subsystem.extensions.declarations.filter(IncludeDeclaration).map [d|
								d.element as Subsystem
							]
							for (var i = 0; i < includes.size; i++) {
								val includedSubsystem = includes.get(i)
								rules += '''«includedSubsystem.fullyQualifiedName.toString("_")»«suffix».getAllRules()'''
							}
						}
						append("return ").append(Arrays).append(".concatAll(").increaseIndentation.newLine
						append(rules.join(",\n")).decreaseIndentation.newLine
						append(");")
					]
				]
				// Wrapper methods to hide the internal implementation
				if (hasConfigBlock.get(0)) {
					fields +=
						subsystem.toField("dependencies" + suffix, typeRef(List, typeRef(org.amelia.dsl.lib.Subsystem)))
					methods += subsystem.toMethod("execute", typeRef(void)) [
						exceptions += typeRef(Exception)
						parameters += subsystem.toParameter("stopExecutionsWhenFinish", typeRef(boolean))
						body = '''super.graph.execute(stopExecutionsWhenFinish);'''
					]
					methods += subsystem.toMethod("execute", typeRef(void)) [
						exceptions += typeRef(Exception)
						parameters += subsystem.toParameter("stopPreviousExecutions", typeRef(boolean))
						parameters += subsystem.toParameter("shutdownAfterDeployment", typeRef(boolean))
						parameters += subsystem.toParameter("stopExecutionsWhenFinish", typeRef(boolean))
						body = '''
							super.graph.execute(
								shutdownAfterDeployment, 
								shutdownAfterDeployment, 
								stopExecutionsWhenFinish);
						'''
					]
					methods += subsystem.toMethod("release", typeRef(void)) [
						parameters +=
							subsystem.toParameter("clazz", typeRef(Class, typeRef(org.amelia.dsl.lib.Subsystem)))
						parameters += subsystem.toParameter("compositeNames", typeRef(List, typeRef(String)))
						body = [
							append("String fqn = clazz.getClass().getCanonicalName();").newLine
							append(org.amelia.dsl.lib.Subsystem).append(" dependency = null;").newLine
							append('''
								for (Subsystem subsystem : this.dependencies«suffix») {
									if (subsystem.alias().equals(fqn)) {
										dependency = subsystem;
										break;
									}
								}
								if (dependency != null) {
									releaseDependency(dependency, compositeNames.toArray(new String[0]));
								} else {
									throw new IllegalArgumentException("Subsystem " + fqn + " is not a dependency");
								}
							''')
						]
					]
				}
				// Add class members
				members += fields
				members += constructors
				members += methods
				members += getters
			}
		]
	}
	
	def Procedure1<ITreeAppendable> setup(Subsystem subsystem, String subsystemParam, String suffix) {
		return [
			var currentHostBlock = 0
			for (hostBlock : subsystem.body.expressions.filter(OnHostBlockExpression)) {
				append(Host).append('''[] hosts«currentHostBlock» = { ''')
				for (var currentHost = 0; currentHost < hostBlock.hosts.length; currentHost++) {
					append("getHost" + currentHostBlock + currentHost + "()")
					if (currentHost < hostBlock.hosts.length - 1)
						append(", ")
				}
				append(" };")
				newLine
				for (rule : hostBlock.rules) {
					var currentCommand = 0
					for (command : rule.commands) {
						append('''«rule.name.toString»[«currentCommand»]''')
						append(''' = init«rule.name.toFirstUpper»«currentCommand»();''').newLine
						append('''«rule.name»[«currentCommand»]''')
						append('''.runsOn(hosts«currentHostBlock»);''').newLine
						if (currentCommand == 0 && !rule.dependencies.empty) {
							val dependencies = newArrayList
							for (dependency : rule.dependencies) {
								dependencies += '''«dependency.getAccessName(subsystem, suffix)»[«dependency.commands.length - 1»]'''
							}
							append('''«rule.name»[«currentCommand»].dependsOn(«dependencies.join(", ")»);''')
							newLine
						} else if (currentCommand > 0) {
							append('''«rule.name»[«currentCommand»].dependsOn(«rule.getAccessName(subsystem, suffix)»[«(currentCommand - 1)»]);''')
							newLine
						}
						currentCommand++
					}
				}
				currentHostBlock++
			}
			
			if (!subsystem.fragment) {
				val rules = subsystem.getAllIncludedRules(suffix)
				val hasConfigBlock = subsystem.body.expressions.exists[c|c instanceof ConfigBlockExpression]
				append('''super.graph = new ''').append(DescriptorGraph).append('''(«subsystemParam»);''').newLine
				append('''super.graph.addDescriptors(getAllRules());''').newLine
				if (hasConfigBlock) {
					append('''this.dependencies«suffix» = dependencies«suffix»;''').newLine
					append('''configure(dependencies«suffix»);''')
				} else if (!rules.empty) {
					append('''super.graph.execute(true);''')
				}
			}
		]
	}
	
	def List<JvmField> getIncludesAsFields(Subsystem subsystem, String suffix) {
		val members = newArrayList
		if (subsystem.extensions != null) {
			for (include : subsystem.extensions.declarations.filter(IncludeDeclaration)) {
				if (include.element instanceof Subsystem) {
					val includedSubsystem = include.element as Subsystem
					val fqn = includedSubsystem.fullyQualifiedName
					members += includedSubsystem.toField(fqn.toString("_") + suffix, typeRef(fqn.toString)) [
						initializer = '''new «fqn»()'''
						final = true
					]
				}
			}
		}
		return members
	}
	
	def List<RuleDeclaration> getAllIncludedRules(Subsystem subsystem, String suffix) {
		val rules = subsystem.body.expressions.filter(OnHostBlockExpression).map[h|h.rules].flatten.toList
		if (subsystem.extensions != null) {
			val includes = subsystem.extensions.declarations.filter(IncludeDeclaration).map [ d |
				d.element as Subsystem
			]
			for (fragment : includes)
				rules += fragment.getAllIncludedRules(suffix)
		}
		return rules
	}
	
	def getAccessName(RuleDeclaration rule, Subsystem subsystem, String suffix) {
		val segments = rule.fullyQualifiedName.segments
		val containerFQN = QualifiedName.create(segments.subList(0, segments.length - 1))
		var accessName = rule.name
		if (!containerFQN.equals(subsystem.fullyQualifiedName)) {
			accessName = containerFQN.toString("_") + suffix + "." + rule.name
		}
		return accessName
	}

}
