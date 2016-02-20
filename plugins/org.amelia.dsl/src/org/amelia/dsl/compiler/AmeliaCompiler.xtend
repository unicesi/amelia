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
package org.amelia.dsl.compiler

import org.amelia.dsl.amelia.ChangeDirectory
import org.amelia.dsl.amelia.CommandLiteral
import org.amelia.dsl.amelia.Compilation
import org.amelia.dsl.amelia.CustomCommand
import org.amelia.dsl.amelia.InterpolatedString
import org.amelia.dsl.amelia.OnHostBlockExpression
import org.amelia.dsl.amelia.RuleDeclaration
import org.amelia.dsl.amelia.StringLiteral
import org.amelia.dsl.amelia.TextEndLiteral
import org.amelia.dsl.amelia.TextLiteral
import org.amelia.dsl.amelia.TextMiddleLiteral
import org.amelia.dsl.amelia.TextStartLiteral
import org.amelia.dsl.lib.util.Commands
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.compiler.Later
import org.eclipse.xtext.xbase.compiler.XbaseCompiler
import org.eclipse.xtext.xbase.compiler.output.ITreeAppendable

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
class AmeliaCompiler extends XbaseCompiler {

	override internalToConvertedExpression(XExpression obj, ITreeAppendable appendable) {
		switch (obj) {
			ChangeDirectory: _toJavaExpression(obj, appendable)
			Compilation: _toJavaExpression(obj, appendable)
			CustomCommand: _toJavaExpression(obj, appendable)
			OnHostBlockExpression: _toJavaExpression(obj, appendable)
			RuleDeclaration: _toJavaExpression(obj, appendable)
			StringLiteral: _toJavaExpression(obj, appendable)
			default: super.internalToConvertedExpression(obj, appendable)
		}
	}

	override doInternalToJavaStatement(XExpression expr, ITreeAppendable appendable, boolean isReferenced) {
		switch (expr) {
			CommandLiteral: _toJavaStatement(expr, appendable, isReferenced)
			OnHostBlockExpression: _toJavaStatement(expr, appendable, isReferenced)
			RuleDeclaration: _toJavaStatement(expr, appendable, isReferenced)
			StringLiteral: _toJavaStatement(expr, appendable, isReferenced)
			default: super.doInternalToJavaStatement(expr, appendable, isReferenced)
		}
	}
	
	override protected boolean isVariableDeclarationRequired(XExpression expr, ITreeAppendable appendable) {
		switch (expr) {
			CommandLiteral: return false
			default: return super.isVariableDeclarationRequired(expr, appendable)
		}
	}
	
	def protected void _toJavaStatement(OnHostBlockExpression block, ITreeAppendable appendable, boolean isReferenced) {
		val b = appendable.trace(block, false);
		if (block.rules.isEmpty())
			return;
		if (block.rules.size()==1) {
			internalToJavaStatement(block.rules.get(0), b, isReferenced);
			return;
		}
		if (isReferenced)
			declareSyntheticVariable(block, b);
		for (var i = 0; i < block.rules.size(); i++) {
			val ex = block.rules.get(i);
			if (i < block.rules.size() - 1) {
				internalToJavaStatement(ex, b, false);
			} else {
				internalToJavaStatement(ex, b, isReferenced);
				if (isReferenced) {
					b.newLine().append(getVarName(block, b)).append(" = ");
					internalToConvertedExpression(ex, b, getLightweightType(block));
					b.append(";");
				}
			}
		}
	}
	
	def protected void _toJavaExpression(OnHostBlockExpression expr, ITreeAppendable appendable) {
		if (expr.rules.isEmpty()) {
			appendable.append("null");
			return;
		}
		if (expr.rules.size() == 1) {
			// conversion was already performed for single expression blocks
			internalToConvertedExpression(expr.rules.get(0), appendable, null);
			return;
		}
		val b = appendable.trace(expr, false);
		b.append(getVarName(expr, b));
	}
	
