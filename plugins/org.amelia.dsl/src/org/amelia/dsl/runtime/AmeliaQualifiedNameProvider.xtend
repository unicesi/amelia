package org.amelia.dsl.runtime

import org.amelia.dsl.amelia.RuleDeclaration
import org.eclipse.emf.ecore.EObject
import org.eclipse.xtext.naming.DefaultDeclarativeQualifiedNameProvider
import org.eclipse.xtext.naming.QualifiedName

class AmeliaQualifiedNameProvider extends DefaultDeclarativeQualifiedNameProvider {

	override getFullyQualifiedName(EObject obj) {
		switch (obj) {
			RuleDeclaration: {
				if (obj.name == null)
					return QualifiedName.create("")
				return QualifiedName.create(obj.name)
			}
			default:
				return super.getFullyQualifiedName(obj)
		}
	}

}
