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
package org.pascani.deployment.amelia.util;

import static org.pascani.deployment.amelia.util.Strings.ascii;

public class Log {
	
	public static void heading(String message) {
		message = ascii(187) + " " + cyan(message);
		System.out.println(message);
	}
	
	public static void info(String message) {
		message = message.replaceAll(ascii(10003), green(ascii(10003))); // ✓
		message = message.replaceAll(ascii(10007), red(ascii(10007))); // ✗
		message = message.replaceAll(ascii(9888), yellow(ascii(9888))); // ⚠

		System.out.println(message);
	}
	
	public static void error(String message) {
		message = ascii(9632) + " " +  red(message);
		System.out.println(message);
	}
	
	private static String red(String text) {
		return "\u001b[1;31m" + text + reset();
	}
	
	private static String green(String text) {
		return "\u001b[1;32m" + text + reset();
	}
	
	private static String yellow(String text) {
		return "\u001b[1;43m" + text + reset();
	}
	
	private static String cyan(String text) {
		return "\u001b[1;36m" + text + reset();
	}
	
	private static String reset() {
		return "\u001b[0m";
	}

}
