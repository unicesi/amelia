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

import static net.sf.expectit.matcher.Matchers.regexp;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.amelia.dsl.lib.descriptors.CommandDescriptor;
import org.amelia.dsl.lib.descriptors.Host;
import org.amelia.dsl.lib.descriptors.Version;

import net.sf.expectit.Expect;
import net.sf.expectit.ExpectIOException;
import net.sf.expectit.Result;
import net.sf.expectit.matcher.Matchers;

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public class Commands {
	
	/**
	 * A builder class to configure the execution of SCA components
	 * 
	 * @author Miguel Jiménez - Initial contribution and API
	 */
	public static class RunBuilder {
		
		private String compositeName;
		private String[] libpath;
		private String serviceName;
		private String methodName;
		private String[] arguments;
		private long timeout;
		private String releaseRegexp;
		private String successMessage;

		public RunBuilder() {
			this.timeout = 0;
			this.releaseRegexp = "Press Ctrl\\+C to quit\\.\\.\\.|Call done!";
		}

		public RunBuilder withComposite(final String compositeName) {
			this.compositeName = compositeName;
			return this;
		}

		public RunBuilder withLibpath(final String... libs) {
			this.libpath = libs;
			return this;
		}

		public RunBuilder withService(final String serviceName) {
			this.serviceName = serviceName;
			return this;
		}

		public RunBuilder withMethod(final String methodName) {
			this.methodName = methodName;
			return this;
		}

		public RunBuilder withArguments(final String... arguments) {
			this.arguments = arguments;
			return this;
		}

		public RunBuilder withTimeout(final long timeout) {
			this.timeout = timeout;
			return this;
		}

		public RunBuilder withoutTimeout() {
			this.timeout = -1;
			return this;
		}

		public RunBuilder withReleaseRegexp(final String regularExpression) {
			this.releaseRegexp = regularExpression;
			return this;
		}
		
		public RunBuilder withSuccessMessage(final String successMessage) {
			this.successMessage = successMessage;
			return this;
		}

		/**
		 * TODO: create a task based on the default task and a process detach
		 * after executing the run command
		 * 
		 * @return a {@link CommandDescriptor} with the necessary configuration
		 *         to run the given composite
		 */
		public CommandDescriptor build() {
			if (this.successMessage == null)
				this.successMessage = this.compositeName + " has been executed";

			List<String> arguments = new ArrayList<String>();
			arguments.add("run");
			arguments.add(this.compositeName);
			arguments.add("-libpath");
			arguments.add(Strings.join(this.libpath, ":"));
			if (this.serviceName != null)
				arguments.add("-s " + this.serviceName);
			if (this.methodName != null)
				arguments.add("-m " + this.methodName);
			if (this.arguments != null)
				arguments.add("-p " + Strings.join(this.arguments, " "));

			CommandDescriptor run = new CommandDescriptor.Builder()
					.withCallable(callableTask(arguments))
					.withCommand("frascati")
					.withArguments(arguments.toArray(new String[0]))
					.withReleaseRegexp(this.releaseRegexp)
					.withSuccessMessage(this.successMessage)
					.withTimeout(this.timeout)
					.isExecution()
					.build();
			return run;
		}
		
		private CallableTask<Integer> callableTask(final List<String> arguments) {
			final String[] errors = { "Error when parsing the composite file '"
					+ compositeName + "'", "Cannot instantiate the FraSCAti factory" };
			return new CallableTask<Integer>() {

				@Override public Integer call(Host host, String prompt)
						throws Exception {
					int PID = -1;
					Expect expect = host.ssh().expect();
					if (timeout == -1)
						expect = expect.withInfiniteTimeout();
					else if (timeout > 0)
						expect = expect.withTimeout(timeout, TimeUnit.MILLISECONDS);

					// Execute the command and obtain the process ID
					expect.sendLine("frascati " + Strings.join(arguments, " ") + " &");
					String _pid = expect.expect(regexp("\\[\\d+\\] (\\d+)")).group(1);

					// Detach the process
					expect.sendLine(ShellUtils.detachProcess());
					PID = Integer.parseInt(_pid);

					try {
						// Expect for a successful execution
						String response = expect.expect(regexp(releaseRegexp)).getBefore();
						if (Strings.containsAnyOf(response, errors)) {
							String message = Strings.firstIn(errors, response);
							Log.error(host, message);
							throw new RuntimeException(message);
						} else {
							Log.success(host, successMessage);
						}
					} catch (ExpectIOException e) {
						String message2 = "Operation timeout waiting for \""
								+ releaseRegexp + "\" in host " + host;

						if (Strings.containsAnyOf(e.getInputBuffer(), errors)) {
							Log.error(host, Strings.firstIn(errors, e.getInputBuffer())
											+ " in host " + host);
							throw e;
						} else {
							throw new RuntimeException(message2);
						}
					}
					return PID;
				}
			};
		}
	}

	/**
	 * Configures a {@link CommandDescriptor} to change the current working
	 * directory
	 * 
	 * @param directory
	 *            The new working directory
	 * @return a {@link CommandDescriptor} with the necessary configuration to
	 *         change the working directory
	 */
	public static CommandDescriptor cd(final String directory) {
		String errorMessage = "Could not change working directory to " + ANSI.YELLOW.format(directory);
		String successMessage = "New working directory: " + ANSI.YELLOW.format(directory);
		String[] errors = { "No existe el fichero o el directorio",
				"Permiso denegado", "No such file or directory",
				"Permission denied" };
		CommandDescriptor cd = new CommandDescriptor.Builder()
				.withCommand("cd")
				.withArguments(directory)
				.withErrorText(errors)
				.withErrorMessage(errorMessage)
				.withSuccessMessage(successMessage)
				.build();
		return cd;
	}
	
	/**
	 * Configures a {@link CommandDescriptor} to run a SCA component
	 * 
	 * @return a builder object to configure the component execution
	 */
	public static RunBuilder run() {
		return new RunBuilder();
	}
	
	/**
	 * Configures a {@link CommandDescriptor} to compile a source directory
	 * 
	 * @param sourceDirectory
	 *            The directory containing all the java sources
	 * @param outputFile
	 *            The name of the output jar file
	 * @param classpath
	 *            The source extra classpath
	 * @return a {@link CommandDescriptor} with the necessary configuration to
	 *         compile the sources
	 */
	public static CommandDescriptor compile(final String sourceDirectory,
			final String outputFile, final List<String> classpath) {
		return compile(sourceDirectory, outputFile, classpath.toArray(new String[0]));
	}
	
	/**
	 * Configures a {@link CommandDescriptor} to compile a source directory
	 * 
	 * @param sourceDirectory
	 *            The directory containing all the java sources
	 * @param outputFile
	 *            The name of the output jar file
	 * @param classpath
	 *            The source extra classpath
	 * @return a {@link CommandDescriptor} with the necessary configuration to
	 *         compile the sources
	 */
	public static CommandDescriptor compile(final String sourceDirectory,
			final String outputFile, final String... classpath) {
		String[] errors = { "No existe el fichero o el directorio",
				"Permiso denegado", "No such file or directory",
				"Permission denied" };
		CommandDescriptor compile = new CommandDescriptor.Builder()
				.withCommand("frascati")
				.withArguments("compile", sourceDirectory, outputFile, Strings.join(classpath, ":"))
				.withErrorText(errors)
				.withErrorMessage(outputFile + ".jar could not been generated")
				.withSuccessMessage(outputFile + ".jar has been generated")
				.build();
		return compile;
	}
	
	/**
	 * Configures a {@link CommandDescriptor} to check the java version
	 * 
	 * @param version
	 *            The expected java version
	 * @return a {@link CommandDescriptor} with the necessary configuration to
	 *         check if the java installed in the host is compliant with the
	 *         expected one.
	 */
	public static CommandDescriptor checkJavaVersion(final Version version) {
		return checkVersion(version, "java", "java -version",
				"java version \"(.*)\"");
	}

	/**
	 * Configures a {@link CommandDescriptor} to check the FraSCAti version
	 * 
	 * @param version
	 *            The expected FraSCAti version
	 * @return a {@link CommandDescriptor} with the necessary configuration to
	 *         check if the FraSCAti installed in the host is compliant with the
	 *         expected one.
	 */
	public static CommandDescriptor checkFrascatiVersion(
			final Version version) {
		return checkVersion(version, "FraSCAti", "frascati --version",
				"OW2 FraSCAti version (([0-9]|\\.)*)");
	}
	
	/**
	 * Configures a {@link CommandDescriptor} to check a program version
	 * 
	 * @param version
	 *            The expected program version
	 * @param programName
	 *            The name of the program being checked
	 * @param versionCommand
	 *            The command to show the current program version
	 * @param regexp
	 *            A regular expression to parse the command's output (version
	 *            must be group 1)
	 * @return a {@link CommandDescriptor} with the necessary configuration to
	 *         check if the program installed in the host is compliant with the
	 *         expected one
	 */
	private static CommandDescriptor checkVersion(final Version version,
			final String programName, final String versionCommand,
			final String regexp) {
		CallableTask<Void> callable = new CallableTask<Void>() {
			@Override public Void call(Host host, String prompt)
					throws Exception {
				Expect expect = host.ssh().expect();
				expect.sendLine(versionCommand);
				Result actualVersion = expect.expect(Matchers.regexp(prompt));
				Pattern pattern = Pattern.compile(regexp);
				Matcher matcher = pattern.matcher(actualVersion.getBefore());
				if (matcher.find()) {
					if (!version.isCompliant(matcher.group(1))) {
						String message = "The " + programName + " version ("
								+ matcher.group(1) + ") is not compliant with "
								+ version;
						Log.error(host, message);
						throw new RuntimeException(
								message + " in host " + host);
					} else {
						Log.success(host, "The " + programName + " version is Ok");
					}
				} else {
					Log.warning(host,
							"The " + programName
									+ " version could not be verified. Unknown version "
									+ actualVersion.getBefore());
				}
				return null;
			}
		};
		CommandDescriptor check = new CommandDescriptor.Builder()
				.withCallable(callable)
				.build();
		return check;
	}

}
