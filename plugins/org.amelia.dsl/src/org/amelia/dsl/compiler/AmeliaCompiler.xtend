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
			default: super.internalToConvertedExpression(obj, appendable)
		}
	}

	override doInternalToJavaStatement(XExpression expr, ITreeAppendable appendable, boolean isReferenced) {
		switch (expr) {
			CommandLiteral: _toJavaStatement(expr, appendable, isReferenced)
			default: super.doInternalToJavaStatement(expr, appendable, isReferenced)
		}
	}
	
	def protected void _toJavaStatement(CommandLiteral expr, ITreeAppendable b, boolean isReferenced) {
		println(expr)
		if (!isReferenced) {
			internalToConvertedExpression(expr, b);
			b.append(";");
		} else if (isVariableDeclarationRequired(expr, b)) {
			val later = new Later() {
				override void exec(ITreeAppendable appendable) {
					internalToConvertedExpression(expr, appendable);
				}
			};
			declareFreshLocalVariable(expr, b, later);
		}
	}
	
	/*
	 * TODO: parse command and arguments. Also, add a way to further initialize this element (e.g., messages)
	 */
	def protected void _toJavaExpression(CustomCommand expr, ITreeAppendable b) {
		// Remove command delimiters
		val lines = expr.expression
			.trim
			.substring(1, expr.expression.length - 1)
			.replaceAll("\\\\´", "´")
			.split("\n")
		val expression = new StringBuilder
		for (line : lines) {
			expression.append(line.trim)
		}
		b.append(Commands).append(".generic(\"").append(expression).append("\")");
	}
	
	def protected void _toJavaExpression(ChangeDirectory expr, ITreeAppendable b) {
		b.append(Commands).append(".cd").append("(")
		internalToConvertedExpression(expr.directory, b)
		b.append(")")
	}
	
	def protected void _toJavaExpression(Compilation expr, ITreeAppendable b) {
		b.append(Commands).append(".compile").append("(")
		internalToConvertedExpression(expr.source, b)
		b.append(", ")
		internalToConvertedExpression(expr.output, b)
		b.append(", ")
		internalToConvertedExpression(expr.classpath, b)
		b.append(")")
	}

}
