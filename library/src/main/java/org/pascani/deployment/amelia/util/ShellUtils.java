package org.pascani.deployment.amelia.util;

/**
 * This class encapsulates utility methods to work with shell interfaces
 * 
 * @author Miguel Jiménez - Initial contribution and API
 */
public class ShellUtils {

	/**
	 * TODO: Only bash, zsh and dash are supported (not csh)
	 * 
	 * @return The command to recognize the current shell being used by a UNIX
	 *         machine
	 */
	public static String currentShellCommand() {
		return "echo $0 | cut -c 2-";
	}

	/**
	 * Creates a string to set the prompt to a reliable prompt, that is, a
	 * unique string not commonly returned by any process. By changing the
	 * prompt, the probability of mistaken expected outputs is considerably
	 * reduced.
	 * 
	 * TODO: Only bash and zsh are supported
	 * 
	 * @param shell
	 *            The current shell being used in the remote machine
	 * @return a command setting the PS1 environment variable to the Amelia
	 *         prompt
	 */
	public static String ameliaPromptFormat(String shell) {

		// e.g., [Amelia library 2015-09-28 18:54:34 user@grid0:~/Desktop]$
		String bash = "[Amelia library \\D{%F %T} \\u@\\h:\\w]$ ";
		String zsh = "[Amelia library %D{%Y-%m-%d %H:%M:%S} %n% @%m% :%~]$ ";

		String command = "PS1=\"";

		if (shell.contains("bash"))
			command += bash + "\"";
		else
			command += zsh + "\"";

		return command;
	}

	/**
	 * @return the regular expression to recognize the Amelia prompt
	 */
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

	/**
	 * @param criteria
	 *            A string to search the programs in execution
	 * @return the kill command to terminate certain processes that meet the
	 *         specified criterion. The returned command uses the SIGTERM
	 *         signal.
	 */
	public static String killCommand(String criterion) {
		StringBuilder sb = new StringBuilder();

		sb.append("for pid in ");
		sb.append("$(" + searchPIDs(criterion) + "); ");
		sb.append("do kill -SIGTERM $pid; ");
		sb.append("done");

		return sb.toString();
	}

	private static String searchPIDs(String criterion) {
		StringBuilder sb = new StringBuilder();

		sb.append("ps -ef ");
		sb.append("| grep \"" + criterion + "\" ");
		sb.append("| grep -v grep ");
		sb.append("| awk '{ print $2 }'");

		return sb.toString();
	}

	/**
	 * TODO: Only bash, zsh, and ksh93 are supported
	 * 
	 * From http://superuser.com/a/705448
	 * 
	 * @return A command to detach a previously executed command from the
	 *         current connection. This way, the execution remains after the
	 *         connection is closed.
	 */
	public static String detachProcess() {
		return "disown";
	}

}
