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

import static net.sf.expectit.matcher.Matchers.regexp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.amelia.dsl.lib.descriptors.AssetBundle;
import org.amelia.dsl.lib.descriptors.CommandDescriptor;
import org.amelia.dsl.lib.descriptors.Host;
import org.amelia.dsl.lib.descriptors.Version;
import org.ow2.scesame.qoscare.core.scaspec.SCANamedNode;
import org.pascani.dsl.lib.sca.FrascatiUtils;

import com.google.common.collect.Lists;

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
		
		public String compositeName;
		public int port;
		public String[] libpath;
		public String serviceName;
		public String methodName;
		public String[] arguments;
		public long timeout;
		public String releaseRegexp;
		public String successMessage;
		public String[] errorTexts;

		public RunBuilder() {
			this.port = -1;
			this.timeout = 0;
			this.releaseRegexp = "Press Ctrl\\+C to quit\\.\\.\\.|Call done!";
			this.errorTexts = new String[0];
		}

		public RunBuilder withComposite(final String compositeName) {
			this.compositeName = compositeName;
			return this;
		}
		
		public RunBuilder withPort(final int port) {
			this.port = port;
			return this;
		}
		
		public RunBuilder withLibpath(final List<String> libs) {
			return withLibpath(libs.toArray(new String[0]));
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
		
		public RunBuilder withArguments(final List<String> arguments) {
			return withArguments(arguments.toArray(new String[0]));
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
		
		public RunBuilder withErrorTexts(final List<String> errorTexts) {
			return withErrorTexts(errorTexts.toArray(new String[0]));
		}
		
		public RunBuilder withErrorTexts(String... errorTexts) {
			this.errorTexts = errorTexts;
			return this;
		}

		/**
		 * @return a {@link CommandDescriptor} with the necessary configuration
		 *         to run the given composite
		 */
		public CommandDescriptor build() {
			if (this.successMessage == null)
				this.successMessage = this.compositeName + " has been executed";

			List<String> arguments = new ArrayList<String>();
			arguments.add("run");
			if (port != -1) {
				arguments.add("-r");
				arguments.add(String.valueOf(port));
			}
			arguments.add(this.compositeName);
			arguments.add("-libpath");
			arguments.add(Arrays.join(this.libpath, ":"));
			if (this.serviceName != null)
				arguments.add("-s " + this.serviceName);
			if (this.methodName != null)
				arguments.add("-m " + this.methodName);
			if (this.arguments != null)
				arguments.add("-p " + Arrays.join(this.arguments, " "));

			CommandDescriptor run = new CommandDescriptor.Builder()
					.withCallable(callableTask(arguments))
					.withCommand("frascati")
					.withArguments(arguments.toArray(new String[0]))
					.withReleaseRegexp(this.releaseRegexp)
					.withSuccessMessage(this.successMessage)
					.withErrorText(this.errorTexts)
					.withTimeout(this.timeout)
					.isExecution()
					.build();
			return run;
		}
		
		private CallableTask<Integer> callableTask(final List<String> arguments) {
			final String[] errors = Arrays.concatAll(errorTexts,
					new String[] {
							"Error when parsing the composite file '" + compositeName + "'",
							"Could not start the SCA composite '" + compositeName + "'",
							"Could not start Jetty server",
							"Address already in use",
							"Cannot instantiate the FraSCAti factory" });
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
					expect.sendLine("frascati "
							+ Arrays.join(arguments.toArray(new String[0]), " ") + " &");
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
		return cdBuilder(directory).build();
	}
	
	/**
	 * Configures a {@link CommandDescriptor.Builder} to change the current working
	 * directory
	 * 
	 * @param directory
	 *            The new working directory
	 * @return a {@link CommandDescriptor.Builder} with the necessary configuration to
	 *         change the working directory
	 */
	public static CommandDescriptor.Builder cdBuilder(final String directory) {
		String errorMessage = "Could not change working directory to " + ANSI.YELLOW.format(directory);
		String successMessage = "New working directory: " + ANSI.YELLOW.format(directory);
		String[] errors = { "No existe el fichero o el directorio", 
				"No existe el archivo o el directorio",
				"Permiso denegado", "No such file or directory",
				"Permission denied" };
		CommandDescriptor.Builder cd = new CommandDescriptor.Builder()
				.withCommand("cd")
				.withArguments(directory)
				.withErrorText(errors)
				.withErrorMessage(errorMessage)
				.withSuccessMessage(successMessage);
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
		return compileBuilder(sourceDirectory, outputFile, classpath).build();
	}
	
	/**
	 * Configures a {@link CommandDescriptor.Builder} to compile a source
	 * directory
	 * 
	 * @param sourceDirectory
	 *            The directory containing all the java sources
	 * @param outputFile
	 *            The name of the output jar file
	 * @param classpath
	 *            The source extra classpath
	 * @return a {@link CommandDescriptor.Builder} with the necessary
	 *         configuration to compile the sources
	 */
	public static CommandDescriptor.Builder compileBuilder(final String sourceDirectory,
			final String outputFile, final List<String> classpath) {
		return compileBuilder(sourceDirectory, outputFile, classpath.toArray(new String[0]));
	}
	
	/**
	 * Configures a {@link CommandDescriptor.Builder} to compile a source
	 * directory
	 * 
	 * @param sourceDirectory
	 *            The directory containing all the java sources
	 * @param outputFile
	 *            The name of the output jar file
	 * @param classpath
	 *            The source extra classpath
	 * @return a {@link CommandDescriptor.Builder} with the necessary
	 *         configuration to compile the sources
	 */
	public static CommandDescriptor.Builder compileBuilder(final String sourceDirectory,
			final String outputFile, final String... classpath) {
		String[] errors = { "No existe el fichero o el directorio",
				"No existe el archivo o el directorio",
				"Permiso denegado", "No such file or directory",
				"Permission denied" };
		CommandDescriptor.Builder compile = new CommandDescriptor.Builder()
				.withCommand("frascati")
				.withArguments("compile", sourceDirectory, outputFile, Arrays.join(classpath, ":"))
				.withErrorText(errors)
				.withErrorMessage(outputFile + ".jar could not been generated")
				.withSuccessMessage(outputFile + ".jar has been generated");
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
				.withCommand(versionCommand)
				.withCallable(callable)
				.build();
		return check;
	}
	
	/**
	 * Configures a {@link CommandDescriptor} to execute the given command
	 * expression
	 * 
	 * @param command
	 *            The command expression
	 * @param errorTexts
	 *            The possible error texts to catch if something goes wrong
	 * @return A new {@link CommandDescriptor} instance configured with the
	 *         given command expression.
	 */
	public static CommandDescriptor generic(final String command, final String... errorTexts) {
		CommandDescriptor result = new CommandDescriptor.Builder()
				.withErrorText(errorTexts)
				.withCommand(command).build();
		return result;
	}
	
	/**
	 * Configures a {@link CommandDescriptor} to evaluate the given FScript in
	 * the specified FraSCAti runtime.
	 * 
	 * @param script
	 *            The script to evaluate
	 * @param bindingUri
	 *            The URI where the FraSCAti runtime is running
	 * @return A new {@link CommandDescriptor} instance configured to evaluate
	 *         the given script.
	 */
	public static CommandDescriptor evalFScript(final String script, final URI bindingUri) {
		CommandDescriptor eval = new CommandDescriptor.Builder()
				.withCallable(new CallableTask<Collection<SCANamedNode>>() {
					@Override public Collection<SCANamedNode> call(Host host,
							String prompt) throws Exception {
						return FrascatiUtils.eval(script, bindingUri);
					}
				}).build();
		return eval;
	}
	
	/**
	 * Configures a {@link CommandDescriptor} to register the given FScript file
	 * in the specified FraSCAti runtime.
	 * 
	 * @param script
	 *            A {@link File} pointing to the FScript to register
	 * @param bindingUri
	 *            The URI where the FraSCAti runtime is running
	 * @return A new {@link CommandDescriptor} instance configured to register
	 *         the given FScript file.
	 */
	public static CommandDescriptor registerFScript(final File script, final URI bindingUri) {
		CommandDescriptor register = new CommandDescriptor.Builder()
				.withCallable(new CallableTask<List<String>>() {
					@Override public List<String> call(Host host,
							String prompt) throws Exception {
						return FrascatiUtils.registerScript(script, bindingUri);
					}
				}).build();
		return register;
	}
	
	/**
	 * Configures a {@link CommandDescriptor} to register the given FScript in
	 * the specified FraSCAti runtime.
	 * 
	 * @param script
	 *            The FScript to register
	 * @param bindingUri
	 *            The URI where the FraSCAti runtime is running
	 * @return A new {@link CommandDescriptor} instance configured to register
	 *         the given FScript.
	 */
	public static CommandDescriptor registerFScript(final String script, final URI bindingUri) {
		CommandDescriptor register = new CommandDescriptor.Builder()
				.withCallable(new CallableTask<List<String>>() {
					@Override public List<String> call(Host host,
							String prompt) throws Exception {
						return FrascatiUtils.registerScript(script, bindingUri);
					}
				}).build();
		return register;
	}
	
	/**
	 * Configures an {@link AssetBundle} to transfer the given local file or
	 * directory
	 * 
	 * @param source
	 *            The local path of the file or directory to transfer
	 * @param destination
	 *            The destination paths where the file or directory will be
	 *            transfered
	 * @param overwrite
	 *            Whether to overwrite or not the remote file or directory if it
	 *            already exists
	 * @return A new {@link AssetBundle} instance configured with the given
	 *         source and destination.
	 */
	public static AssetBundle scp(final String source,
			final Iterable<String> destination, final boolean overwrite) {
		Map<String, List<String>> transfers = new HashMap<String, List<String>>();
		transfers.put(source, Lists.newArrayList(destination));
		return new AssetBundle(transfers, overwrite);
	}

	/**
	 * Configures an {@link AssetBundle} to transfer the given local file or
	 * directory
	 * 
	 * @param source
	 *            The local path of the file or directory to transfer
	 * @param destination
	 *            The destination path where the file or directory will be
	 *            transfered
	 * @param overwrite
	 *            Whether to overwrite or not the remote file or directory if it
	 *            already exists
	 * @return A new {@link AssetBundle} instance configured with the given
	 *         source and destination.
	 */
	public static AssetBundle scp(final String source,
			final String destination, final boolean overwrite) {
		return scp(source, Lists.newArrayList(destination), overwrite);
	}
	
	/**
	 * Configures an {@link AssetBundle} from a given plain file.
	 * 
	 * <p>
	 * Each row must contain the following columns (each column must be
	 * separated using the tab character):
	 * <ul>
	 * <li>source (local path)
	 * <li>destination (remote path)
	 * </ul>
	 * 
	 * @param filepath
	 *            The path of the file containing the transfers
	 * @return A new {@link AssetBundle} instance configured with sources and
	 *         destinations from the given file.
	 * @throws IOException
	 *             if something bad happens while reading the file
	 */
	public static AssetBundle scp(final String filepath) throws IOException {
		AssetBundle bundle = new AssetBundle();
		InputStream in = new FileInputStream(filepath);
		InputStreamReader streamReader = null;
		BufferedReader bufferedReader = null;
		try {
			streamReader = new InputStreamReader(in);
			bufferedReader = new BufferedReader(streamReader);
			String line;
			int l = 1;
			while ((line = bufferedReader.readLine()) != null) {
				if (!line.isEmpty() && line.contains("\t")) {
					String[] data = line.split("\t");
					bundle.add(data[0], data[1]);
				} else {
					String message = "Bad format in asset bundle: [" + l + "] " + line;
					throw new RuntimeException(message);
				}
				++l;
			}
		} finally {
			in.close();
			streamReader.close();
			bufferedReader.close();
		}
		return bundle;
	}

}
