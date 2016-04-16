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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.amelia.dsl.lib.descriptors.Host;

/**
 * @author Miguel Jiménez - Initial contribution and API
 * 
 * TODO: document this class
 */
public class Hosts {

	public static Host host(final String hostname, final int ftpPort,
			final int sshPort, final String username, final String password,
			final String identifier) {
		return new Host(hostname, ftpPort, sshPort, username, password,
				identifier);
	}

	public static Host host(final String hostname, final String username,
			final String password, final String identifier) {
		return new Host(hostname, 21, 22, username, password, identifier);
	}

	public static Host host(final String hostname, final String username,
			final String password) {
		return new Host(hostname, 21, 22, username, password);
	}
	
	/**
	 * Reads a plain file and instantiate as many hosts as lines are in the
	 * file.
	 * 
	 * <p>
	 * Each row must contain the following columns (each column must be
	 * separated using the tab character):
	 * <ul>
	 * <li>host
	 * <li>FTP port
	 * <li>SSH port
	 * <li>user
	 * <li>password
	 * <li>[optional] identifier
	 * </ul>
	 * 
	 * @param filepath
	 *            The hosts file path
	 * @return a list of hosts
	 * @throws IOException
	 *             if something bad happens while reading the file
	 */
	public static List<Host> hosts(String filepath) throws IOException {
		List<Host> hosts = new ArrayList<Host>();
		InputStream in = new FileInputStream(filepath);
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
					throw new RuntimeException(message);
				}

				++l;
			}
		} finally {
			in.close();
			streamReader.close();
			bufferedReader.close();
		}
		return hosts;
	}

}
