package org.amelia.dsl.compiler

import org.amelia.dsl.amelia.SequentialBlock
import org.eclipse.emf.ecore.EObject
import org.eclipse.xtext.xbase.XBlockExpression
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.compiler.XbaseCompiler
import org.eclipse.xtext.xbase.compiler.output.ITreeAppendable
import org.eclipse.xtext.xbase.XConstructorCall
import org.eclipse.xtext.xbase.compiler.Later

class AmeliaCompiler extends XbaseCompiler {

	override internalToConvertedExpression(XExpression obj, ITreeAppendable appendable) {
		switch (obj) {
			SequentialBlock: _toJavaExpression(obj as XBlockExpression, appendable)
			default: super.internalToConvertedExpression(obj, appendable)
		}
	}

	override doInternalToJavaStatement(XExpression obj, ITreeAppendable appendable, boolean isReferenced) {
		switch (obj) {
			SequentialBlock: _toJavaStatement(obj, appendable, isReferenced)
			default: super.doInternalToJavaStatement(obj, appendable, isReferenced)
		}
	}

	def void _toJavaStatement(SequentialBlock expr, ITreeAppendable appendable, boolean isReferenced) {
		val b = appendable.trace(expr, false)
		if (expr.getExpressions().isEmpty())
			return;
		if (expr.getExpressions().size() == 1) {
			internalToJavaStatement(expr.getExpressions().get(0), b, isReferenced);
			return;
		}
		if (isReferenced)
			declareSyntheticVariable(expr, b);
		val expressions = expr.getExpressions();
		var String previous = null
		for (var i = 0; i < expressions.size(); i++) {
			val ex = expressions.get(i);
			var name = "_"
			if (i < expressions.size() - 1 || !isReferenced) {
				declareInitializedSyntheticVariable(ex, b);
				name = getVarName(ex, b)
			} else {
				internalToJavaStatement(ex, b, isReferenced);
				name = getVarName(expr, b)
				b.newLine().append(name).append(" = ");
				internalToConvertedExpression(ex, b, getLightweightType(expr));
				b.append(";");
			}
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
	def protected void declareInitializedSyntheticVariable(XExpression expr, ITreeAppendable b) {
		val type = getTypeForVariableDeclaration(expr);
		val proposedName = makeJavaIdentifier(getFavoriteVariableName(expr));
		val varName = b.declareSyntheticVariable(expr, proposedName);
		b.newLine();
		b.append(type);
		b.append(" ").append(varName).append(" = ");
		internalToJavaStatement(expr, b, false);
	}

}
