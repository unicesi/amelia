package org.pascani.deployment.amelia.util;

public class Strings {

	public static String join(String[] args, String separator) {
		String output = "";

		for (int i = 0; i < args.length; i++) {
			if (i == args.length - 1)
				separator = "";
			output += args[i] + separator;
		}

		return output;
	}

	public static boolean containsAnyOf(String source, String[] strings) {
		boolean contains = false;

		for (int i = 0; i < strings.length && !contains; i++) {
			contains = source.contains(strings[i]);
		}

		return contains;
	}
	
	public static String ascii(int codePoint) {
		return String.valueOf(Character.toChars(codePoint));
	}

}
