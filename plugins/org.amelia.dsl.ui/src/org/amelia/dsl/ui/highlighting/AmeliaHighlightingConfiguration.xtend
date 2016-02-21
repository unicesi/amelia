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

import org.eclipse.xtext.ui.editor.syntaxcoloring.IHighlightingConfigurationAcceptor
import org.eclipse.xtext.xbase.ui.highlighting.XbaseHighlightingConfiguration

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
class AmeliaHighlightingConfiguration extends XbaseHighlightingConfiguration {
	
	public static val String CUSTOM_COMMAND_ID = "amelia.custom_command"
	
	override configure(IHighlightingConfigurationAcceptor acceptor) {
		acceptor.acceptDefaultHighlighting(CUSTOM_COMMAND_ID, "Custom command literals", customCommand())
		super.configure(acceptor)
	}

	def customCommand() {
		val textStyle = stringTextStyle.copy
		return textStyle;
	}
	
}
