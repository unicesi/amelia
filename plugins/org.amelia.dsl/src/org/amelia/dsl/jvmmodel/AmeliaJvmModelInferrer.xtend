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
import org.amelia.dsl.amelia.OnHostBlockExpression
import org.amelia.dsl.amelia.Subsystem
import org.amelia.dsl.amelia.VariableDeclaration
import org.amelia.dsl.lib.descriptors.CommandDescriptor
import org.amelia.dsl.lib.descriptors.Host
import org.amelia.dsl.outputconfiguration.AmeliaOutputConfigurationProvider
import org.amelia.dsl.outputconfiguration.OutputConfigurationAdapter
import org.eclipse.xtext.common.types.JvmVisibility
import org.eclipse.xtext.naming.IQualifiedNameProvider
import org.eclipse.xtext.xbase.jvmmodel.AbstractModelInferrer
import org.eclipse.xtext.xbase.jvmmodel.IJvmDeclaredTypeAcceptor
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder

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
				documentation = subsystem.documentation
				superTypes += typeRef(org.amelia.dsl.lib.Subsystem.Deployment)
				for (declaration : subsystem.body.expressions.filter(VariableDeclaration)) {
					members += declaration.toField(declaration.name, declaration.type ?: inferredType) [
						documentation = declaration.documentation
						initializer = declaration.right
						final = !declaration.writeable
					]
				}
				for (hostBlock : subsystem.body.expressions.filter(OnHostBlockExpression)) {
					for (rule : hostBlock.rules) {
						members += rule.toField(rule.name, typeRef(CommandDescriptor).addArrayTypeDimension) [
							initializer = '''new «CommandDescriptor»[«rule.commands.length»]'''
							final = true
						]
					}	
				}
				members += subsystem.toMethod("deploy", typeRef(void)) [
					exceptions += typeRef(Exception)
					parameters += subsystem.toParameter("subsystem", typeRef(String))
					parameters +=
						subsystem.toParameter("dependencies",
							typeRef(List, typeRef(org.amelia.dsl.lib.Subsystem)))
					body = [
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
									append('''«rule.name»[«currentCommand»]''')
									append(''' = init«rule.name.toFirstUpper»«currentCommand»();''').newLine
									append('''«rule.name»[«currentCommand»]''')
									append('''.runsOn(hosts«currentHostBlock»);''').newLine
									if (currentCommand == 0 && !rule.dependencies.empty) {
										val dependencies = newArrayList
										for (dependency : rule.dependencies.map[r|r.name]) {
											dependencies += dependency + "[0]"
										}
										append('''«rule.name»[«currentCommand»].dependsOn(«dependencies.join(", ")»);''')
									} else if (currentCommand > 0) {
										append('''«rule.name»[«currentCommand»].dependsOn(«rule.name»[«(currentCommand - 1)»]);''')
									}
									currentCommand++
									if (currentCommand == rule.commands.length)
										newLine
								}
							}
							currentHostBlock++
						}
					]
				]
				var currentHostBlock = 0
				for (hostBlock : subsystem.body.expressions.filter(OnHostBlockExpression)) {
					var currentHost = 0
					for (host : hostBlock.hosts) {
						members += host.toMethod("getHost" + currentHostBlock + currentHost++, typeRef(Host)) [
							body = host
						]
					}
					for (rule : hostBlock.rules) {
						var currentCommand = 0
						for (command : rule.commands) {
							members += rule.toMethod("init" + rule.name.toFirstUpper + currentCommand++, typeRef(CommandDescriptor)) [
								visibility = JvmVisibility::PRIVATE
								body = command
							]
						}
					}
					currentHostBlock++
				}
			}
		]
	}

}
