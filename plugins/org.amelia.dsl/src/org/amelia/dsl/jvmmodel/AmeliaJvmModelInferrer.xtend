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

import com.google.common.collect.Lists
import com.google.inject.Inject
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.List
import org.amelia.dsl.amelia.ConfigBlockExpression
import org.amelia.dsl.amelia.DependDeclaration
import org.amelia.dsl.amelia.DeploymentDeclaration
import org.amelia.dsl.amelia.IncludeDeclaration
import org.amelia.dsl.amelia.OnHostBlockExpression
import org.amelia.dsl.amelia.RuleDeclaration
import org.amelia.dsl.amelia.VariableDeclaration
import org.amelia.dsl.lib.DescriptorGraph
import org.amelia.dsl.lib.Subsystem
import org.amelia.dsl.lib.Subsystem.Deployment
import org.amelia.dsl.lib.SubsystemGraph
import org.amelia.dsl.lib.descriptors.CommandDescriptor
import org.amelia.dsl.lib.descriptors.Host
import org.amelia.dsl.lib.util.Arrays
import org.amelia.dsl.outputconfiguration.AmeliaOutputConfigurationProvider
import org.amelia.dsl.outputconfiguration.OutputConfigurationAdapter
import org.eclipse.xtext.common.types.JvmField
import org.eclipse.xtext.common.types.JvmVisibility
import org.eclipse.xtext.naming.IQualifiedNameProvider
import org.eclipse.xtext.naming.QualifiedName
import org.eclipse.xtext.xbase.compiler.output.ITreeAppendable
import org.eclipse.xtext.xbase.jvmmodel.AbstractModelInferrer
import org.eclipse.xtext.xbase.jvmmodel.IJvmDeclaredTypeAcceptor
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1

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
	
	static val prefix = "＿"

	def dispatch void infer(DeploymentDeclaration deployment, IJvmDeclaredTypeAcceptor acceptor, boolean isPreIndexingPhase) {
		val clazz = deployment.toClass(deployment.fullyQualifiedName)
		clazz.eAdapters.add(new OutputConfigurationAdapter(AmeliaOutputConfigurationProvider::AMELIA_OUTPUT))
		acceptor.accept(clazz) [
			if (!isPreIndexingPhase) {
				val subsystems = if(deployment.extensions != null) 
						deployment.extensions.declarations.filter(IncludeDeclaration).map [d|
							d.element as org.amelia.dsl.amelia.Subsystem
						]
					else
						Collections.EMPTY_LIST
				documentation = deployment.documentation
				members +=
					deployment.toField(AmeliaJvmModelInferrer.prefix + "subsystems",
						typeRef(HashMap, typeRef(String), typeRef(Subsystem))) [
					visibility = JvmVisibility.PRIVATE
					initializer = [
						trace(deployment)
							.append("new ").append(HashMap).append("<").append(String).append(", ")
							.append(Subsystem).append(">()")
					]
				]
				members += deployment.toMethod("main", typeRef(void)) [
					static = true
					parameters += deployment.toParameter("args", typeRef(String).addArrayTypeDimension)
					exceptions += typeRef(Exception)
					body = [
						append(clazz).append(" main = new ").append(clazz).append("();").newLine
						append("main.init();").newLine
						append("main.custom();")
					]
				]
				members += deployment.toMethod("custom", typeRef(void)) [
					visibility = JvmVisibility.PRIVATE
					body = deployment.body
				]
				members += deployment.toMethod("set", typeRef(void)) [
					visibility = JvmVisibility.PRIVATE
					parameters += deployment.toParameter("deployment", typeRef(Deployment))
					body = [
						trace(deployment)
							.append("String clazz = deployment.getClass().getCanonicalName();")
							.newLine
							.append('''if («AmeliaJvmModelInferrer.prefix»subsystems.get(clazz) != null) {''')
							.increaseIndentation.newLine
						append('''«AmeliaJvmModelInferrer.prefix»subsystems.put(clazz, new ''')
							.append(Subsystem).append('''(clazz, deployment));''')
						trace(deployment)
							.decreaseIndentation.newLine
							.append('''
							} else {
								throw new RuntimeException("Subsystem '" + clazz + "' has not been included in " + 
									"deployment '«deployment.fullyQualifiedName»'");
							}''')
					]
				]
				members += deployment.toMethod("init", typeRef(void)) [
					visibility = JvmVisibility.PRIVATE
					exceptions += typeRef(Exception)
					body = [
						for (subsystem : subsystems) {
							val qualifiedName = subsystem.fullyQualifiedName
							trace(deployment)
								.append('''«AmeliaJvmModelInferrer.prefix»subsystems.put("«qualifiedName»", new ''')
								.append(Subsystem)
								.append('''("«qualifiedName»", «qualifiedName».class.newInstance()));''')
							if (!subsystems.last.equals(subsystem))
								trace(deployment).newLine
						}
					]
				]
				members += deployment.toMethod("deploy", typeRef(boolean)) [
					visibility = JvmVisibility.PRIVATE
					parameters += deployment.toParameter("stopExecutedComponents", typeRef(boolean))
					exceptions += typeRef(Exception)
					body = [
						trace(deployment)
							.append(SubsystemGraph).append(" graph = ").append(SubsystemGraph).append(".getInstance();").newLine
							.append('''
								«FOR subsystem : subsystems»
									«val dependencies = 
										if(subsystem.extensions != null)
											subsystem.extensions.declarations.filter(DependDeclaration)
										else
											Collections.EMPTY_LIST
									»
									«IF !dependencies.empty»
										«dependencies.join(
											'''«AmeliaJvmModelInferrer.prefix»subsystems.get("«subsystem.fullyQualifiedName»").dependsOn(
											''',
											",\n",
											"\n);",
											[d|'''	«AmeliaJvmModelInferrer.prefix»subsystems.get("«d.element.fullyQualifiedName»")''']
										)»
									«ENDIF»
								«ENDFOR»
							''')
					    	.append("for (").append(Subsystem).append(''' subsystem : «AmeliaJvmModelInferrer.prefix»subsystems.values()) {''')
					    	.increaseIndentation.newLine
					    	.append("subsystem.deployment().setup();")
					    	.decreaseIndentation.newLine
					    	.append("}").newLine
							.append('''graph.addSubsystems(«AmeliaJvmModelInferrer.prefix»subsystems.values().toArray(new ''')
							.append(Subsystem).append("[0]));").newLine
						append("boolean successful = graph.execute(stopExecutedComponents);")
						trace(deployment)
							.newLine.append("return successful;")
					]
				]
			}
		]
	}

	def dispatch void infer(org.amelia.dsl.amelia.Subsystem subsystem, IJvmDeclaredTypeAcceptor acceptor, boolean isPreIndexingPhase) {
		val clazz = subsystem.toClass(subsystem.fullyQualifiedName)
		if (clazz == null)
			return;
		clazz.eAdapters.add(new OutputConfigurationAdapter(AmeliaOutputConfigurationProvider::AMELIA_OUTPUT))
		acceptor.accept(clazz) [
			if (!isPreIndexingPhase) {
				val subsystemParam = AmeliaJvmModelInferrer.prefix + "subsystem"
				val dependenciesParam = AmeliaJvmModelInferrer.prefix + "dependencies"
				val includedSubsystems = if (subsystem.extensions != null)
						subsystem.extensions.declarations.filter(IncludeDeclaration).map [ i |
							if (i.element instanceof org.amelia.dsl.amelia.Subsystem)
								i.element as org.amelia.dsl.amelia.Subsystem
						]
					else
						Collections.EMPTY_LIST
				val params = newArrayList
				val includedParams = newArrayList
				val _parameters = newArrayList
				val fields = newArrayList
				val constructors = newArrayList
				val methods = newArrayList
				val getters = newArrayList
				var currentHostBlock = 0
				val hasConfigBlock = newArrayList(false)
				
				documentation = subsystem.documentation
				superTypes += typeRef(Subsystem.Deployment)
				
				if (subsystem.extensions != null) {
					includedParams += includedSubsystems.map [ s |
						s.body.expressions.filter(VariableDeclaration).filter[v|v.param]
					].flatten
				}
				
				// Transform includes into fields (recursive)
				fields += getIncludesAsFields(subsystem)
				
				// Included parameters as getters
				for (includedSubsystem : includedSubsystems) {
					val _params = includedSubsystem.body.expressions.filter(VariableDeclaration).filter[v|v.param]
					val fqn = includedSubsystem.fullyQualifiedName.toString("_")
					for (e : _params) {
						getters += e.toMethod("get" + e.name.toFirstUpper, e.type ?: inferredType) [
							body = '''return this.«prefix»«fqn».get«e.name.toFirstUpper»();'''
						]
					}
				}
				
				for (e : subsystem.body.expressions) {
					switch (e) {
						VariableDeclaration: {
							// Transform variable declarations into fields
							_parameters += e.toField(e.name, e.type ?: inferredType) [
								documentation = e.documentation
							]
							if (e.right != null) {
								getters += e.toMethod("init" + e.name.toFirstUpper, e.type ?: inferredType) [
									visibility = JvmVisibility.PRIVATE
									body = e.right
								]								
							}
							if (e.param) {
								params += e
								getters += e.toGetter(e.name, e.type ?: inferredType)
							}
						}
						ConfigBlockExpression: {
							// There is only one configuration block
							hasConfigBlock.add(0, true)
							methods += e.toMethod("configure", typeRef(void)) [
								exceptions += typeRef(Exception)
								parameters +=
									e.toParameter("dependencies", typeRef(List, typeRef(Subsystem)))
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
							if (e.hosts != null) {
								getters += e.hosts.toMethod("getHost" + currentHostBlock++, e.hosts.inferredType) [
									visibility = JvmVisibility.PRIVATE
									body = e.hosts
								]	
							}
						}
					}
				}
				// Setup rules' commands
				constructors += subsystem.toConstructor [
					body = [
						for (includedSubsystem : includedSubsystems) {
							val fqn = includedSubsystem.fullyQualifiedName
							trace(subsystem)
								.append('''this.«prefix»«fqn.toString("_")» = new «fqn»();''')
								.newLine
						}
					]
				]
				if (!params.empty) {
					constructors += subsystem.toConstructor [
						for (param : params) {
							if (param.type != null || param.right != null)
								parameters += param.toParameter(param.name, param.type ?: param.right.inferredType)
						}
						for (param : includedParams) {
							if (param.type != null || param.right != null)
								parameters += param.toParameter(param.name, param.type ?: param.right.inferredType)
						}
						body = [
							trace(subsystem)
								.append(params.join("\n", [p|'''this.«p.name» = «p.name»;''']))
								.newLine
							for (includedSubsystem : includedSubsystems) {
								val fqn = includedSubsystem.fullyQualifiedName
								val _params = includedSubsystem.body.expressions.filter(VariableDeclaration).filter[v|v.param].map[p|p.name]
								trace(subsystem)
									.append('''this.«prefix»«fqn.toString("_")» = new «fqn»(«_params.join(", ")»);''')
									.newLine
							}
						]
					]
				}
				methods += subsystem.toMethod("init", typeRef(void)) [
					visibility = JvmVisibility.PRIVATE
					body = initRules(subsystem)
				]
				methods += subsystem.toMethod("setup", typeRef(void)) [
					annotations += annotationRef(Override)
					body = setupRules(subsystem, subsystemParam)
				]
				methods += subsystem.toMethod("deploy", typeRef(void)) [
					exceptions += typeRef(Exception)
					parameters += subsystem.toParameter(subsystemParam, typeRef(String))
					parameters +=
						subsystem.toParameter(dependenciesParam, typeRef(List, typeRef(Subsystem)))
					body = subsystem.setupGraph(subsystemParam)
				]
				
				// Method to return all rules from included subsystems
				getters += subsystem.toMethod("getAllRules", typeRef(CommandDescriptor).addArrayTypeDimension) [
					body = [
						val rules = subsystem.body.expressions.filter(OnHostBlockExpression).map[h|h.rules].flatten.map [r|
							r.name
						].toList
						if (subsystem.extensions != null) {
							val includes = subsystem.extensions.declarations.filter(IncludeDeclaration).map [d|
								d.element as org.amelia.dsl.amelia.Subsystem
							]
							for (var i = 0; i < includes.size; i++) {
								val includedSubsystem = includes.get(i)
								rules += '''«AmeliaJvmModelInferrer.prefix»«includedSubsystem.fullyQualifiedName.toString("_")».getAllRules()'''
							}
						}
						if (rules.empty) {
							trace(subsystem).append("return new ").append(CommandDescriptor).append("[0];")
						} else {
							trace(subsystem)
								.append("return ").append(Arrays).append(".concatAll(")
								.increaseIndentation.newLine
								.append(rules.join(",\n")).decreaseIndentation.newLine
								.append(");")
						}
					]
				]
				// Wrapper methods to hide the internal implementation
				if (hasConfigBlock.get(0)) {
					fields +=
						subsystem.toField(AmeliaJvmModelInferrer.prefix + "dependencies", typeRef(List, typeRef(Subsystem))) [
							initializer = [
								trace(subsystem)
									.append("new ").append(ArrayList).append("<").append(Subsystem).append(">()")
							]
						]
					methods += subsystem.toMethod("execute", typeRef(void)) [
						exceptions += typeRef(Exception)
						parameters += subsystem.toParameter("stopPreviousExecutions", typeRef(boolean))
						body = [
							trace(subsystem)
								.append("super.graph.execute(stopPreviousExecutions);")
						]
					]
					methods += subsystem.toMethod("execute", typeRef(void)) [
						exceptions += typeRef(Exception)
						parameters += subsystem.toParameter("stopPreviousExecutions", typeRef(boolean))
						parameters += subsystem.toParameter("shutdownAfterDeployment", typeRef(boolean))
						parameters += subsystem.toParameter("stopExecutionsWhenFinish", typeRef(boolean))
						body = [
							trace(subsystem)
								.append('''
									super.graph.execute(
										stopPreviousExecutions, 
										shutdownAfterDeployment, 
										stopExecutionsWhenFinish);
								''')
						]
					]
					methods += subsystem.toMethod("searchDependency", typeRef(Subsystem)) [
						visibility = JvmVisibility.PRIVATE
						parameters +=
							subsystem.toParameter("clazz", typeRef(Class, wildcardExtends(typeRef(Deployment))))
						body = [
							trace(subsystem)
								.append(Subsystem).append(''' dependency = null;''').newLine
								.append('''String fqn = clazz.getCanonicalName();''').newLine
								.append("for (").append(Subsystem).append(''' subsystem : this.«AmeliaJvmModelInferrer.prefix»dependencies) {''')
								.newLine
								.append('''
										if (subsystem.alias().equals(fqn)) {
											dependency = subsystem;
											break;
										}
									}
									if (dependency == null) {
										throw new IllegalArgumentException("Subsystem " + fqn + " is not a dependency");
									}
									return dependency;''')
						]
					]
					methods += subsystem.toMethod("release", typeRef(void)) [
						parameters +=
							subsystem.toParameter("clazz", typeRef(Class, wildcardExtends(typeRef(Deployment))))
						parameters += subsystem.toParameter("compositeNames", typeRef(List, typeRef(String)))
						body = [
							trace(subsystem)
								.append(Subsystem).append(''' dependency = searchDependency(clazz);''')
								.newLine
								.append('''dependency.deployment().shutdownAndStopComponents(compositeNames.toArray(new String[0]));''')
						]
					]
					methods += subsystem.toMethod("release", typeRef(void)) [
						parameters +=
							subsystem.toParameter("clazz", typeRef(Class, wildcardExtends(typeRef(Deployment))))
						parameters += subsystem.toParameter("stopAllComponents", typeRef(boolean))
						body = [
							trace(subsystem)
								.append(Subsystem).append(" dependency = searchDependency(clazz);")
								.newLine
								.append('''dependency.deployment().shutdown(stopAllComponents);''')
						]
					]
				}
				// Add class members
				members += _parameters
				members += fields
				members += constructors
				members += methods
				members += getters
			}
		]
	}
	
	def Procedure1<ITreeAppendable> initRules(org.amelia.dsl.amelia.Subsystem subsystem) {
		return [
			for (varDecl : subsystem.body.expressions.filter(VariableDeclaration)) {
				if (varDecl.right != null) {
					if (varDecl.param) {
						trace(subsystem)
							.append('''if (this.«varDecl.name» == null)''')
							.increaseIndentation.newLine
					}
					trace(subsystem)
						.append('''this.«varDecl.name» = init«varDecl.name.toFirstUpper»();''')
					if (varDecl.param)
						trace(subsystem).decreaseIndentation
					trace(subsystem).newLine
				}
			}
			for (hostBlock : subsystem.body.expressions.filter(OnHostBlockExpression)) {
				for (rule : hostBlock.rules) {
					var currentCommand = 0
					for (command : rule.commands) {
						append('''«rule.name»[«currentCommand»]''')
						append(''' = init«rule.name.toFirstUpper»«currentCommand»();''')
						trace(subsystem).newLine
						currentCommand++
					}
				}
			}
		]
	}
	
	def Procedure1<ITreeAppendable> setupRules(org.amelia.dsl.amelia.Subsystem subsystem, String subsystemParam) {
		return [
			trace(subsystem).append("init();").newLine
			if (subsystem.extensions != null) {
				val setups = subsystem.extensions.declarations.filter(IncludeDeclaration).map [ d |
					if (d.element instanceof org.amelia.dsl.amelia.Subsystem)
						d.element as org.amelia.dsl.amelia.Subsystem
				].join("", "\n", "\n", [ s |
					'''«AmeliaJvmModelInferrer.prefix»«s.fullyQualifiedName.toString("_")».setup();'''
				])
				trace(subsystem).append(setups)
			}
			var currentHostBlock = 0
			for (hostBlock : subsystem.body.expressions.filter(OnHostBlockExpression)) {
				if (currentHostBlock > 0)
					trace(subsystem).newLine
					append(List).append("<").append(Host).append(">").append(" hosts" + currentHostBlock)
					append(" = ").append(Lists).append('''.newArrayList(getHost«currentHostBlock»());''').newLine
				for (rule : hostBlock.rules) {
					var currentCommand = 0
					for (command : rule.commands) {
						trace(subsystem)
							.append('''«rule.name»[«currentCommand»]''')
							.append('''.runsOn(hosts«currentHostBlock»);''')
						if (currentCommand == 0 && !rule.dependencies.empty) {
							val dependencies = newArrayList
							for (dependency : rule.dependencies) {
								dependencies += '''«dependency.getAccessName(subsystem)»[«dependency.commands.length - 1»]'''
							}
							trace(subsystem).newLine
							append('''«rule.name»[«currentCommand»].dependsOn(«dependencies.join(", ")»);''')
						} else if (currentCommand > 0) {
							trace(subsystem).newLine
							append('''«rule.name»[«currentCommand»].dependsOn(«rule.getAccessName(subsystem)»[«(currentCommand - 1)»]);''')
						}
						if (!rule.commands.last.equals(command))
							trace(subsystem).newLine
						currentCommand++
					}
				}
				currentHostBlock++
			}
		]
	}
	
	def Procedure1<ITreeAppendable> setupGraph(org.amelia.dsl.amelia.Subsystem subsystem, String subsystemParam) {
		return [
			val rules = subsystem.getAllIncludedRules()
			val hasConfigBlock = subsystem.body.expressions.exists[c|c instanceof ConfigBlockExpression]
			trace(subsystem)
				.append('''super.graph = new ''').append(DescriptorGraph).append('''(«subsystemParam»);''').newLine
				.append('''super.graph.addDescriptors(getAllRules());''').newLine
			if (hasConfigBlock) {
				trace(subsystem)
					.append('''this.«AmeliaJvmModelInferrer.prefix»dependencies = «AmeliaJvmModelInferrer.prefix»dependencies;''').newLine
				append('''configure(«AmeliaJvmModelInferrer.prefix»dependencies);''')
			} else if (!rules.empty) {
				trace(subsystem).append('''super.graph.execute(true);''')
			}
		]
	}
	
	def List<JvmField> getIncludesAsFields(org.amelia.dsl.amelia.Subsystem subsystem) {
		val members = newArrayList
		if (subsystem.extensions != null) {
			for (include : subsystem.extensions.declarations.filter(IncludeDeclaration)) {
				if (include.element instanceof org.amelia.dsl.amelia.Subsystem) {
					val includedSubsystem = include.element as org.amelia.dsl.amelia.Subsystem
					val fqn = includedSubsystem.fullyQualifiedName
					members += includedSubsystem.toField(AmeliaJvmModelInferrer.prefix + fqn.toString("_"), typeRef(fqn.toString))
				}
			}
		}
		return members
	}
	
	def List<RuleDeclaration> getAllIncludedRules(org.amelia.dsl.amelia.Subsystem subsystem) {
		val rules = subsystem.body.expressions.filter(OnHostBlockExpression).map[h|h.rules].flatten.toList
		if (subsystem.extensions != null) {
			val includes = subsystem.extensions.declarations.filter(IncludeDeclaration).map [ d |
				d.element as org.amelia.dsl.amelia.Subsystem
			]
			for (includedSubsystem : includes)
				rules += includedSubsystem.getAllIncludedRules()
		}
		return rules
	}
	
	def getAccessName(RuleDeclaration rule, org.amelia.dsl.amelia.Subsystem subsystem) {
		val segments = rule.fullyQualifiedName.segments
		val containerFQN = QualifiedName.create(segments.subList(0, segments.length - 1))
		var accessName = rule.name
		if (!containerFQN.equals(subsystem.fullyQualifiedName)) {
			accessName = AmeliaJvmModelInferrer.prefix + containerFQN.toString("_") + "." + rule.name
		}
		return accessName
	}

}
