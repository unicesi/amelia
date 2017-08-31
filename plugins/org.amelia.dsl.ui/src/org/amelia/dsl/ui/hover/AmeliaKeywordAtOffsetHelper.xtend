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

import org.eclipse.jface.text.IRegion
import org.eclipse.emf.ecore.EObject
import org.eclipse.xtext.Keyword
import org.eclipse.xtext.util.Tuples
import org.eclipse.xtext.nodemodel.util.NodeModelUtils
import org.eclipse.xtext.resource.XtextResource
import org.eclipse.jface.text.Region

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
class AmeliaKeywordAtOffsetHelper {

	def resolveKeywordAt(XtextResource resource, int offset) {
		val parseResult = resource.getParseResult()
		if (parseResult !== null) {
			var leaf = NodeModelUtils.findLeafNodeAtOffset(parseResult.getRootNode(), offset)
			if (leaf !== null && leaf.isHidden() && leaf.getOffset() == offset) {
				leaf = NodeModelUtils.findLeafNodeAtOffset(parseResult.getRootNode(), offset - 1)
			}
			if (leaf !== null && leaf.getGrammarElement() instanceof Keyword) {
				val keyword = leaf.getGrammarElement() as Keyword
				return Tuples.create(keyword as EObject, new Region(leaf.getOffset(), leaf.getLength()) as IRegion)
			}
		}
	}

}
