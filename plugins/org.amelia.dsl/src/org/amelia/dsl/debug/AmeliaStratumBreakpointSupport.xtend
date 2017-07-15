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
package org.amelia.dsl.debug

import org.amelia.dsl.amelia.CommandLiteral
import org.amelia.dsl.amelia.OnHostBlockExpression
import org.amelia.dsl.amelia.RuleDeclaration
import org.eclipse.xtext.debug.IStratumBreakpointSupport
import org.eclipse.xtext.nodemodel.ICompositeNode
import org.eclipse.xtext.resource.XtextResource
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.XbasePackage

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
class AmeliaStratumBreakpointSupport implements IStratumBreakpointSupport {
	
	override isValidLineForBreakPoint(XtextResource resource, int line) {
		val parseResult = resource.getParseResult();
		if (parseResult === null)
			return false;
		val node = parseResult.getRootNode();
		return isValidLineForBreakpoint(node, line);
	}
	
	def boolean isValidLineForBreakpoint(ICompositeNode node, int line) {
		for (n : node.getChildren()) {
			val textRegion = n.getTextRegionWithLineInformation()
			if (textRegion.getLineNumber()<= line && textRegion.getEndLineNumber() >= line) {
				val eObject = n.getSemanticElement()
				switch (eObject) {
					CommandLiteral: return true
					RuleDeclaration: return true
					OnHostBlockExpression: return true
					XExpression case !(eObject.eClass() == XbasePackage.Literals.XBLOCK_EXPRESSION): {
						return true
					}
					ICompositeNode case isValidLineForBreakpoint(n as ICompositeNode, line): {
						return true
					}
				}
			}
			if (textRegion.getLineNumber() > line) {
				return false
			}
		}
		return false
	}
	
}
