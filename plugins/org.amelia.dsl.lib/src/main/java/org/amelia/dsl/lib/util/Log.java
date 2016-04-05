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

import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.amelia.dsl.lib.descriptors.Host;

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public class Log {

	public static final String SEPARATOR_LONG = ANSI.GRAY
			.format("--------------------------------------------------------------------------------");
	
	public static final String SEPARATOR = ANSI.GRAY
			.format("------------------------------------------------");

	private static final SimpleDateFormat timeFormatter = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss.SSS");
	
	private static final String INFO = ANSI.CYAN.format("   INFO");
	private static final String SUCCESS = ANSI.GREEN.format("SUCCESS");
	private static final String WARN = ANSI.YELLOW.format("   WARN");
	private static final String ERROR = ANSI.RED.format("  ERROR");
	
	public static void print(String message) {
		print(message, false);
	}
	
	private static synchronized void print(String message, boolean showTime) {
		// FIXME: bad coloring when there is already colored text before pairs
		message = colorPairs(message, "['\"]", "['\"]", ANSI.CYAN);
//		message = colorPairs(message, "\\[", "\\]", ANSI.MAGENTA);
		message = colorPairs(message, "\\(", "\\)", ANSI.BLUE);
		if (showTime) {
			long currentTime = System.currentTimeMillis();
			String formattedTime = timeFormatter.format(currentTime);
			formattedTime = ANSI.GRAY.format(formattedTime);
			System.out.println(formattedTime + " " + message);
		} else {
			System.out.println(message);
		}
	}

	public static void info(String message) {
		print(INFO + " " + message, true);
	}

	public static void error(Host host, String message) {
		print(ERROR + formatHost(host) + message, true);
	}

	public static void warning(Host host, String message) {
		print(WARN + formatHost(host) + message, true);
	}

	public static void success(Host host, String message) {
		
		print(SUCCESS + formatHost(host) + message, true);
	}

	public static void error(String message) {
		print(ERROR + " " + message, true);
	}
	
	private static String formatHost(Host host) {
		return host != null
				? " [" + ANSI.MAGENTA.format(host.toFixedString()) + "] " : " ";
	}

	// Adapted from: http://stackoverflow.com/a/24080170/738968
	private static String colorPairs(String text, String leftSymbol,
			String rightSymbol, ANSI color) {
		StringBuilder sb = new StringBuilder();
		sb.append("(" + leftSymbol + ")");
		sb.append("((?:(?!\1).)*)");
		sb.append("(" + rightSymbol + ")");

		Pattern quotes = Pattern.compile(sb.toString());
		Matcher matcher = quotes.matcher(text);

		if (matcher.find()) {
			String left = matcher.group(1);
			String right = matcher.group(3);
			String coloredText = color.format(matcher.group(2));
			if ((left.equals("'") && right.equals("'"))
					|| (left.equals("\"") && right.equals("\""))) {
				text = matcher.replaceAll(coloredText);
			} else {
				text = matcher.replaceAll(left + coloredText + right);
			}
		}

		return text;
	}

	public static void printBanner() {
		// From
		// http://www.chris.com/ascii/index.php?art=transportation/airplanes
		String e = Strings.ascii(233), c = Strings.ascii(169);
		ANSI b = ANSI.BLUE, r = ANSI.RED, y = ANSI.CYAN, bb = ANSI.BLINK;

		String banner = "\n" + "               __"
				+ b.format("/\\")
				+ "__              .----------------------------------------. \n"
				+ "              `=="
				+ b.format("/\\")
				+ "==`             |                                        | \n"
				+ "    " + r.format("____________") + r.format("/")
				+ r.format("__") + b.format("\\") + r.format("____________")
				+ "   |              A M E L I A               | \n" + "   "
				+ r.format("/____________________________\\")
				+ "  |                                        | \n" + "     "
				+ r.format("__") + "||" + r.format("__") + "||"
				+ r.format("__") + b.format("/") + ".--." + b.format("\\")
				+ r.format("__") + "||" + r.format("__") + "||"
				+ r.format("__") + "    | A deployment library by Miguel Jim"
				+ e + "nez | \n" + "    " + r.format("/__") + "|"
				+ r.format("___") + "|" + r.format("___") + "( "
				+ bb.format(y.format("><")) + " )" + r.format("___") + "|"
				+ r.format("___") + "|" + r.format("__\\") + "   |        " + c
				+ " Universidad Icesi 2015        | \n" + "              _"
				+ b.format("/") + "`--`" + b.format("\\")
				+ "_             |                                        | \n"
				+ "             (" + b.format("/") + "------" + b.format("\\")
				+ ")            '----------------------------------------' \n";

		System.out.printf("%s\n", banner);
	}

}
