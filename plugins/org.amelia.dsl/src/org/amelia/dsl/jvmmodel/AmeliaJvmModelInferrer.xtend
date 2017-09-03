/*
 * Copyright © 2015 Universidad Icesi
 * 
 * This file is part of the Amelia project.
 * 
 * The Amelia project is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * The Amelia project is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with The Amelia project. If not, see <http://www.gnu.org/licenses/>.
 */
package org.amelia.dsl.jvmmodel

import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import com.google.inject.Inject
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.List
import java.util.Map
import org.amelia.dsl.amelia.ConfigBlockExpression
import org.amelia.dsl.amelia.DependDeclaration
import org.amelia.dsl.amelia.DeploymentDeclaration
import org.amelia.dsl.amelia.IncludeDeclaration
import org.amelia.dsl.amelia.Model
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
	
	val prefix = "＿"
	
	var Map<OnHostBlockExpression, String> hostBlockIndices = new HashMap<OnHostBlockExpression, String>
	var Map<RuleDeclaration, String> ruleIndices = new HashMap<RuleDeclaration, String>

	def dispatch void infer(DeploymentDeclaration deployment, IJvmDeclaredTypeAcceptor acceptor, boolean isPreIndexingPhase) {
		val clazz = deployment.toClass(deployment.fullyQualifiedName)
		acceptor.accept(clazz) [
			if (!isPreIndexingPhase) {
				val model = deployment.eContainer as Model
				val subsystems = if(model.extensions !== null) 
						model.extensions.declarations.filter(IncludeDeclaration).map [d|
							d.element as org.amelia.dsl.amelia.Subsystem
						]
					else
						Collections.EMPTY_LIST
				documentation = deployment.documentation
				members +=
					deployment.toField(prefix + "subsystems",
						typeRef(HashMap, typeRef(String), typeRef(List, typeRef(Subsystem)))) [
					visibility = JvmVisibility.PRIVATE
					initializer = [
						trace(deployment)
							.append("new ").append(HashMap).append("<").append(String).append(", ")
							.append(List).append("<").append(Subsystem).append(">>()")
					]
				]
				members += deployment.toMethod("main", typeRef(void)) [
					static = true
					parameters += deployment.toParameter("args", typeRef(String).addArrayTypeDimension)
					exceptions += typeRef(Exception)
					body = [
						append('System.setProperty("java.util.logging.config.file", "logging.properties");').newLine
						append(clazz).append(" main = new ").append(clazz).append("();").newLine
						append("main.init();").newLine
						append("main.custom();")
					]
				]
				members += deployment.toMethod("custom", typeRef(void)) [
					visibility = JvmVisibility.PRIVATE
					body = deployment.body
				]
				members += deployment.toMethod("add", typeRef(void)) [
					visibility = JvmVisibility.PRIVATE
					parameters += deployment.toParameter("deployment", typeRef(Deployment))
					body = [
						trace(deployment)
							.append("String clazz = deployment.getClass().getCanonicalName();")
							.newLine
							.append('''if («prefix»subsystems.containsKey(clazz)) {''')
							.increaseIndentation.newLine
						append('''«prefix»subsystems.get(clazz).add(new ''')
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
								.append('''«prefix»subsystems.put("«qualifiedName»", new ''')
								.append(ArrayList)
								.append('''());''')
							if (!subsystems.last.equals(subsystem))
								trace(deployment).newLine
						}
					]
				]
				members += deployment.toMethod("start", typeRef(boolean)) [
					visibility = JvmVisibility.PRIVATE
					parameters += deployment.toParameter("stopExecutedComponents", typeRef(boolean))
					parameters += deployment.toParameter("shutdownAfterDeployment", typeRef(boolean))
					exceptions += typeRef(Exception)
					body = startMethodBody(deployment, subsystems, true)
				]
				members += deployment.toMethod("start", typeRef(boolean)) [
					visibility = JvmVisibility.PRIVATE
					parameters += deployment.toParameter("stopExecutedComponents", typeRef(boolean))
					exceptions += typeRef(Exception)
					body = '''return start(stopExecutedComponents, true);'''
				]
			}
		]
	}

	def dispatch void infer(org.amelia.dsl.amelia.Subsystem subsystem, IJvmDeclaredTypeAcceptor acceptor, boolean isPreIndexingPhase) {
		val clazz = subsystem.toClass(subsystem.fullyQualifiedName)
		if (clazz === null)
			return;
		acceptor.accept(clazz) [
			if (!isPreIndexingPhase) {
				val model = subsystem.eContainer as Model
				val subsystemParam = prefix + "subsystem"
				val dependenciesParam = prefix + "dependencies"
				val params = subsystem.params
				val includedSubsystems = subsystem.includedSubsystems
				val includedParams = subsystem.includedParams(false)
				val includedVars = subsystem.includedVars(false)
				val _parameters = newArrayList
				val fields = newArrayList
				val constructors = newArrayList
				val methods = newArrayList
				val getters = newArrayList
				val hasConfigBlock = newArrayList(false)
				
				// count blocks and rules (indices)
				hostBlockIndices = subsystem.hostBlocksIndices
				ruleIndices = subsystem.rulesIndices
				
				documentation = subsystem.documentation
				superTypes += typeRef(Subsystem.Deployment)

				// Transform includes into fields
				fields += subsystem.includesAsFields
				
				for (e : subsystem.body.expressions) {
					switch (e) {
						VariableDeclaration: {
							// Transform variable declarations into fields
							_parameters += e.toField(e.name, e.type ?: inferredType) [
								documentation = e.documentation
							]
							if (e.right !== null) {
								getters += e.toMethod("init" + e.name.toFirstUpper, e.type ?: inferredType) [
									visibility = JvmVisibility.PRIVATE
									body = e.right
								]								
							}
							getters += e.toGetter(e.name, e.type ?: inferredType)
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
							val hostBlockIndex = hostBlockIndices.get(e)
							
							// Transform rules inside on-host blocks into array fields
							for (rule : e.rules) {
								fields += rule.toField(rule.name, typeRef(CommandDescriptor).addArrayTypeDimension) [
									initializer = '''new «CommandDescriptor»[«rule.commands.length»]'''
									final = true
									visibility = JvmVisibility.PUBLIC
								]
								if (rule.condition !== null) {
									getters += rule.condition.toMethod("getRuleCondition" + ruleIndices.get(rule), rule.condition.inferredType) [
										visibility = JvmVisibility.PUBLIC
										body = rule.condition
									]
								}
								var currentCommand = 0
								for (command : rule.commands) {
									getters += rule.toMethod("init" + rule.name.toFirstUpper + currentCommand++, typeRef(CommandDescriptor)) [
										visibility = JvmVisibility::PRIVATE
										body = command
									]
								}
							}
							
							// Helper methods. Replace this when Xtext allows to compile XExpressions in specific places
							if (e.hosts !== null) {
								getters += e.hosts.toMethod("getHost" + hostBlockIndex, e.hosts.inferredType) [
									visibility = JvmVisibility.PRIVATE
									body = e.hosts
								]	
							}
							if (e.condition !== null) {
								getters += e.condition.toMethod("getHostCondition" + hostBlockIndex, e.condition.inferredType) [
									visibility = JvmVisibility.PUBLIC
									body = e.condition
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
								.append('''this.«includedSubsystem.javaName» = new «fqn»();''')
								.newLine
						}
					]
				]
				if (!params.empty || !includedParams.empty) {
					constructors += subsystem.toConstructor [
						for (param : params) {
							if (param.type !== null || param.right !== null)
								parameters += param.toParameter(param.name, param.type ?: param.right.inferredType)
						}
						parameters += subsystem.includedParams(true).map[ p |
							p.toParameter(p.fullyQualifiedName.javaName, p.type ?: p.right.inferredType)
						].groupBy[p|p.name].values.map[l|l.get(0)]
						body = [
							trace(subsystem)
								.append(params.join("\n", [p|'''this.«p.name» = «p.name»;''']))
								.newLine
							for (includedSubsystem : includedSubsystems) {
								val fqn = includedSubsystem.fullyQualifiedName
								val _params = includedSubsystem.params + includedSubsystem.includedParams(true)
								trace(subsystem)
									.append('''this.«includedSubsystem.javaName» = new «fqn»(«_params.join(", ", [p|p.fullyQualifiedName.javaName])»);''')
									.newLine
							}
						]
					]
				}
				// Add a getter for non-duplicate parameters
				val includedParamsAndVars = includedParams + includedVars
				val duplicates = (includedParamsAndVars + subsystem.variables).groupBy[p|p.name]
					.values.filter[l|l.size > 1]
					.map[l|l.get(0).name]
					.toList
				for (paramOrVar : includedParamsAndVars) {
					if (!duplicates.contains(paramOrVar.name)) {
						getters += paramOrVar.toMethod("get" + paramOrVar.name.toFirstUpper, paramOrVar.type ?: inferredType) [
							body = '''return this.«paramOrVar.fullyQualifiedName.skipLast(1).javaName».get«paramOrVar.name.toFirstUpper»();'''
						]
					}
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
				methods += subsystem.toMethod("main", typeRef(void)) [
					static = true
					exceptions += typeRef(Exception)
					parameters += subsystem.toParameter("args", typeRef(String).addArrayTypeDimension)
					body = [
						val allowed = subsystem.body.expressions.filter(VariableDeclaration).filter[v|v.param && v.right === null].empty
							&& (
								model.extensions === null
								|| (model.extensions !== null && model.extensions.declarations.filter(DependDeclaration).empty)
							)
						if (allowed) {
							append(Subsystem).append(" subsystem = new ").append(Subsystem)
								.append('''("«subsystem.fullyQualifiedName»", new «subsystem.name»());''').newLine
							append("subsystem.deployment().setup();").newLine
							append(SubsystemGraph).append(" graph = ").append(SubsystemGraph).append(".getInstance();").newLine
							append('''
								graph.addSubsystems(subsystem);
								graph.execute(true, false);''')
						} else {
							append('''
								throw new Exception("Subsystems with dependencies or non-initialized" 
									+ " parameters cannot be executed without using a deployment descriptor");''')
						}
					]
				]
				
				// Method to return all rules from included subsystems
				getters += subsystem.toMethod("getAllRules", typeRef(CommandDescriptor).addArrayTypeDimension) [
					body = [
						val rules = subsystem.body.expressions.filter(OnHostBlockExpression).map[h|h.rules].flatten.map [r|
							r.name
						].toList
						if (model.extensions !== null) {
							val includes = model.extensions.declarations.filter(IncludeDeclaration).map [d|
								d.element as org.amelia.dsl.amelia.Subsystem
							]
							for (includedSubsystem : includes) {
								rules += '''«includedSubsystem.javaName».getAllRules()'''
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
						subsystem.toField(prefix + "dependencies", typeRef(List, typeRef(Subsystem))) [
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
								.append("for (").append(Subsystem).append(''' subsystem : this.«prefix»dependencies) {''')
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
				if (varDecl.right !== null) {
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
		val model = subsystem.eContainer as Model
		return [
			if (model.extensions !== null) {
				val setups = model.extensions.declarations.filter(IncludeDeclaration).map [ d |
					if (d.element instanceof org.amelia.dsl.amelia.Subsystem)
						d.element as org.amelia.dsl.amelia.Subsystem
				].join("", "\n", "\n", [ s |
					'''«s.javaName».setup();'''
				])
				trace(subsystem).append(setups)
			}
			trace(subsystem).append("init();")
			var currentHostBlock = 0
			for (hostBlock : subsystem.body.expressions.filter(OnHostBlockExpression)) {
				trace(subsystem)
					.newLine
					.append(List).append("<").append(Host).append(">").append(" hosts" + currentHostBlock)
					.append(" = ").append(Lists).append('''.newArrayList(getHost«currentHostBlock»());''')
				for (rule : hostBlock.rules) {
					var currentCommand = 0
					for (command : rule.commands) {
						trace(subsystem)
							.newLine
							.append('''«rule.name»[«currentCommand»]''')
							.append('''.runsOn(hosts«currentHostBlock»);''')
						if (hostBlock.condition !== null) { // condition on host
							trace(subsystem)
								.newLine
								.append('''«rule.name»[«currentCommand»]''')
								.append('''.addExecutionCondition(getHostCondition«hostBlockIndices.get(hostBlock)»());''')
						}
						if (rule.condition !== null) { // condition on rule
							trace(subsystem)
								.newLine
								.append('''«rule.name»[«currentCommand»]''')
								.append('''.addExecutionCondition(getRuleCondition«ruleIndices.get(rule)»());''')
						}
						if (currentCommand == 0 && !rule.dependencies.empty) {
							val nonConditionalDeps = rule.dependencies.filter[r|(r.eContainer as OnHostBlockExpression).condition === null]
							val conditionalDeps = rule.dependencies.filter[ r |
								r.condition !== null || (r.eContainer as OnHostBlockExpression).condition !== null
							]
							if (!nonConditionalDeps.empty) {
								val dependencies = newArrayList
								for (dependency : nonConditionalDeps) {
									dependencies += '''«dependency.javaName(subsystem)»[«dependency.commands.length - 1»]'''
								}
								trace(subsystem)
									.newLine
									.append('''«rule.name»[«currentCommand»].dependsOn(«dependencies.join(", ")»);''')	
							}
							
							if (!conditionalDeps.empty) {
								for (dependency : conditionalDeps) {
									val getter = '''«dependency.javaName(subsystem)»[«dependency.commands.length - 1»]'''
									trace(subsystem)
										.newLine
										.append('''«rule.name»[«currentCommand»].dependsOn(«getter»);''')
								}
							}

						} else if (currentCommand > 0) {
							trace(subsystem)
								.newLine
								.append('''«rule.name»[«currentCommand»].dependsOn(«rule.javaName(subsystem)»[«(currentCommand - 1)»]);''')
						}
						currentCommand++
					}
				}
				currentHostBlock++
			}
		]
	}
	
	def Procedure1<ITreeAppendable> setupGraph(org.amelia.dsl.amelia.Subsystem subsystem, String subsystemParam) {
		return [
			val rules = subsystem.includedRules
			val hasConfigBlock = subsystem.body.expressions.exists[c|c instanceof ConfigBlockExpression]
			trace(subsystem)
				.append('''super.graph = new ''').append(DescriptorGraph).append('''(«subsystemParam»);''').newLine
				.append('''super.graph.addDescriptors(getAllRules());''').newLine
			if (hasConfigBlock) {
				trace(subsystem)
					.append('''this.«prefix»dependencies = «prefix»dependencies;''').newLine
				append('''configure(«prefix»dependencies);''')
			} else if (!rules.empty) {
				trace(subsystem).append('''super.graph.execute(true);''')
			}
		]
	}
	
	def Procedure1<ITreeAppendable> startMethodBody(DeploymentDeclaration deployment,
		Iterable<org.amelia.dsl.amelia.Subsystem> subsystems, boolean includeSecondParam) {
		return [
			trace(deployment)
				.append(SubsystemGraph).append(" graph = ").append(SubsystemGraph).append(".getInstance();").newLine
				.append('''
					«FOR subsystem : subsystems»
						«val model = subsystem.eContainer as Model»
						«val dependencies = 
							if(model.extensions !== null)
								model.extensions.declarations.filter(DependDeclaration)
							else
								Collections.EMPTY_LIST
						»
						«IF !dependencies.empty»
							for (Subsystem s : «prefix»subsystems.get("«subsystem.fullyQualifiedName»")) {
								s.dependsOn(
									«FOR d : dependencies SEPARATOR ",\n"»
										«prefix»subsystems.get("«d.element.fullyQualifiedName»").toArray(new «Subsystem.simpleName»[0])
									«ENDFOR»
								);
							}
						«ENDIF»
					«ENDFOR»
				''')
				.append(Subsystem).append("[] subs = ").append(Lists)
			   		.append(".newArrayList(").append(Iterables).append('''.concat(«prefix»subsystems.values())).toArray(new ''')
			   		.append(Subsystem).append("[0]);").newLine
			   	.append("for(").append(Subsystem).append(" s : subs) {")
			   	.increaseIndentation.newLine
			  	.append("s.deployment().setup();")
			   	.decreaseIndentation.newLine
			   	.append("}").newLine
				.append('''graph.addSubsystems(subs);''').newLine
			append("boolean successful = graph.execute(stopExecutedComponents")
			if (includeSecondParam)
				append(", shutdownAfterDeployment")
			append(");")
			trace(deployment)
				.newLine.append("return successful;")
		]
	}
	
	def List<JvmField> includesAsFields(org.amelia.dsl.amelia.Subsystem subsystem) {
		val members = newArrayList
		val model = subsystem.eContainer as Model
		if (model.extensions !== null) {
			for (include : model.extensions.declarations.filter(IncludeDeclaration)) {
				if (include.element instanceof org.amelia.dsl.amelia.Subsystem) {
					val includedSubsystem = include.element as org.amelia.dsl.amelia.Subsystem
					val fqn = includedSubsystem.fullyQualifiedName
					members += includedSubsystem.toField("$" + fqn.toString("$"), typeRef(fqn.toString))
				}
			}
		}
		return members
	}
	
	def Map<RuleDeclaration, String> getRulesIndices(org.amelia.dsl.amelia.Subsystem subsystem) {
		var b = 0
		var r = 0
		val indices = new HashMap<RuleDeclaration, String>
		for (hostBlock : subsystem.body.expressions.filter(OnHostBlockExpression)) {
			for (rule : hostBlock.rules) {
				indices.put(rule, b + "" + r)
				r++
			}
			b++
		}
		return indices
	}
	
	def Map<OnHostBlockExpression, String> getHostBlocksIndices(org.amelia.dsl.amelia.Subsystem subsystem) {
		var b = 0
		val indices = new HashMap<OnHostBlockExpression, String>
		for (hostBlock : subsystem.body.expressions.filter(OnHostBlockExpression)) {
			indices.put(hostBlock, b + "")
			b++
		}
		return indices
	}
	
	def List<RuleDeclaration> includedRules(org.amelia.dsl.amelia.Subsystem subsystem) {
		val model = subsystem.eContainer as Model
		val rules = subsystem.body.expressions.filter(OnHostBlockExpression).map[h|h.rules].flatten.toList
		if (model.extensions !== null) {
			val includes = model.extensions.declarations.filter(IncludeDeclaration).map [ d |
				d.element as org.amelia.dsl.amelia.Subsystem
			]
			for (includedSubsystem : includes)
				rules += includedSubsystem.includedRules()
		}
		return rules
	}
	
	def includedSubsystems(org.amelia.dsl.amelia.Subsystem subsystem) {
		val model = subsystem.eContainer as Model
		return if (model.extensions !== null)
			model.extensions.declarations.filter(IncludeDeclaration).map [ i |
				if (i.element instanceof org.amelia.dsl.amelia.Subsystem)
					i.element as org.amelia.dsl.amelia.Subsystem
			]
		else
			Collections.EMPTY_LIST
	}
	
	def variables(org.amelia.dsl.amelia.Subsystem subsystem) {
		return subsystem.body.expressions.filter(VariableDeclaration)
	}
	
	def params(org.amelia.dsl.amelia.Subsystem subsystem) {
		return variables(subsystem).filter[v|v.param]
	}

	def List<VariableDeclaration> includedParams(org.amelia.dsl.amelia.Subsystem subsystem, boolean recursive) {
		return includedVariableDecl(subsystem, recursive, true, false)
	}

	def List<VariableDeclaration> includedVars(org.amelia.dsl.amelia.Subsystem subsystem, boolean recursive) {
		return includedVariableDecl(subsystem, recursive, false, true)
	}

	def List<VariableDeclaration> includedVariableDecl(org.amelia.dsl.amelia.Subsystem subsystem,
		boolean recursive, boolean inclParams, boolean inclVars) {
		val variableDecls = newArrayList
		val model = subsystem.eContainer as Model
		val includedSubsystems = if (model.extensions !== null)
				model.extensions.declarations.filter(IncludeDeclaration).filter [ i |
					i.element instanceof org.amelia.dsl.amelia.Subsystem
				].map [ i |
					i.element as org.amelia.dsl.amelia.Subsystem
				]
			else
				Collections.EMPTY_LIST
		for (s : includedSubsystems) {
			var includedVarDecls = s.body.expressions.filter(VariableDeclaration)
			if (!inclParams) // filter out parameters
				includedVarDecls = includedVarDecls.filter[v|!v.param]
			if (!inclVars) // filter out variables
				includedVarDecls = includedVarDecls.filter[v|v.param]
			for (param : includedVarDecls) {
				if (param.type !== null || param.right !== null)
					variableDecls += param
			}
			if (recursive)
				variableDecls += includedVariableDecl(s, recursive, inclParams, inclVars)
		}
		return variableDecls
	}
	
	def javaName(RuleDeclaration rule, org.amelia.dsl.amelia.Subsystem subsystem) {
		val segments = rule.fullyQualifiedName.segments
		val containerFQN = QualifiedName.create(segments.subList(0, segments.length - 1))
		var accessName = rule.name
		if (!containerFQN.equals(subsystem.fullyQualifiedName)) {
			accessName = containerFQN.javaName + "." + rule.name
		}
		return accessName
	}
	
	def javaName(org.amelia.dsl.amelia.Subsystem subsystem) {
		return "$" + subsystem.fullyQualifiedName.toString("$")
	}
	
	def javaName(QualifiedName fqn) {
		return "$" + fqn.toString("$")
	}

}
