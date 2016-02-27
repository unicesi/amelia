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
