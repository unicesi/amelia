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
package org.amelia.dsl.ui.highlighting

import org.eclipse.xtext.ui.editor.syntaxcoloring.IHighlightingConfigurationAcceptor
import org.eclipse.xtext.ui.editor.utils.TextStyle
import org.eclipse.xtext.xbase.ui.highlighting.XbaseHighlightingConfiguration

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
class AmeliaHighlightingConfiguration extends XbaseHighlightingConfiguration {
	
	public static val RICH_TEXT_ID = AmeliaHighlightingStyles.RICH_TEXT_ID
	
	override configure(IHighlightingConfigurationAcceptor acceptor) {
		acceptor.acceptDefaultHighlighting(RICH_TEXT_ID, "Rich text literals", richTextLiterals())
		super.configure(acceptor)
	}

	def richTextLiterals() {
		val textStyle = stringTextStyle.copy
		return textStyle;
	}
	
	override TextStyle staticField(){
		return defaultTextStyle().copy();
	}
	
	override TextStyle field(){
		return defaultTextStyle().copy();
	}
	
}
