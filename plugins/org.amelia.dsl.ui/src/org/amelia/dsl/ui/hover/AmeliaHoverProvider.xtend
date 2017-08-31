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

import org.eclipse.xtext.xbase.ui.hover.XbaseHoverProvider
import com.google.inject.Inject
import org.eclipse.emf.ecore.EObject
import org.eclipse.jface.text.IRegion
import org.eclipse.xtext.ui.editor.hover.html.XtextBrowserInformationControlInput
import org.eclipse.xtext.Keyword
import org.eclipse.jface.internal.text.html.HTMLPrinter

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
class AmeliaHoverProvider extends XbaseHoverProvider {

	/**
	 * Utility mapping keywords and hover text.
	 */
	@Inject AmeliaKeywordHovers keywordHovers

	override getHoverInfo(EObject obj, IRegion region, XtextBrowserInformationControlInput prev) {
		if (obj instanceof Keyword) {
			val html = this.getHoverInfoAsHtml(obj)
			if (html !== null) {
				val buffer = new StringBuffer(html)
				HTMLPrinter.insertPageProlog(buffer, 0, getStyleSheet())
				HTMLPrinter.addPageEpilog(buffer)
				return new XtextBrowserInformationControlInput(prev, obj, buffer.toString(), labelProvider)
			}
		}
		return super.getHoverInfo(obj, region, prev)
	}

	override getHoverInfoAsHtml(EObject o) {
		if (o instanceof Keyword)
			return keywordHovers.hoverText(o as Keyword)
		return super.getHoverInfoAsHtml(o)
	}

}
