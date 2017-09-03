package org.amelia.dsl.examples;

/**
 * Utility methods.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @date 2017-09-02
 * @version $Id$
 * @since 0.0.1
 */
public final class Util {

	/**
	 * Throws an exception with the given message, when shouldThrow evaluates
	 * to {@code true}.
	 * @param shouldThrow whether this method should throw an exception
	 * @param message The error message
	 * @throws Exception when shouldThrow evaluates to {@code true}
	 */
	public static void raiseif(final boolean shouldThrow, final String message)
		throws Exception {
		if (shouldThrow)
			throw new Exception(message);
	}

}
