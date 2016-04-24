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

import java.util.regex.Pattern
import org.eclipse.xtext.ui.editor.syntaxcoloring.DefaultAntlrTokenToAttributeIdMapper
import org.eclipse.xtext.ide.editor.syntaxcoloring.HighlightingStyles

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
class AmeliaAntlrTokenToAttributeIdMapper extends DefaultAntlrTokenToAttributeIdMapper {
	
	static val PARAMETERS = Pattern.compile("'-classpath'|'-libpath'|'-r'|'-s'|'-m'|'-p'|'--service-name'|'--method-name'")
	static val RICH_STRING_TERMINAL_RULE_NAMES = #{
		'RULE_RICH_TEXT',
		'RULE_RICH_TEXT_START',
		'RULE_RICH_TEXT_MIDDLE',
		'RULE_RICH_TEXT_END'
	}
		
	override protected String calculateId(String tokenName, int tokenType) {
		if (org.amelia.dsl.ui.highlighting.AmeliaAntlrTokenToAttributeIdMapper.PARAMETERS.matcher(tokenName).matches()) {
			return HighlightingStyles.PUNCTUATION_ID
		} else if (RICH_STRING_TERMINAL_RULE_NAMES.contains(tokenName)) {
			return AmeliaHighlightingStyles.RICH_TEXT_ID
		}
		return super.calculateId(tokenName, tokenType)
	}
	
}
