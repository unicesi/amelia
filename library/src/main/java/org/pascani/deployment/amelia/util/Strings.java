package org.pascani.deployment.amelia.util;

import java.util.Collection;

public class Strings {

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

	public static String ascii(int codePoint) {
		return String.valueOf(Character.toChars(codePoint));
	}
	
	public static String truncate(String arg, int minimum, int maximun) {
		if(arg.length() > maximun)
			arg = "..." + arg.substring(arg.length() - maximun + 3);
			
		arg = String.format("%" + minimum + "." + maximun + "s", arg);
		return arg;
	}

}
