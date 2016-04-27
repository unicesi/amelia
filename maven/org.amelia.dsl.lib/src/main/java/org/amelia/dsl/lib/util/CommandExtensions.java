/*
 * Copyright © 2015 Universidad Icesi
 * 
 * This file is part of the Amelia project.
 * 
 * The Amelia project is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * The Amelia project is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the Amelia project. If not, see <http://www.gnu.org/licenses/>.
 */
package org.amelia.dsl.lib.util;

import org.amelia.dsl.lib.descriptors.CommandDescriptor;
import org.eclipse.xtext.xbase.lib.Inline;
import org.eclipse.xtext.xbase.lib.Pure;
import org.eclipse.xtext.xbase.lib.Procedures;

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public class CommandExtensions {
	
	/**
	 * Augments the left {@link CommandDescriptor} with the right one.
	 * 
	 * @param left
	 *            The command at the left hand side of the plus operand
	 * @param right
	 *            The command at the right hand side of the plus operand
	 * @return A new {@link CommandDescriptor} with the same configuration as
	 *         {@code left}, but augmenting its command with {@code right}.
	 */
	@Pure
	@Inline(value = "$1.augmentWith($2)")
	public static CommandDescriptor operator_plus(
			CommandDescriptor left, CommandDescriptor right) {
		return left.augmentWith(right);
	}
	
	/**
	 * Modifies the given {@link CommandDescriptor} builder and builds it
	 * 
	 * @param builder
	 *            The {@link CommandDescriptor} builder
	 * @param procedure
	 *            The procedure modifying the builder
	 * @return the built {@link CommandDescriptor}
	 */
	public static CommandDescriptor operator_doubleArrow(
			final CommandDescriptor.Builder builder,
			final Procedures.Procedure1<CommandDescriptor.Builder> procedure) {
		procedure.apply(builder);
		return builder.build();
	}
	
	/**
	 * Modifies the given {@link Commands.RunBuilder} and builds it
	 * 
	 * @param builder
	 *            The run command builder
	 * @param procedure
	 *            The procedure modifying the builder
	 * @return the built {@link CommandDescriptor}
	 */
	public static CommandDescriptor operator_doubleArrow(
			final Commands.RunBuilder builder,
			final Procedures.Procedure1<Commands.RunBuilder> procedure) {
		procedure.apply(builder);
		return builder.build();
	}

}
