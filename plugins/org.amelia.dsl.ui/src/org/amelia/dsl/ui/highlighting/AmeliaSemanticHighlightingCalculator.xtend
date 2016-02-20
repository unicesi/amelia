/*
 * Copyright © 2015 Universidad Icesi
 * 
 * This file is part of the Amelia DSL.
 * 
 * The Amelia DSL is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * The Amelia DSL is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with The Amelia DSL. If not, see <http://www.gnu.org/licenses/>.
 */
package org.amelia.dsl.ui.highlighting

import org.amelia.dsl.amelia.TextLiteral
import org.eclipse.emf.ecore.EObject
import org.eclipse.xtext.ide.editor.syntaxcoloring.IHighlightedPositionAcceptor
import org.eclipse.xtext.nodemodel.util.NodeModelUtils
import org.eclipse.xtext.util.CancelIndicator
import org.eclipse.xtext.xbase.ide.highlighting.XbaseHighlightingCalculator
import org.amelia.dsl.amelia.TextStartLiteral
import org.amelia.dsl.amelia.TextMidLiteral
import org.amelia.dsl.amelia.TextEndLiteral

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
class AmeliaSemanticHighlightingCalculator extends XbaseHighlightingCalculator {
	
	override protected boolean highlightElement(EObject object, IHighlightedPositionAcceptor acceptor, CancelIndicator cancelIndicator) {
		switch (object) {
			TextLiteral: highlightCustomCommand(object, acceptor)
			TextStartLiteral: highlightCustomCommand(object, acceptor)
			TextMidLiteral: highlightCustomCommand(object, acceptor)
			TextEndLiteral: highlightCustomCommand(object, acceptor)
		}
		return super.highlightElement(object, acceptor, cancelIndicator)
	}
	
	def protected highlightCustomCommand(EObject expr, IHighlightedPositionAcceptor acceptor) {
		val node = NodeModelUtils.findActualNodeFor(expr);
		highlightNode(acceptor, node, AmeliaHighlightingConfiguration.CUSTOM_COMMAND_ID);
		// TODO: lexical highlight: http://www.eclipse.org/forums/index.php?t=msg&goto=479001&
	}
	
}
