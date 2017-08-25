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

import org.amelia.dsl.lib.CallableTask;
import org.amelia.dsl.lib.descriptors.CommandDescriptor;
import org.amelia.dsl.lib.descriptors.Host;
import org.eclipse.xtext.xbase.lib.Inline;
import org.eclipse.xtext.xbase.lib.Procedures;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.eclipse.xtext.xbase.lib.Pure;

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

	/**
     * Executes the given procedure in case the command is not executed
     * successfully.
     * @param command The command to execute
     * @param procedure The callback
     * @return a command descriptor
     */
    public static CommandDescriptor tryOrElse(final CommandDescriptor command,
        final Procedure1<Exception> procedure) {
        return new CommandDescriptor.Builder()
            .withSuccessMessage("TryOrElse command executed successfully")
            .withErrorMessage("Unknown error in TryOrElse procedure")
            .withCommand(command.toCommandString())
            .withCallable(new CallableTask<Object>() {
                @Override
                public Object call(Host host, String prompt, boolean quiet)
                	throws Exception {
                    try {
                        command.callable().call(host, prompt, true);
                        Log.success(host, command.doneMessage());
                    } catch(Exception e) {
                        procedure.apply(e);
                        Log.error(
                            host,
                            String.format(
                                "fallback executed: %s",
                                command.errorMessage()
                            )
                        );
                    }
                    return new Object(); // prevent null pointer exceptions
                }
            }).build();
    }

    /**
     * Applies a function on the output of the given command.
     * @param command the command to execute
     * @param function the function
     * @return a command wrapping the original command
     */
    public static CommandDescriptor fetch(final CommandDescriptor command,
    	final Procedure1<String> procedure) {
        return new CommandDescriptor.Builder()
            .withSuccessMessage(command.doneMessage())
            .withErrorMessage(command.errorMessage())
            .withCommand(command.toCommandString())
            .withCallable(new CallableTask<Object>() {
                @Override
                public Object call(Host host, String prompt, boolean quiet)
                	throws Exception {
                	final Object response = command.callable()
                		.call(host, prompt, true);
                	String fetched = String.class.isInstance(response)
                		? (String) response : response.toString();
                	procedure.apply(fetched);
                    return new Object();
                }
            }).build();
    }

}
