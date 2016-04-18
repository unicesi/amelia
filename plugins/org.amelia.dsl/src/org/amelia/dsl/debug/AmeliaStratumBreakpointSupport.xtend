package org.amelia.dsl.debug

import org.amelia.dsl.amelia.CommandLiteral
import org.amelia.dsl.amelia.OnHostBlockExpression
import org.amelia.dsl.amelia.RuleDeclaration
import org.eclipse.xtext.nodemodel.ICompositeNode
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.XbasePackage
import org.eclipse.xtext.xbase.debug.XbaseStratumBreakpointSupport

class AmeliaStratumBreakpointSupport extends XbaseStratumBreakpointSupport {
	
	override isValidLineForBreakpoint(ICompositeNode node, int line) {
		for (n : node.getChildren()) {
			val textRegion = n.getTextRegionWithLineInformation()
			if (textRegion.getLineNumber()<= line && textRegion.getEndLineNumber() >= line) {
				val eObject = n.getSemanticElement()
				switch (eObject) {
					OnHostBlockExpression: return true
					RuleDeclaration: return true
					CommandLiteral: return true
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