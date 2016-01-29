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
package org.amelia.dsl.scoping

import org.amelia.dsl.amelia.AmeliaPackage
import org.amelia.dsl.amelia.Task
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EReference
import org.eclipse.xtext.EcoreUtil2
import org.eclipse.xtext.scoping.Scopes
import org.eclipse.xtext.scoping.impl.FilteringScope
import org.amelia.dsl.amelia.Subsystem

/**
 * This class contains custom scoping description.
 * 
 * See https://www.eclipse.org/Xtext/documentation/303_runtime_concepts.html#scoping
 * on how and when to use it.
 */
class AmeliaScopeProvider extends AmeliaImportedNamespaceAwareLocalScopeProvider {
	
	override getScope(EObject context, EReference reference) {
		switch (context) {
			Task case reference == AmeliaPackage.Literals.TASK__DEPENDENCIES: {
				val root = EcoreUtil2.getRootContainer(context)
				val tasks = Scopes.scopeFor(EcoreUtil2.getAllContentsOfType(root, Task));
				return new FilteringScope(tasks) [ input |
					input.getEObjectOrProxy() != context
				]
			}
			Subsystem case reference == AmeliaPackage.Literals.SUBSYSTEM__DEPENDENCIES: {
				val subsystems = getGlobalScope(context.eResource, reference)
				return new FilteringScope(subsystems) [ input |
					input.getEObjectOrProxy() != context
				]
			}
			default: return super.getScope(context, reference)
		}
	}

}
