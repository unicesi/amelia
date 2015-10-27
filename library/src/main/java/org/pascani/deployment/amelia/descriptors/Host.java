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
package org.pascani.deployment.amelia.descriptors;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pascani.deployment.amelia.FTPHandler;
import org.pascani.deployment.amelia.SSHHandler;
import org.pascani.deployment.amelia.util.Log;

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public class Host implements Comparable<Host> {

	private final String identifier;

	private final String hostname;

	private final int ftpPort;

	private final int sshPort;

	private final String username;

	private final String password;

	private final SSHHandler ssh;

	private final FTPHandler ftp;

	private int fixedWith;

	/**
	 * The logger
	 */
	private final static Logger logger = LogManager.getLogger(Host.class);

	public Host(final String hostname, final int ftpPort, final int sshPort,
			final String username, final String password,
			final String identifier) {
		this.identifier = identifier;
		this.hostname = hostname;
		this.ftpPort = ftpPort;
		this.sshPort = sshPort;
		this.username = username;
		this.password = password;

		this.ssh = new SSHHandler(this);
		this.ftp = new FTPHandler(this);

		this.fixedWith = toString().length();
	}

	public Host(final String hostname, final int ftpPort, final int sshPort,
			final String username, final String password) {
		this(hostname, ftpPort, sshPort, username, password, UUID.randomUUID()
				.toString());
	}

	public void openSSHConnection() throws InterruptedException {
		this.ssh.start();
		this.ssh.join();

		if (this.ssh.isConnected())
			Log.info(this, "");
	}

	public void closeSSHConnection() throws IOException {
		this.ssh.close();
	}

	public void openFTPConnection() throws InterruptedException {
		this.ftp.start();
		this.ftp.join();

		if (this.ftp.client().isConnected())
			Log.info(this, "");
	}

	public boolean closeFTPConnection() throws IOException {
		return this.ftp.close();
	}

	public void stopExecutions() throws IOException {
		this.ssh.stopExecutions();
	}

	public int stopExecutions(List<Execution> executions) throws IOException {
		return this.ssh.stopExecutions(executions);
	}

	public static Host[] fromFile(String pathname) throws IOException {
		List<Host> hosts = new ArrayList<Host>();

		InputStream in = new FileInputStream(pathname);
		InputStreamReader streamReader = null;
		BufferedReader bufferedReader = null;

		try {
			streamReader = new InputStreamReader(in);
			bufferedReader = new BufferedReader(streamReader);

			String line;
			int l = 1;
			while ((line = bufferedReader.readLine()) != null) {

				String[] d = line.split("\t");

				if (d.length == 5) {
					int ftpPort = Integer.parseInt(d[1]);
					int sshPort = Integer.parseInt(d[2]);
					hosts.add(new Host(d[0], ftpPort, sshPort, d[3], d[4]));

				} else if (d.length == 6) {
					int ftpPort = Integer.parseInt(d[1]);
					int sshPort = Integer.parseInt(d[2]);
					hosts.add(new Host(d[0], ftpPort, sshPort, d[3], d[4], d[5]));

				} else {
					String message = "Bad format in hosts file: [" + l + "] "
							+ line;
					RuntimeException e = new RuntimeException(message);
					logger.error(message, e);

					throw e;
				}

				++l;
			}
		} finally {
			in.close();
			streamReader.close();
			bufferedReader.close();
		}

		return hosts.toArray(new Host[0]);
	}

	public String identifier() {
		return this.identifier;
	}

	public String hostname() {
		return this.hostname;
	}

	public int ftpPort() {
		return this.ftpPort;
	}

	public int sshPort() {
		return this.sshPort;
	}

	public String username() {
		return this.username;
	}

	public String password() {
		return this.password;
	}

	public SSHHandler ssh() {
		return this.ssh;
	}

	public FTPHandler ftp() {
		return this.ftp;
	}

	public void setFixedWidth(int width) {
		this.fixedWith = width;
	}

	public String toFixedString() {
		return String.format("  %-" + this.fixedWith + "s", toString());
	}

	@Override
	public String toString() {
		return this.identifier;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((this.identifier == null) ? 0 : this.identifier.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Host other = (Host) obj;
		if (this.identifier == null) {
			if (other.identifier != null) {
				return false;
			}
		} else if (!this.identifier.equals(other.identifier)) {
			return false;
		}
		return true;
	}

	public int compareTo(Host o) {
		return this.identifier.compareTo(o.identifier);
	}

}
