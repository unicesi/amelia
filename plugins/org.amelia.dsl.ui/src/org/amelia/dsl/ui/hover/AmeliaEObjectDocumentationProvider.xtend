package org.amelia.dsl.ui.hover

import com.google.inject.Inject
import org.eclipse.emf.ecore.EObject
import org.eclipse.xtext.documentation.IEObjectDocumentationProvider
import org.eclipse.xtext.documentation.impl.MultiLineCommentDocumentationProvider

class AmeliaEObjectDocumentationProvider implements IEObjectDocumentationProvider {

	@Inject MultiLineCommentDocumentationProvider docProvider
	
	override getDocumentation(EObject o) {
		return docProvider.getDocumentation(o)
	}
	
}
