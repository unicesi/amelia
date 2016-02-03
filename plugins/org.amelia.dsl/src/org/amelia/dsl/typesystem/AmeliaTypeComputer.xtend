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
package org.amelia.dsl.typesystem

import java.util.List
import org.amelia.dsl.amelia.SequentialBlock
import org.amelia.dsl.lib.descriptors.CommandDescriptor
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.typesystem.computation.ITypeComputationState
import org.eclipse.xtext.xbase.typesystem.computation.ITypeExpectation
import org.eclipse.xtext.xbase.typesystem.computation.XbaseTypeComputer
import org.eclipse.xtext.xbase.typesystem.conformance.ConformanceFlags
import org.amelia.dsl.amelia.ChangeDirectory

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
class AmeliaTypeComputer extends XbaseTypeComputer {
	
	override computeTypes(XExpression expression, ITypeComputationState state) {
		switch (expression) {
			SequentialBlock: _computeTypes(expression, state)
			ChangeDirectory: _computeTypes(expression, state)
			default: super.computeTypes(expression, state)
		}
	}
	
	def protected _computeTypes(ChangeDirectory expression, ITypeComputationState state) {
		val result = getRawTypeForName(org.amelia.dsl.lib.descriptors.ChangeDirectory, state);
		state.acceptActualType(result);
	}
	
	def protected void _computeTypes(SequentialBlock object, ITypeComputationState state) {
		val children = object.commands
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
