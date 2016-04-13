package org.amelia.dsl.ui.editor.bracketmatching

import org.eclipse.xtext.ide.editor.bracketmatching.DefaultBracePairProvider
import org.eclipse.xtext.ide.editor.bracketmatching.BracePair

class AmeliaBracePairProvider extends DefaultBracePairProvider {
	new() {
		super(#{
			new BracePair("(", ")", false),
			new BracePair("{", "}", true),
			new BracePair("[", "]", true),
			new BracePair("«", "»", false)
		})
	}	
}
