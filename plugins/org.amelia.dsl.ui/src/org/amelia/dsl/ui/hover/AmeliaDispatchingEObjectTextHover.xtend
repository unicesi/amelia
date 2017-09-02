/*
 * Copyright © 2017 Universidad Icesi
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
package org.amelia.dsl.ui.hover

import com.google.inject.Inject
import org.eclipse.emf.ecore.EObject
import org.eclipse.jface.text.IRegion
import org.eclipse.jface.text.ITextViewer
import org.eclipse.xtext.Keyword
import org.eclipse.xtext.resource.XtextResource
import org.eclipse.xtext.ui.editor.hover.IEObjectHoverProvider
import org.eclipse.xtext.ui.editor.hover.IEObjectHoverProvider.IInformationControlCreatorProvider
import org.eclipse.xtext.xbase.ui.hover.XbaseDispatchingEObjectTextHover

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
class AmeliaDispatchingEObjectTextHover extends XbaseDispatchingEObjectTextHover {

	@Inject AmeliaKeywordAtOffsetHelper keywordAtOffsetHelper

	@Inject IEObjectHoverProvider hoverProvider

	IInformationControlCreatorProvider lastCreatorProvider = null

	override getHoverInfo(EObject first, ITextViewer textViewer, IRegion hoverRegion) {
		if (first instanceof Keyword) {
			lastCreatorProvider = hoverProvider.getHoverInfo(first, textViewer, hoverRegion)
			return if(lastCreatorProvider === null) null else lastCreatorProvider.getInfo()
		}
		lastCreatorProvider = null
		super.getHoverInfo(first, textViewer, hoverRegion)
	}

	override getHoverControlCreator() {
		if (this.lastCreatorProvider === null)
			super.getHoverControlCreator()
		else
			lastCreatorProvider.getHoverControlCreator()
	}

	override getXtextElementAt(XtextResource resource, int offset) {
		var result = super.getXtextElementAt(resource, offset)
		if (result === null)
			result = keywordAtOffsetHelper.resolveKeywordAt(resource, offset)
		result
	}

}
