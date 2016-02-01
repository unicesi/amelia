package org.amelia.dsl.typesystem

import org.amelia.dsl.amelia.SequentialBlock
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.typesystem.computation.ITypeComputationState
import org.eclipse.xtext.xbase.typesystem.computation.XbaseTypeComputer
import org.eclipse.xtext.xbase.XBlockExpression

class AmeliaTypeComputer extends XbaseTypeComputer {
	
	override computeTypes(XExpression expression, ITypeComputationState state) {
		switch (expression) {
			SequentialBlock: _computeTypes(expression as XBlockExpression, state)
			default: super.computeTypes(expression, state)
		}
	}
	
}
