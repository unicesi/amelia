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

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.amelia.dsl.lib.descriptors.Host;

import com.google.common.collect.Range;

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
	private static final String DEBUG = "  DEBUG";
	
	public static void print(String message) {
		print(message, false);
	}
	
	private static synchronized void print(String message, boolean showTime) {
		message = colorPairs(message);
		if (showTime) {
			long currentTime = System.currentTimeMillis();
			String formattedTime = timeFormatter.format(currentTime);
//			formattedTime = ANSI.GRAY.format(formattedTime);
			System.out.println(formattedTime + " " + message);
		} else {
			System.out.println(message);
		}
	}

	public static void info(String message) {
		print(INFO + " " + message, true);
	}

	public static void info(Host host, String message) {
		print(INFO + formatHost(host) + message, true);
	}

	public static void error(Host host, String message) {
		print(ERROR + formatHost(host) + message, true);
	}
	
	public static void debug(Host host, String message) {
		print(DEBUG + formatHost(host) + "--- " + message, true);
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
				? " [" + host.toFixedString() + "] " : " ";
	}
	
	private static String colorPairs(final String text) {
		String result = text;
		Map<ANSI, char[]> pairs = new HashMap<ANSI, char[]>();
		pairs.put(ANSI.BLUE, new char[] { '(', ')' });
		pairs.put(ANSI.MAGENTA, new char[] { '[', ']' });
		pairs.put(ANSI.GREEN, new char[] { '{', '}' });
		pairs.put(ANSI.CYAN, new char[] { '\'', '\'' });
		pairs.put(ANSI.YELLOW, new char[] { '"', '"' });
		for (ANSI color : pairs.keySet()) {
			char[] chars = pairs.get(color);
			PairMatcher matcher = new PairMatcher(result, chars[0], chars[1]);
			result = cleanRedundantRegions(matcher.redundantRegions(), result);
			matcher.removeRedundantRegions();
			for (Range<Integer> r : matcher.getRegions()) {
				String start = result.substring(0, r.lowerEndpoint() + 1);
				String middle = result.substring(r.lowerEndpoint() + 1,
						r.upperEndpoint());
				String end = result.substring(r.upperEndpoint());
				String lastUsedColor = lastUsedColor(start);
				result = start + color.format(middle) + lastUsedColor + end;
			}
		}		
		return result;
	}
	
	private static String cleanRedundantRegions(final List<Range<Integer>> regions,
			final String text) {
		String result = text;
		for (Range<Integer> r : regions) {
			result = result.substring(0, r.lowerEndpoint()) + "\u200B"
					+ result.substring(r.lowerEndpoint() + 1, r.upperEndpoint())
					+ "\u200B" + result.substring(r.upperEndpoint() + 1);
		}
		return result;
	}
	
	private static String lastUsedColor(final String text) {
		String lastUsedColor = "";
		String reset = "\u001b[1;0m";
		Pattern color = Pattern.compile("\u001b" + "\\[1;[0-9]+m");
		Matcher matcher = color.matcher(text);
		if (matcher.find()) {
			String matchedColor = matcher.group();
			if (text.lastIndexOf(matchedColor) > text.lastIndexOf(reset)) {
				lastUsedColor = matchedColor;
			}
		}
		return lastUsedColor;
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
