/*
 * Copyright © 2015 Universidad Icesi
 * 
 * This file is part of the Amelia library.
 * 
 * The Amelia library is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * The Amelia library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the Amelia library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.amelia.dsl.lib.util;

import java.security.InvalidParameterException;

/**
 * @author Miguel Jiménez - Initial contribution and API
 * 
 * TODO: document this class
 * TODO: use generics
 */
public class Arrays {
	
	/*
	 * Taken from: http://stackoverflow.com/a/784842/738968
	 */
	public static <T> T[] concatAll(T[] first, T[]... rest) {
		int totalLength = first.length;
		for (T[] array : rest) {
			totalLength += array.length;
		}
		T[] result = java.util.Arrays.copyOf(first, totalLength);
		int offset = first.length;
		for (T[] array : rest) {
			System.arraycopy(array, 0, result, offset, array.length);
			offset += array.length;
		}
		return result;
	}
	
	public static String[] skip(String[] strings, int elementsToSkip) {
		if (elementsToSkip > strings.length)
			throw new InvalidParameterException(
					"The number of elements to skip cannot be greater than "
							+ "the actual number of elements in the array");
		String[] result = new String[strings.length - elementsToSkip];
		System.arraycopy(strings, elementsToSkip, result, 0, result.length);
		return result;
	}
	
	public static String[] take(String[] strings, int elementsToTake) {
		if (elementsToTake > strings.length)
			throw new InvalidParameterException(
					"The number of elements to take cannot be greater than "
							+ "the actual number of elements in the array");
		String[] result = new String[elementsToTake];
		System.arraycopy(strings, 0, result, 0, elementsToTake);
		return result;
	}
	
	public static String join(String[] args, String separator,
			String lastSeparator) {
		String output = "";
		for (int i = 0; i < args.length; i++) {
			if (i == args.length - 2)
				separator = lastSeparator;
			if (i == args.length - 1)
				separator = "";
			output += args[i] + separator;
		}
		return output;
	}
	
	public static String join(String[] args, String separator) {
		return join(args, separator, separator);
	}

}
