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
package org.pascani.deployment.amelia.process;

import static net.sf.expectit.filter.Filters.removeColors;
import static net.sf.expectit.filter.Filters.removeNonPrintable;
import static net.sf.expectit.matcher.Matchers.regexp;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import net.sf.expectit.Expect;
import net.sf.expectit.ExpectBuilder;
import net.sf.expectit.Result;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pascani.deployment.amelia.descriptors.Host;
import org.pascani.deployment.amelia.util.Amelia;
import org.pascani.deployment.amelia.util.ShellUtils;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

/**
 * TODO
 * 
 * @author Miguel Jiménez - Initial contribution and API
 */
public class SSHHandler extends Thread {

	private final Host host;

	private Session session;

	private Channel channel;

	private Expect expect;

	private final int timeout;

	private File output;

	/**
	 * The logger
	 */
	private final static Logger logger = LogManager.getLogger(SSHHandler.class);

	public SSHHandler(final Host host) {
		this.host = host;

		String _timeout = Amelia
				.getConfigurationEntry("connection_timeout");

		if (_timeout != null && !_timeout.isEmpty())
			this.timeout = Integer.parseInt(_timeout);
		else
			this.timeout = 0;
	}

	@Override
	public void run() {
		try {

			connect();
			initialize();
			configure();

		} catch (JSchException e) {
			logger.error("Error establishing connection with " + this.host, e);
			e.printStackTrace();
		} catch (IOException e) {
			logger.error("Error initializing connection for " + this.host, e);
			e.printStackTrace();
		}
	}

	private void connect() throws JSchException, IOException {
		JSch jsch = new JSch();

		jsch.addIdentity(Amelia.getConfigurationEntry("identity"));
		jsch.setKnownHosts(Amelia.getConfigurationEntry("known_hosts"));

		this.session = jsch.getSession(this.host.username(),
				this.host.hostname(), this.host.sshPort());

		if (this.host.password() != null)
			this.session.setPassword(this.host.password());

		UserInfo ui = new AuthenticationUserInfo();

		this.session.setUserInfo(ui);
		this.session.connect(this.timeout);

		this.channel = session.openChannel("shell");
		this.channel.connect(this.timeout);
	}

	private void initialize() throws IOException {
		this.output = createOutputFile();
		PrintStream outputStream = new PrintStream(this.output, "UTF-8");
		
		this.expect = new ExpectBuilder()
				.withOutput(this.channel.getOutputStream())
				.withInputs(this.channel.getInputStream(), this.channel.getExtInputStream())
				.withEchoInput(outputStream)
				.withEchoOutput(outputStream)
				.withInputFilters(removeColors(), removeNonPrintable())
				.withExceptionOnFailure()
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
		
		if(!shell.matches("bash|zsh")) {
			RuntimeException e = new RuntimeException("Shell not supported: " + shell);
			logger.error("Shell not supported: " + shell, e);
			
			throw e;
		}

		// Change shell prompt to the Amelia prompt
		this.expect.sendLine(ShellUtils.ameliaPromptFormat(shell));
		this.expect.expect(regexp(prompt));
	}

	public Expect expect() {
		return this.expect;
	}

	public void close() throws IOException {
		expect.close();
		channel.disconnect();
		session.disconnect();
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
