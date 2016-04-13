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

import org.eclipse.xtext.xbase.ide.highlighting.XbaseHighlightingStyles

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
interface AmeliaHighlightingStyles extends XbaseHighlightingStyles {
	String RICH_TEXT_ID = "amelia.richText";
	String RICH_TEXT_DELIMITER_ID = "amelia.richText.delimiter";
	String INSIGNIFICANT_TEMPLATE_TEXT = "amelia.insignificant.template.text";
}
