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
 package org.amelia.dsl.runtime

import org.amelia.dsl.amelia.Model
import org.amelia.dsl.amelia.RuleDeclaration
import org.amelia.dsl.amelia.Subsystem
import org.eclipse.emf.ecore.EObject
import org.eclipse.xtext.EcoreUtil2
import org.eclipse.xtext.naming.DefaultDeclarativeQualifiedNameProvider

class AmeliaQualifiedNameProvider extends DefaultDeclarativeQualifiedNameProvider {

	override getFullyQualifiedName(EObject obj) {
		switch (obj) {
			RuleDeclaration: {
				val subsystem = (EcoreUtil2.getRootContainer(obj) as Model).typeDeclaration as Subsystem
				return subsystem.fullyQualifiedName.append(obj.name)
			}
			default:
				return super.getFullyQualifiedName(obj)
		}
	}

}
