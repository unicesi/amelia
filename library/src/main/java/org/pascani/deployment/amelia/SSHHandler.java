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
package org.pascani.deployment.amelia;

import static net.sf.expectit.filter.Filters.removeColors;
import static net.sf.expectit.filter.Filters.removeNonPrintable;
import static net.sf.expectit.matcher.Matchers.regexp;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import net.sf.expectit.Expect;
import net.sf.expectit.ExpectBuilder;
import net.sf.expectit.Result;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pascani.deployment.amelia.commands.Run;
import org.pascani.deployment.amelia.descriptors.Execution;
import org.pascani.deployment.amelia.descriptors.Host;
import org.pascani.deployment.amelia.util.AuthenticationUserInfo;
import org.pascani.deployment.amelia.util.Log;
import org.pascani.deployment.amelia.util.ShellUtils;
import org.pascani.deployment.amelia.util.Strings;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public class SSHHandler extends Thread {

	private final Host host;

	private Session session;

	private Channel channel;

	private Expect expect;

	private final int connectionTimeout;

	private final int executionTimeout;

	private File output;

	private final List<Execution> executions;

	private final SingleThreadTaskQueue taskQueue;

	/**
	 * The logger
	 */
	private final static Logger logger = LogManager.getLogger(SSHHandler.class);

	public SSHHandler(final Host host) {
		this.host = host;

		String _connectionTimeout = Amelia
				.getConfigurationEntry("connection_timeout");
		String _executionTimeout = Amelia
				.getConfigurationEntry("execution_timeout");

		this.connectionTimeout = Integer.parseInt(_connectionTimeout);
		this.executionTimeout = Integer.parseInt(_executionTimeout);
		this.executions = new ArrayList<Execution>();
		this.taskQueue = new SingleThreadTaskQueue();

		// Handle uncaught exceptions
		this.setUncaughtExceptionHandler(Amelia.exceptionHandler);
		this.taskQueue.setUncaughtExceptionHandler(Amelia.exceptionHandler);
	}

	@Override
	public void run() {
		try {
			connect();
			initialize();
			configure();

			// Once it's configured, it's ready to execute commands
			this.taskQueue.start();

		} catch (JSchException e) {
			String message = "Error establishing SSH connection with "
					+ this.host;

			Log.error(this.host, "");
			throw new RuntimeException(message, e);
		} catch (IOException e) {
			String message = "Error initializing SSH connection with "
					+ this.host;

			Log.error(this.host, "");
			throw new RuntimeException(message, e);
		}
	}

	private void connect() throws JSchException, IOException {
		JSch jsch = new JSch();

		String identity = Amelia.getConfigurationEntry("identity");
		String knownHosts = Amelia.getConfigurationEntry("known_hosts");

		if (new File(identity).exists())
			jsch.addIdentity(identity);
		else
			logger.warn("Identity file '" + identity
					+ "' not found. Execution will continue without it");

		if (new File(knownHosts).exists())
			jsch.setKnownHosts(knownHosts);
		else
			logger.warn("Known hosts file '" + knownHosts
					+ "' not found. Execution will continue without it");

		this.session = jsch.getSession(this.host.username(),
				this.host.hostname(), this.host.sshPort());

		if (this.host.password() != null)
			this.session.setPassword(this.host.password());

		UserInfo ui = new AuthenticationUserInfo();

		this.session.setUserInfo(ui);
		this.session.connect(this.connectionTimeout);

		this.channel = session.openChannel("shell");
		this.channel.connect(this.connectionTimeout);
	}

	private void initialize() throws IOException {
		this.output = createOutputFile();
		PrintStream outputStream = new PrintStream(this.output, "UTF-8");

		this.expect = new ExpectBuilder()
				.withOutput(this.channel.getOutputStream())
				.withInputs(this.channel.getInputStream(),
						this.channel.getExtInputStream())
				.withEchoInput(outputStream).withEchoOutput(outputStream)
				.withInputFilters(removeColors(), removeNonPrintable())
				.withExceptionOnFailure()
				.withTimeout(this.executionTimeout, TimeUnit.MILLISECONDS)
				.build();
	}

	private void configure() throws IOException {
		String prompt = ShellUtils.ameliaPromptRegexp();
		String initialPrompt = "\\$|#";

		this.expect.expect(regexp(initialPrompt));

		// Switch off echo
		this.expect.sendLine("stty -echo");
		this.expect.expect(regexp(initialPrompt));

		// Query the current shell
		this.expect.sendLine(ShellUtils.currentShellCommand());
		Result result = this.expect.expect(regexp(initialPrompt));

		String shell = result.getBefore().split("\n")[0].trim();

		if (!shell.matches("bash|zsh")) {
			RuntimeException e = new RuntimeException("Shell not supported: "
					+ shell);
			logger.error("Shell not supported: " + shell, e);
			throw e;
		}

		// Change shell prompt to the Amelia prompt
		this.expect.sendLine(ShellUtils.ameliaPromptFormat(shell));
		this.expect.expect(regexp(prompt));
	}

	public <V> V executeCommand(Callable<V> command) throws InterruptedException {
		V result = this.taskQueue.execute(command);

		if (command instanceof Run) {
			Run run = (Run) command;
			Execution descriptor = (Execution) run.descriptor();
			this.executions.add(descriptor);
		}

		return result;
	}

	public int stopExecutions(List<Execution> executions) throws IOException {
		// TODO: Improve the search string to identify deployed composites when
		// the classpath is different (libraries are in different order)

		String prompt = ShellUtils.ameliaPromptRegexp();
		List<String> components = new ArrayList<String>();

		// Stop executions in reverse order (to avoid abruptly stopping
		// components)
		for (int i = executions.size() - 1; i >= 0; i--) {
			Execution descriptor = executions.remove(i);

			String criterion = descriptor.toCommandSearchString();
			this.expect.sendLine(ShellUtils.runningCompositeName(criterion));
			Result r = this.expect.expect(regexp(prompt));

			if (r.getBefore().contains(descriptor.compositeName())) {
				this.expect.sendLine(ShellUtils.killCommand(criterion));
				this.expect.expect(regexp(prompt));

				components.add(descriptor.compositeName());
				logger.info("Execution of composite "
						+ descriptor.compositeName()
						+ " was successfully stopped in " + this.host);
			}
		}

		if (!components.isEmpty()) {
			int nExecutions = components.size();
			String have = nExecutions == 1 ? " has " : " have ";
			String s = nExecutions == 1 ? "" : "s";
			String message = "Component" + s + " "
					+ Strings.join(components, ", ", " and ") + have
					+ "been stopped";
			Log.info(this.host, message);
		}

		return components.size();
	}

	public void stopExecutions() throws IOException {
		stopExecutions(this.executions);
	}

	public List<Execution> executions() {
		return this.executions;
	}

	public Expect expect() {
		return this.expect;
	}

	public void close() throws IOException {
		if (this.expect != null)
			this.expect.close();

		if (this.channel != null && this.channel.isConnected())
			this.channel.disconnect();

		if (this.session != null && this.session.isConnected())
			this.session.disconnect();
	}

	public boolean isConnected() {
		return this.session.isConnected() && this.channel.isConnected();
	}

	private File createOutputFile() throws IOException {
		String fileName = this.host + "-" + System.nanoTime() + ".txt";

		File parent = new File("sessions");
		File file = new File(parent, fileName);

		if (!parent.exists())
			parent.mkdir();

		file.createNewFile();

		return file;
	}

	public Host host() {
		return this.host;
	}

}
