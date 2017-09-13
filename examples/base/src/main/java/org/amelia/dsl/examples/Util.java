package org.amelia.dsl.examples;

import org.amelia.dsl.lib.descriptors.CommandDescriptor;
import org.amelia.dsl.lib.util.Log;

/**
 * Utility methods.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @date 2017-09-02
 * @version $Id$
 * @since 0.0.1
 */
public final class Util {

	/**
	 * Throws an exception with the given message, when {@code shouldThrow}
	 * resolves to {@code true}.
	 * @param shouldThrow whether this method should throw an exception
	 * @param message The error message
	 * @throws Exception when shouldThrow evaluates to {@code true}
	 */
	public static void raiseif(final boolean shouldThrow, final String message)
		throws Exception {
		if (shouldThrow)
			throw new Exception(message);
	}

	/**
	 * Warns the user before executing a command.
	 * @param command The command
	 * @param message The warning message
	 * @return a command wrapping the original command
	 */
	public static CommandDescriptor warn(final CommandDescriptor command,
		final String message) {
		return new CommandDescriptor.Builder()
			.withSuccessMessage(command.doneMessage())
			.withErrorMessage(command.errorMessage())
			.withCommand(command.toCommandString())
			.withCallable((host, prompt, quiet) -> {
				Log.warning(host, message);
				return command.callable().call(host, prompt, false);
			})
			.build();
	}

}
