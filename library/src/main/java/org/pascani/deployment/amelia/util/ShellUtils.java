package org.pascani.deployment.amelia.util;

public class ShellUtils {
	
	/**
	 * TODO: Only bash, zsh and dash are supported (not csh)
	 *  
	 * @return
	 */
	public static String currentShellCommand() {
		return "echo $0 | cut -c 2-";
	}
	
	/**
	 * TODO: Only bash and zsh are supported
	 * 
	 * @param shell
	 * @return
	 */
	public static String ameliaPromptFormat(String shell) {
		
		// [Amelia library 2015-09-28 18:54:34 user@grid0:~/Desktop]$ 
		String bash = "[Amelia library \\D{%F %T} \\u@\\h:\\w]$ ";
		String zsh = "[Amelia library %D{%Y-%m-%d %H:%M:%S} %n% @%m% :%~]$ ";
		
		String command = "PS1=\"";
		
		if(shell.contains("bash"))
			command += bash + "\"";
		else
			command += zsh + "\"";
		
		return command;
	}

	public static String ameliaPromptRegexp() {
		String date = "(\\d+){4}-(\\d+){2}-(\\d+){2}";
		String time = "(\\d+){2}:(\\d+){2}:(\\d+){2}";
		String user = "([a-z_][a-z0-9_]{0,30})";
		String host = "([a-zA-Z0-9-\\.]{0,24})";
		String directory = "(/)?([^/\0]+(/)?)+";

		StringBuilder regexp = new StringBuilder();

		regexp.append("\\[Amelia library ");
		regexp.append(date + " ");
		regexp.append(time + " ");
		regexp.append(user);
		regexp.append("@");
		regexp.append(host + ":");
		regexp.append(directory);
		regexp.append("\\]\\$ ");

		return regexp.toString();
	}

}
