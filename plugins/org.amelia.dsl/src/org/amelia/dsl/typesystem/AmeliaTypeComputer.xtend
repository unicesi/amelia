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

import org.amelia.dsl.amelia.ChangeDirectory
import org.amelia.dsl.amelia.Compilation
import org.amelia.dsl.amelia.CustomCommand
import org.amelia.dsl.amelia.OnHostBlockExpression
import org.amelia.dsl.lib.descriptors.CommandDescriptor
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.typesystem.computation.ITypeComputationState
import org.eclipse.xtext.xbase.typesystem.computation.XbaseTypeComputer

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
class AmeliaTypeComputer extends XbaseTypeComputer {
	
	override computeTypes(XExpression expression, ITypeComputationState state) {
		switch (expression) {
			ChangeDirectory: _computeTypes(expression, state)
			Compilation: _computeTypes(expression, state)
			CustomCommand: _computeTypes(expression, state)
			OnHostBlockExpression: _computeTypes(expression, state)
			default: super.computeTypes(expression, state)
		}
	}
	
	def protected _computeTypes(OnHostBlockExpression block, ITypeComputationState state) {
		// Compute type for the inner expressions
		state.withinScope(block);
		val noExpectationState = state.withoutExpectation();
		noExpectationState.computeTypes(block.host);
		addLocalToCurrentScope(block.host, state);
	}
	
	def protected _computeTypes(CustomCommand command, ITypeComputationState state) {
		// set the actual type for the entire expression
		val result = getRawTypeForName(CommandDescriptor, state);
		state.acceptActualType(result);
	}
	
	def protected _computeTypes(ChangeDirectory command, ITypeComputationState state) {
		// Compute type for the inner expressions
		state.withinScope(command);
		val noExpectationState = state.withoutExpectation();
		noExpectationState.computeTypes(command.directory);
		addLocalToCurrentScope(command.directory, state);
		
		// set the actual type for the entire expression
		val result = getRawTypeForName(CommandDescriptor, state);
		state.acceptActualType(result);
	}
	
	def protected _computeTypes(Compilation command, ITypeComputationState state) {
		// Compute type for the inner expressions
		state.withinScope(command);
		val noExpectationState = state.withoutExpectation();
		noExpectationState.computeTypes(command.source);
		noExpectationState.computeTypes(command.output);
		noExpectationState.computeTypes(command.classpath);
		addLocalToCurrentScope(command.source, state);
		addLocalToCurrentScope(command.output, state);
		addLocalToCurrentScope(command.classpath, state);
		
		// set the actual type for the entire expression
		val result = getRawTypeForName(CommandDescriptor, state);
		state.acceptActualType(result);
	}
}
