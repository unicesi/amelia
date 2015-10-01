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

}
