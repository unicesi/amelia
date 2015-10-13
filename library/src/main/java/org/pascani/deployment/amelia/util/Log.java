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
	
	private static String cyan(String text) {
		return "\u001b[1;36m" + text + reset();
	}
	
	private static String reset() {
		return "\u001b[0m";
	}

}
