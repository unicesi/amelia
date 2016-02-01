package org.amelia.dsl.typesystem

import java.util.List
import org.amelia.dsl.amelia.SequentialBlock
import org.amelia.dsl.lib.descriptors.CommandDescriptor
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.typesystem.computation.ITypeComputationState
import org.eclipse.xtext.xbase.typesystem.computation.ITypeExpectation
import org.eclipse.xtext.xbase.typesystem.computation.XbaseTypeComputer
import org.eclipse.xtext.xbase.typesystem.conformance.ConformanceFlags

class AmeliaTypeComputer extends XbaseTypeComputer {
	
	override computeTypes(XExpression expression, ITypeComputationState state) {
		switch (expression) {
			SequentialBlock: _computeTypes(expression, state)
			default: super.computeTypes(expression, state)
		}
	}
	
	def protected void _computeTypes(SequentialBlock object, ITypeComputationState state) {
		val children = object.getExpressions()
		if (children.isEmpty()) {
			for (ITypeExpectation expectation : state.getExpectations()) {
				val expectedType = expectation.getExpectedType()
				if (expectedType != null && expectedType.isPrimitiveVoid()) {
					expectation.acceptActualType(expectedType, ConformanceFlags.CHECKED_SUCCESS);
				} else {
					expectation.acceptActualType(expectation.getReferenceOwner().newAnyTypeReference(),
						ConformanceFlags.UNCHECKED)
				}
			}
		} else {
			state.withinScope(object);
			for (var i = 0; i < children.size(); i++) {
				val expression = children.get(i)
				val expressionState = state.withoutExpectation() // no expectation
				expressionState.computeTypes(expression)
				addLocalToCurrentScope(expression, state)
			}
		}
		val listType = findDeclaredType(List, state);
		val list = state.getReferenceOwner().newParameterizedTypeReference(listType)
		list.addTypeArgument(getRawTypeForName(CommandDescriptor, state))
		state.acceptActualType(list)
	}
	
}
