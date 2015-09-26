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

import java.io.IOException;

import net.sf.expectit.Expect;
import net.sf.expectit.ExpectBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pascani.deployment.amelia.util.AmeliaRuntime;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

public class SessionHandler {

	private final int timeout;

	private final String host;

	private final int port;

	private final String user;

	private final String password;

	private Session session;

	private Channel channel;

	private Expect expect;

	/**
	 * The logger
	 */
	private final static Logger logger = LogManager
			.getLogger(SessionHandler.class);

	public SessionHandler(final String host, final int port, final String user,
			final String password) {
		this.host = host;
		this.port = port;
		this.user = user;
		this.password = password;

		String _timeout = AmeliaRuntime
				.getConfigurationEntry("connection_timeout");

		if (_timeout != null && !_timeout.isEmpty())
			this.timeout = Integer.parseInt(_timeout);
		else
			this.timeout = 0;
	}

	public SessionHandler(final String host, final int port, final String user) {
		this(host, port, user, null);
	}

	public Expect start() {
		try {

			connect();
			initialize();

		} catch (JSchException e) {
			logger.error("Error establishing connection with " + this.user
					+ "@" + this.host, e);
			e.printStackTrace();
		} catch (IOException e) {
			logger.error("Error initializing Expect for " + this.user + "@"
					+ this.host, e);
			e.printStackTrace();
		}

		return this.expect;
	}

	private void connect() throws JSchException {
		JSch jsch = new JSch();

		jsch.addIdentity(AmeliaRuntime.getConfigurationEntry("identity"));
		jsch.setKnownHosts(AmeliaRuntime.getConfigurationEntry("known_hosts"));

		this.session = jsch.getSession(this.user, this.host, this.port);
		if (this.password != null)
			this.session.setPassword(this.password);

		UserInfo ui = new AuthenticationUserInfo();
		session.setUserInfo(ui);

		this.session.connect(this.timeout);

		this.channel = session.openChannel("shell");
		this.channel.setInputStream(System.in);
		this.channel.setOutputStream(System.out);
		this.channel.connect(this.timeout);
	}

	private void initialize() throws IOException {
		this.expect = new ExpectBuilder()
				.withOutput(this.channel.getOutputStream())
				.withInputs(this.channel.getInputStream(),
						this.channel.getExtInputStream())
				.withEchoInput(System.out)
				// .withEchoOutput(System.out)
				.withInputFilters(removeColors(), removeNonPrintable())
				.withExceptionOnFailure().build();
	}

	public void close() throws IOException {
		expect.close();
		channel.disconnect();
		session.disconnect();
	}

}
