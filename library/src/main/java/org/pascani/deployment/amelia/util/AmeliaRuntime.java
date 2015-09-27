/*
 * Copyright © 2015 Universidad Icesi
 * 
 * This file is part of the Amelia library.
 * 
 * The Amelia library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
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
package org.pascani.deployment.amelia.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pascani.deployment.amelia.process.FTPHandler;
import org.pascani.deployment.amelia.process.SSHHandler;

/**
 * TODO
 * 
 * @author Miguel Jiménez - Initial contribution and API
 */
public class AmeliaRuntime {

	/**
	 * A map to store initial configuration parameters
	 */
	private static Map<String, String> configuration;

	private static Map<String, SSHHandler> sshConnections = new Hashtable<String, SSHHandler>();

	private static Map<String, FTPHandler> ftpConnections = new Hashtable<String, FTPHandler>();

	/**
	 * The logger
	 */
	private final static Logger logger = LogManager
			.getLogger(AmeliaRuntime.class);

	public static void storeConnection(String key, SSHHandler handler) {
		sshConnections.put(key, handler);
	}

	public static void storeConnection(String key, FTPHandler handler) {
		ftpConnections.put(key, handler);
	}

	public static SSHHandler getSSHConnection(String key) {
		return sshConnections.get(key);
	}

	public static FTPHandler getFTPConnection(String key) {
		return ftpConnections.get(key);
	}

	public static Map<String, SSHHandler> SSHConnections() {
		return sshConnections;
	}

	public static Map<String, FTPHandler> FTPConnections() {
		return ftpConnections;
	}

	/**
	 * @return the configured value for the specified entry
	 */
	public static String getConfigurationEntry(String key) {
		if (configuration == null)
			readConfiguration();

		return configuration.get(key);
	}

	/**
	 * Reads configuration properties
	 */
	private static void readConfiguration() {
		Properties config = new Properties();
		InputStream input = null;
		boolean ok = false;

		try {
			input = AmeliaRuntime.class.getClassLoader().getResourceAsStream(
					"amelia.properties");
			if (input != null) {
				config.load(input);
				ok = true;
			}
		} catch (FileNotFoundException e) {
			logger.warn("No configuration file was found. Execution is started with default values");
		} catch (IOException e) {
			logger.error("Error loading configuration file. Execution is started with default values");
		} finally {
			if (!ok) {
				String home = System.getProperty("user.home");

				// Set defaults
				config.put("identity", home + "/.ssh/id_rsa");
				config.put("known_hosts", home + "/.ssh/known_hosts");
				config.put("connection_timeout", "0"); // No timeout
			}

			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					logger.error("Error closing stream of configuration file",
							e);
				}
			}
		}

		configuration = new HashMap<String, String>();

		for (Object key : config.keySet()) {
			String name = (String) key;
			configuration.put(name, config.getProperty(name));
		}
	}

}