	def protected void _toJavaStatement(RuleDeclaration expr, ITreeAppendable appendable, boolean isReferenced) {
		if (!isReferenced) {
			internalToConvertedExpression(expr, appendable);
			appendable.append(";");
		} else if (isVariableDeclarationRequired(expr, appendable)) {
			val later = new Later() {
				override void exec(ITreeAppendable appendable) {
					internalToConvertedExpression(expr, appendable);
				}
			};
			declareFreshLocalVariable(expr, appendable, later);
		}
	}
	
	def protected void _toJavaExpression(RuleDeclaration expr, ITreeAppendable appendable) {
		appendable.append('''/* RuleDeclaration «expr.name»*/''');
	}
	
	def protected void _toJavaStatement(CommandLiteral expr, ITreeAppendable appendable, boolean isReferenced) {
		if (!isReferenced) {
			internalToConvertedExpression(expr, appendable);
			appendable.append(";");
		} else if (isVariableDeclarationRequired(expr, appendable)) {
			val later = new Later() {
				override void exec(ITreeAppendable appendable) {
					internalToConvertedExpression(expr, appendable);
				}
			};
			declareFreshLocalVariable(expr, appendable, later);
		}
	}
	
	def protected String formatCommandText(String value, boolean escapeClosingComment) {
		var _value = if (escapeClosingComment)
				value.replaceAll("\\\\* \\/", "\\\\* \\/")
			else
				value
		var lines = _value // Strings.convertToJavaString(_value, true)
			.substring(1, value.length - 1)
			.replaceAll("\"", "\\\\\"")
			.replaceAll("\\\\\\{", "{")
			.replaceAll("\\\\\\}", "}")
			.replaceAll("\\\\\\'", "'")
			.replaceAll("(\\\\)([^\"])", "$1$1$2") // replace \_ by \\_
			.split("\n")
		if (lines.length > 1)
			lines = lines.map[l|l.replaceAll("^\\s*", "")] // left trim
		return lines.filter[l|!l.isEmpty].join(" ")
	}
	
	def protected void compileTemplate(InterpolatedString literal, ITreeAppendable appendable) {
		compileTemplate(literal, appendable, false)
	}
	
	def protected void compileTemplate(InterpolatedString literal, ITreeAppendable appendable, boolean escapeClosingComment) {
		appendable.append("\"")
		for (part : literal.expressions) {
			switch (part) {
				TextLiteral:
					appendable.append(part.value.formatCommandText(escapeClosingComment))
				TextStartLiteral:
					appendable.append(part.value.formatCommandText(escapeClosingComment))
				TextMiddleLiteral:
					appendable.append(part.value.formatCommandText(escapeClosingComment))
				TextEndLiteral:
					appendable.append(part.value.formatCommandText(escapeClosingComment))
				XExpression: {
					appendable.append("\" + ")
					internalToConvertedExpression(part, appendable)
					appendable.append(" + \"")
				}
			}
		}
		appendable.append("\"")
	}
	
	def void _toJavaExpression(StringLiteral expr, ITreeAppendable b) {
		compileTemplate(expr.value, b)
	}
	
	def protected void _toJavaStatement(StringLiteral expr, ITreeAppendable b, boolean isReferenced) {
		generateComment(new Later() {
			override void exec(ITreeAppendable appendable) {
				compileTemplate(expr.value, appendable, true)
			}
		}, b, isReferenced);
	}
	
	/*
	 * TODO: Add a way to further initialize this element (e.g., messages)
	 */
	def protected void _toJavaExpression(CustomCommand expr, ITreeAppendable appendable) {
		appendable.append(Commands).append(".generic(")
		internalToConvertedExpression(expr.value, appendable)
		appendable.append(")")
	}
	
	def protected void _toJavaExpression(ChangeDirectory expr, ITreeAppendable appendable) {
		appendable.append(Commands).append(".cd").append("(")
		internalToConvertedExpression(expr.directory, appendable)
		appendable.append(")")
	}
	
	def protected void _toJavaExpression(Compilation expr, ITreeAppendable appendable) {
		appendable.append(Commands).append(".compile").append("(")
		internalToConvertedExpression(expr.source, appendable)
		appendable.append(", ")
		internalToConvertedExpression(expr.output, appendable)
		appendable.append(", ")
		internalToConvertedExpression(expr.classpath, appendable)
		appendable.append(")")
	}

}
