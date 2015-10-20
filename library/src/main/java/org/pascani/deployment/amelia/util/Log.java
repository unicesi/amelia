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
		return "\u001b[1;33m" + text + reset();
	}
	
	private static String blue(String text) {
		return "\u001b[1;34m" + text + reset();
	}
	
	private static String cyan(String text) {
		return "\u001b[1;36m" + text + reset();
	}
	
	private static String reset() {
		return "\u001b[0m";
	}
	
	private static String blink(String text) {
		return "\u001B[5m" + text + reset();
	}
	
	public static void printBanner() {
		// From
		// http://www.chris.com/ascii/index.php?art=transportation/airplanes
		String e = Strings.ascii(233), c = Strings.ascii(169);
		String banner = "\n" + "               __"
				+ blue("/\\")
				+ "__              .----------------------------------------. \n"
				+ "              `=="
				+ blue("/\\")
				+ "==`             |                                        | \n"
				+ "    " + red("____________") + blue("/") + red("__") + blue("\\")
				+ red("____________")
				+ "   |              A M E L I A               | \n" + "   "
				+ red("/____________________________\\")
				+ "  |                                        | \n" + "     "
				+ red("__") + "||" + red("__") + "||" + red("__") + blue("/") + ".--."
				+ blue("\\") + red("__") + "||" + red("__") + "||" + red("__")
				+ "    | A deployment library by Miguel Jim" + e + "nez | \n"
				+ "    " + red("/__") + "|" + red("___") + "|" + red("___") + "( "
				+ blink(cyan("><")) + " )" + red("___") + "|" + red("___") + "|"
				+ red("__\\") + "   |        " + c
				+ " Universidad Icesi 2015        | \n" + "              _"
				+ blue("/") + "`--`" + blue("\\")
				+ "_             |                                        | \n"
				+ "             (" + blue("/") + "------" + blue("\\")
				+ ")            '----------------------------------------' \n";

		System.out.printf("%s\n", banner);
	}
	
}
