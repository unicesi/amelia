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
import org.amelia.dsl.amelia.Subsystem
import org.amelia.dsl.amelia.Task
import org.amelia.dsl.amelia.VariableDeclaration
import org.eclipse.xtext.naming.IQualifiedNameProvider
import org.eclipse.xtext.xbase.jvmmodel.AbstractModelInferrer
import org.eclipse.xtext.xbase.jvmmodel.IJvmDeclaredTypeAcceptor
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder

/**
 * <p>Infers a JVM model from the source model.</p> 
 * 
 * <p>The JVM model should contain all elements that would appear in the Java code 
 * which is generated from the source model. Other models link against the JVM model rather than the source model.</p>     
 */
class AmeliaJvmModelInferrer extends AbstractModelInferrer {

	/**
	 * convenience API to build and initialize JVM types and their members.
	 */
	@Inject extension JvmTypesBuilder

	@Inject extension IQualifiedNameProvider

	def dispatch void infer(Subsystem subsystem, IJvmDeclaredTypeAcceptor acceptor, boolean isPreIndexingPhase) {
		acceptor.accept(subsystem.toClass(subsystem.fullyQualifiedName) [
			if (!isPreIndexingPhase) {
				val fields = newArrayList
				val methods = newArrayList
				documentation = subsystem.documentation
				for (e : subsystem.body.expressions) {
					switch (e) {
						VariableDeclaration: {
							fields += e.toField(e.name, if (e.type != null) e.type else inferredType) [
								documentation = e.documentation
								final = !e.writeable
								initializer = e.right
							]
						}
						Task: {
							methods += e.toMethod(e.name.toFirstLower, typeRef(void)) [
								documentation = e.documentation
								body = e.body
								if (e.declaredFormalParameters != null)
									for (param : e.declaredFormalParameters)
										parameters += param.toParameter(param.name, param.parameterType)
							]
						}
					}
				}
				members += fields
				members += methods
			}
		])
	}

}
