package org.amelia.dsl.ui.highlighting

import java.util.regex.Pattern
import org.eclipse.xtext.ui.editor.syntaxcoloring.DefaultAntlrTokenToAttributeIdMapper

class AmeliaAntlrTokenToAttributeIdMapper extends DefaultAntlrTokenToAttributeIdMapper {
	
	val static final _PUNCTUATION = Pattern.compile("'-classpath'|'-libpath'|'-r'|'-s'|'-m'|'-p'|'--service-name'|'--method-name'")
		
	override protected String calculateId(String tokenName, int tokenType) {
		if(_PUNCTUATION.matcher(tokenName).matches()) {
			return AmeliaHighlightingConfiguration.PUNCTUATION_ID
		}
		return super.calculateId(tokenName, tokenType)
	}
	
}
