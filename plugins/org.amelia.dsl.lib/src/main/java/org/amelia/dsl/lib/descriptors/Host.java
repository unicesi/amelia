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
package org.amelia.dsl.lib.descriptors;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.amelia.dsl.lib.FTPHandler;
import org.amelia.dsl.lib.SSHHandler;
import org.amelia.dsl.lib.util.Log;
import org.amelia.dsl.lib.util.Strings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Miguel Jiménez - Initial contribution and API
 * 
 * TODO: document this class
 */
public class Host implements Comparable<Host> {

	private final String identifier;

	private final String hostname;

	private final int ftpPort;

	private final int sshPort;

	private final String username;

	private final String password;

	private SSHHandler ssh;

	private FTPHandler ftp;

	private int fixedWith;

	/**
	 * The logger
	 */
	private final static Logger logger = LogManager.getLogger(Host.class);
	
	/*
	 * From: https://en.wikipedia.org/wiki/List_of_Disney_animated_universe_characters
	 */
	private final static String[] randomNames = {"mandy", "billy", "nemo",
		"doris","abby", "alice", "apollo", "ariel", "aurora", "bambi", "ben",
		"blaze", "bobbie", "buster"};
	
	private final static List<String> pickedNames = new ArrayList<String>();

	public Host(final String hostname, final int ftpPort, final int sshPort,
			final String username, final String password,
			final String identifier) {
		this.identifier = identifier;
		this.hostname = hostname;
		this.ftpPort = ftpPort;
		this.sshPort = sshPort;
		this.username = username;
		this.password = password;
		this.fixedWith = toString().length();
	}

	public Host(final String hostname, final int ftpPort, final int sshPort,
			final String username, final String password) {
		this(hostname, ftpPort, sshPort, username, password, randomName());
	}

	public void openSSHConnection(String subsystem) throws InterruptedException {
		if (this.ssh == null)
			this.ssh = new SSHHandler(this, subsystem);

		if (!this.ssh.isConnected()) {
			this.ssh.start();
			this.ssh.join();

			if (this.ssh.isConnected())
				Log.ok(this, "Connection established");
		} else {
			Log.ok(this, "Already opened");
		}
	}

	public void closeSSHConnection() throws IOException {
		if (this.ssh != null)
			this.ssh.close();
	}

	public void openFTPConnection() throws InterruptedException {
		if (this.ftp == null)
			this.ftp = new FTPHandler(this);

		if (!this.ftp.isConnected()) {
			this.ftp.start();
			this.ftp.join();

			if (this.ftp.client().isConnected())
				Log.ok(this, "Connection established");
		} else {
			Log.ok(this, "Already opened");
		}
	}

	public void closeFTPConnection() throws IOException {
		if (this.ftp != null)
			this.ftp.close();
	}

	public void stopExecutions() throws IOException {
		this.ssh.stopExecutions();
	}

	public int stopExecutions(List<CommandDescriptor> executions) throws IOException {
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
	
	private static String randomName() {
		Random g = new Random();
		String name = randomNames[g.nextInt(randomNames.length)];
		String proposal = name;
		int i = 1;
		while (pickedNames.contains(proposal)) {
			proposal = name + "-" + i++;
		}
		pickedNames.add(proposal);
		return proposal;
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
		return Strings.center(toString(), this.fixedWith);
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
