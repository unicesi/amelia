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

import org.amelia.dsl.amelia.CdCommand
import org.amelia.dsl.amelia.CommandLiteral
import org.amelia.dsl.amelia.CompileCommand
import org.amelia.dsl.amelia.CustomCommand
import org.amelia.dsl.amelia.InterpolatedString
import org.amelia.dsl.amelia.RunCommand
import org.amelia.dsl.amelia.StringLiteral
import org.amelia.dsl.amelia.TextEndLiteral
import org.amelia.dsl.amelia.TextLiteral
import org.amelia.dsl.amelia.TextMiddleLiteral
import org.amelia.dsl.amelia.TextStartLiteral
import org.amelia.dsl.lib.descriptors.CommandDescriptor
import org.amelia.dsl.lib.util.Commands
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.compiler.Later
import org.eclipse.xtext.xbase.compiler.XbaseCompiler
import org.eclipse.xtext.xbase.compiler.output.ITreeAppendable
import org.eclipse.xtext.util.Strings
import org.amelia.dsl.amelia.EvalCommand
import org.pascani.dsl.lib.sca.FrascatiUtils

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
class AmeliaCompiler extends XbaseCompiler {

	override internalToConvertedExpression(XExpression obj, ITreeAppendable appendable) {
		switch (obj) {
			CdCommand: _toJavaExpression(obj, appendable)
			CompileCommand: _toJavaExpression(obj, appendable)
			RunCommand: _toJavaExpression(obj, appendable)
			CustomCommand: _toJavaExpression(obj, appendable)
			EvalCommand: _toJavaExpression(obj, appendable)
			StringLiteral: _toJavaExpression(obj, appendable)
			default: super.internalToConvertedExpression(obj, appendable)
		}
	}

	override doInternalToJavaStatement(XExpression expr, ITreeAppendable appendable, boolean isReferenced) {
		switch (expr) {
			CommandLiteral: _toJavaStatement(expr, appendable, isReferenced)
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
		_value = _value
			.substring(1, value.length - 1)
			.replaceAll("\\\\\"", "\"")
			.replaceAll("\\\\\\{", "{")
			.replaceAll("\\\\\\}", "}")
		var lines = Strings.convertToJavaString(_value, true).split("\n")
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
	
	def protected void _toJavaExpression(CustomCommand expr, ITreeAppendable appendable) {
		if (expr.initializedLater) {
			appendable.append("new ").append(CommandDescriptor.Builder).append("()")
			appendable.increaseIndentation.increaseIndentation
			appendable.newLine.append(".withCommand(")
			internalToConvertedExpression(expr.value, appendable)
			appendable.append(")")
			appendable.decreaseIndentation.decreaseIndentation
		} else {
			appendable.append(Commands).append(".generic(")
			internalToConvertedExpression(expr.value, appendable)
			appendable.append(")")
		}
	}
	
	def protected void _toJavaExpression(EvalCommand expr, ITreeAppendable appendable) {
		appendable.append(Commands)
		appendable.append(".evalFScript(")
		internalToConvertedExpression(expr.script, appendable)
		appendable.append(", ")
		if (expr.uri != null)
			internalToConvertedExpression(expr.uri, appendable)
		else
			appendable.append(FrascatiUtils).append(".DEFAULT_BINDING_URI")
		appendable.append(")")
	}
	
	def protected void _toJavaExpression(CdCommand expr, ITreeAppendable appendable) {
		appendable.append(Commands)
		if (expr.initializedLater)
			appendable.append(".cdBuilder")
		else
			appendable.append(".cd")
		appendable.append("(")
		internalToConvertedExpression(expr.directory, appendable)
		appendable.append(")")
	}
	
	def protected void _toJavaExpression(CompileCommand expr, ITreeAppendable appendable) {
		appendable.append(Commands)
		if (expr.initializedLater)
			appendable.append(".compileBuilder")
		else
			appendable.append(".compile")
		appendable.append("(")
		internalToConvertedExpression(expr.source, appendable)
		appendable.append(", ")
		internalToConvertedExpression(expr.output, appendable)
		if (expr.classpath != null) {
			appendable.append(", ")
			internalToConvertedExpression(expr.classpath, appendable)
		}
		appendable.append(")")
	}
	
	def protected void _toJavaExpression(RunCommand expr, ITreeAppendable appendable) {
		appendable.append(Commands).append(".run").append("()")
		appendable.increaseIndentation.increaseIndentation
		
		appendable.newLine.append(".withComposite(")
		internalToConvertedExpression(expr.composite, appendable)
		appendable.append(")")
		
		appendable.newLine.append(".withLibpath(")
		internalToConvertedExpression(expr.libpath, appendable)
		appendable.append(")")
		if (expr.hasPort) {
			appendable.newLine.append(".withPort(")
			internalToConvertedExpression(expr.port, appendable)
			appendable.append(")")
		}
		if (expr.hasService) {
			appendable.newLine.append(".withService(")
			internalToConvertedExpression(expr.service, appendable)
			appendable.append(")")
		}
		if (expr.hasMethod) {
			appendable.newLine.append(".withMethod(")
			internalToConvertedExpression(expr.method, appendable)
			appendable.append(")")
		}
		if (expr.hasParams) {
			appendable.newLine.append(".withArguments(")
			internalToConvertedExpression(expr.params, appendable)
			appendable.append(")")
		}
		if (!expr.initializedLater)
			appendable.append(".build()")
		appendable.decreaseIndentation.decreaseIndentation
	}

}
