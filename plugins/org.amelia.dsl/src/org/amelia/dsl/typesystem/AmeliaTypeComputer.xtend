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

import org.amelia.dsl.amelia.CdCommand
import org.amelia.dsl.amelia.CompileCommand
import org.amelia.dsl.amelia.CustomCommand
import org.amelia.dsl.amelia.EvalCommand
import org.amelia.dsl.amelia.RunCommand
import org.amelia.dsl.amelia.StringLiteral
import org.amelia.dsl.lib.descriptors.CommandDescriptor
import org.amelia.dsl.lib.util.Commands
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.typesystem.computation.ITypeComputationState
import org.eclipse.xtext.xbase.typesystem.computation.XbaseTypeComputer

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
class AmeliaTypeComputer extends XbaseTypeComputer {
	
	override computeTypes(XExpression expression, ITypeComputationState state) {
		switch (expression) {
			CdCommand: _computeTypes(expression, state)
			CompileCommand: _computeTypes(expression, state)
			RunCommand: _computeTypes(expression, state)
			CustomCommand: _computeTypes(expression, state)
			EvalCommand: _computeTypes(expression, state)
			StringLiteral: _computeTypes(expression, state)
			default: super.computeTypes(expression, state)
		}
	}
	
	def protected _computeTypes(EvalCommand command, ITypeComputationState state) {
		// Compute type for the inner expressions
		state.withinScope(command)
		val noExpectationState = state.withoutExpectation()
		noExpectationState.computeTypes(command.script)
		noExpectationState.computeTypes(command.uri)
		addLocalToCurrentScope(command.script, state)
		addLocalToCurrentScope(command.uri, state)
		
		// set the actual type for the entire expression
		val result = getRawTypeForName(CommandDescriptor, state)
		state.acceptActualType(result)
	}
	
	def protected _computeTypes(CustomCommand command, ITypeComputationState state) {
		// Compute type for the inner expressions
		state.withinScope(command)
		val noExpectationState = state.withoutExpectation()
		noExpectationState.computeTypes(command.value)
		addLocalToCurrentScope(command.value, state)
		
		// set the actual type for the entire expression
		val result = if (command.initializedLater)
				getRawTypeForName(CommandDescriptor.Builder, state)
			else
				getRawTypeForName(CommandDescriptor, state)
		state.acceptActualType(result)
	}
	
	def protected _computeTypes(StringLiteral literal, ITypeComputationState state) {
		// Compute type for the inner expressions
		state.withinScope(literal)
		val noExpectationState = state.withoutExpectation()
		for (part : literal.value.expressions) {
			if (part instanceof XExpression) {
				noExpectationState.computeTypes(part)
				addLocalToCurrentScope(part, state)
			}
		}
		// set the actual type for the entire expression
		val result = getRawTypeForName(String, state)
		state.acceptActualType(result)
	}
	
	def protected _computeTypes(CdCommand command, ITypeComputationState state) {
		// Compute type for the inner expressions
		state.withinScope(command)
		val noExpectationState = state.withoutExpectation()
		noExpectationState.computeTypes(command.directory)
		addLocalToCurrentScope(command.directory, state)
		
		// set the actual type for the entire expression
		val result = if (command.initializedLater)
				getRawTypeForName(CommandDescriptor.Builder, state)
			else
				getRawTypeForName(CommandDescriptor, state)
		state.acceptActualType(result)
	}
	
	def protected _computeTypes(CompileCommand command, ITypeComputationState state) {
		// Compute type for the inner expressions
		state.withinScope(command)
		val noExpectationState = state.withoutExpectation()
		noExpectationState.computeTypes(command.source)
		noExpectationState.computeTypes(command.output)
		noExpectationState.computeTypes(command.classpath)
		addLocalToCurrentScope(command.source, state)
		addLocalToCurrentScope(command.output, state)
		addLocalToCurrentScope(command.classpath, state)
		
		// set the actual type for the entire expression
		val result = if (command.initializedLater)
				getRawTypeForName(CommandDescriptor.Builder, state)
			else
				getRawTypeForName(CommandDescriptor, state)
		state.acceptActualType(result)
	}
	
	def protected _computeTypes(RunCommand command, ITypeComputationState state) {
		// Compute type for the inner expressions
		state.withinScope(command)
		val noExpectationState = state.withoutExpectation()
		noExpectationState.computeTypes(command.composite)
		addLocalToCurrentScope(command.composite, state)
		noExpectationState.computeTypes(command.libpath)
		addLocalToCurrentScope(command.libpath, state)
		if (command.hasPort) {
			noExpectationState.computeTypes(command.port)
			addLocalToCurrentScope(command.port, state)
		}
		if (command.hasService) {
			noExpectationState.computeTypes(command.service)
			addLocalToCurrentScope(command.service, state)
		}
		if (command.hasMethod) {
			noExpectationState.computeTypes(command.method)
			addLocalToCurrentScope(command.method, state)
		}
		if (command.hasParams) {
			noExpectationState.computeTypes(command.params)
			addLocalToCurrentScope(command.params, state)
		}	
		// set the actual type for the entire expression
		val result = if (command.initializedLater)
				getRawTypeForName(Commands.RunBuilder, state)
			else
				getRawTypeForName(CommandDescriptor, state)
		state.acceptActualType(result)
	}
}
