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

/**
 * @author Miguel Jiménez - Initial contribution and API
 * 
 * TODO: document this class
 */
public class Strings {

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
