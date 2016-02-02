package org.amelia.dsl.compiler

import java.util.ArrayList
import org.amelia.dsl.amelia.SequentialBlock
import org.amelia.dsl.lib.descriptors.CommandDescriptor
import org.eclipse.emf.ecore.EObject
import org.eclipse.xtext.xbase.XConstructorCall
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.compiler.Later
import org.eclipse.xtext.xbase.compiler.XbaseCompiler
import org.eclipse.xtext.xbase.compiler.output.ITreeAppendable

class AmeliaCompiler extends XbaseCompiler {

	override internalToConvertedExpression(XExpression obj, ITreeAppendable appendable) {
		switch (obj) {
			SequentialBlock: _toJavaExpression(obj, appendable)
			default: super.internalToConvertedExpression(obj, appendable)
		}
	}

	override doInternalToJavaStatement(XExpression obj, ITreeAppendable appendable, boolean isReferenced) {
		switch (obj) {
			SequentialBlock: _toJavaStatement(obj, appendable, isReferenced)
			default: super.doInternalToJavaStatement(obj, appendable, isReferenced)
		}
	}
	
	def protected void _toJavaExpression(SequentialBlock expr, ITreeAppendable appendable) {
		val b = appendable.trace(expr, false);
		b.append(getVarName(expr, b));
	}

	def protected void _toJavaStatement(SequentialBlock expr, ITreeAppendable appendable, boolean isReferenced) {
		val b = appendable.trace(expr, false)
		var sequentialBlockName = "_"
		if (isReferenced) {
			declareInitializedSyntheticVariable(expr, b, [ t |
				t.append("new ").append(ArrayList).append("<").append(CommandDescriptor).append(">();")
			]);
			sequentialBlockName = getVarName(expr, b)
		}
		val expressions = expr.getExpressions();
		var String previous = null
		for (var i = 0; i < expressions.size(); i++) {
			val current = expressions.get(i);
			declareInitializedSyntheticVariable(current, b, [ t |
				internalToJavaStatement(current, t, false)
			]);
			val name = getVarName(current, b)
			if (isReferenced)
				b.newLine.append(sequentialBlockName).append(".add(").append(name).append(");")
			if (previous != null) {
				b.newLine.append(name).append(".dependsOn(").append(previous).append(")").append(";")
				previous = name
			} else
				previous = name
		}
	}
	
	/**
	 * Removes a line break when the expression is not referenced
	 */
	override protected void _toJavaStatement(XConstructorCall expr, ITreeAppendable b, boolean isReferenced) {
		for (XExpression arg : expr.getArguments())
			prepareExpression(arg, b);
		if (!isReferenced) {
			constructorCallToJavaExpression(expr, b);
			b.append(";");
		} else if (isVariableDeclarationRequired(expr, b)) {
			val later = new Later() {
				override void exec(ITreeAppendable appendable) {
					constructorCallToJavaExpression(expr, appendable);
				}
			};
			declareFreshLocalVariable(expr, b, later);
		}
	}

	/**
	 * Add a favorite variable name for the {@link SequentialBlock} type
	 */
	override protected String getFavoriteVariableName(EObject ex) {
		switch (ex) {
			SequentialBlock: return SequentialBlock.simpleName.toFirstLower
			default: return super.getFavoriteVariableName(ex)
		}
	}
	
	/**
	 * Declares a synthetic variable initialized with a given expression
	 */
	def protected void declareInitializedSyntheticVariable(XExpression expr, ITreeAppendable b, Later later) {
		val type = getTypeForVariableDeclaration(expr);
		val proposedName = makeJavaIdentifier(getFavoriteVariableName(expr));
		val varName = b.declareSyntheticVariable(expr, proposedName);
		b.newLine();
		b.append(type);
		b.append(" ").append(varName).append(" = ");
		later.exec(b);
	}

}
