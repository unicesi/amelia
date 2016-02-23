package org.amelia.dsl.runtime

import org.eclipse.emf.ecore.EObject
import org.eclipse.xtext.naming.DefaultDeclarativeQualifiedNameProvider

class AmeliaQualifiedNameProvider extends DefaultDeclarativeQualifiedNameProvider {

	override getFullyQualifiedName(EObject obj) {
		switch (obj) {
			default:
				return super.getFullyQualifiedName(obj)
		}
	}

}
