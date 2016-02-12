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
import java.util.Collection;

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public class Strings {
	
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

	public static String join(Collection<String> args, String separator,
			String lastSeparator) {
		String[] _args = args.toArray(new String[0]);
		return join(_args, separator, lastSeparator);
	}

	public static String join(Collection<String> args, String separator) {
		return join(args, separator, separator);
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

	public static boolean containsAnyOf(String source, String[] strings) {
		boolean contains = false;
		for (int i = 0; i < strings.length && !contains; i++) {
			contains = source.contains(strings[i]);
		}
		return contains;
	}
	
	public static String firstIn(String[] strings, String text) {
		for (int i = 0; i < strings.length; i++) {
			if (text.contains(strings[i]))
				return strings[i];
		}
		return null;
	}

	public static String ascii(int codePoint) {
		return String.valueOf(Character.toChars(codePoint));
	}

	public static String truncate(String arg, int minimum, int maximun) {
		if (arg.length() > maximun)
			arg = "..." + arg.substring(arg.length() - maximun + 3);

		arg = String.format("%" + minimum + "." + maximun + "s", arg);
		return arg;
	}

	public static String center(String text, int length) {
		String out = String.format("%" + length + "s%s%" + length + "s", "",
				text, "");
		float mid = (out.length() / 2);
		float start = mid - (length / 2);
		float end = start + length;
		return out.substring((int) start, (int) end);
	}

}
