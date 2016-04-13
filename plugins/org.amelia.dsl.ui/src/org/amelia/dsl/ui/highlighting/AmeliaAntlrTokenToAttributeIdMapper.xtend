package org.amelia.dsl.ui.highlighting

import java.util.regex.Pattern
import org.eclipse.xtext.ui.editor.syntaxcoloring.DefaultAntlrTokenToAttributeIdMapper
import org.eclipse.xtext.ide.editor.syntaxcoloring.HighlightingStyles

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
