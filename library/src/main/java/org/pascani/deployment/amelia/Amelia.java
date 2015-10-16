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
package org.pascani.deployment.amelia;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pascani.deployment.amelia.descriptors.Host;
import org.pascani.deployment.amelia.util.Log;

/**
 * This class is a repository of the SSH and FTP connections.
 * 
 * <p>
 * Additionally, it contains the initial configuration parameters of execution.
 * </p>
 * 
 * @author Miguel Jiménez - Initial contribution and API
 */
public class Amelia {

	private static int hostFixedWidth = 0;

	/**
	 * A map to store initial configuration parameters
	 */
	private static Map<String, String> configuration;

	/**
	 * A map to store SSH handlers per host (host id)
	 */
	private static Map<String, Host> hosts = new Hashtable<String, Host>();

	public static volatile boolean aborting = false;
	
	public static volatile boolean shuttingDown = false;

	/**
	 * An uncaught exception handler
	 */
	public static final Thread.UncaughtExceptionHandler exceptionHandler = new Thread.UncaughtExceptionHandler() {
		public void uncaughtException(Thread t, Throwable e) {
			if (!aborting && !shuttingDown) {
				// Prevent showing error messages raised because of the first
				// reported error
				aborting = true;

				String message = e
						.getMessage()
						.replace("java.util.concurrent.ExecutionException: ", "")
						.replace("java.lang.RuntimeException: ", "")
						.replace("org.pascani.deployment.amelia.DeploymentException: ", "");

				logger.error(message, e);
				Log.error("Stopping deployment: " + message);
				
				Amelia.shutdown(true);
			}
		}
	};

	/**
	 * The logger
	 */
	private final static Logger logger = LogManager.getLogger(Amelia.class);

	/**
	 * Opens FTP connections with the specified hosts
	 * 
	 * @param hosts
	 *            The array of hosts containing the FTP handler
	 * @throws InterruptedException
	 *             If any thread interrupts any of the handler threads
	 */
	public static void openFTPConnections(Host... _hosts)
			throws InterruptedException {
		Log.heading("Establishing FTP connections");
		
		for (Host host : _hosts) {
			host.openFTPConnection();

			if (!hosts.containsKey(host.identifier()))
				hosts.put(host.identifier(), host);

			logger.info("FTP connection for " + host
					+ " was successfully established");
		}
	}

	/**
	 * Opens SSH connections with the specified hosts
	 * 
	 * @param hosts
	 *            The array of hosts containing the SSH handler
	 * @throws InterruptedException
	 *             If any thread interrupts any of the handler threads
	 */
	public static void openSSHConnections(Host... _hosts)
			throws InterruptedException {
		Log.heading("Establishing SSH connections");
		
		for (Host host : _hosts) {
			host.openSSHConnection();

			if (!hosts.containsKey(host.identifier())) {
				hosts.put(host.identifier(), host);

				if (hostFixedWidth < host.toString().length())
					hostFixedWidth = host.toString().length();
			}

			if(host.ssh().isConnected())
				logger.info("SSH connection for " + host
						+ " was successfully established");
		}

		// set a common (fixed) with for all hosts
		for (Map.Entry<String, Host> entry : hosts.entrySet()) {
			entry.getValue().setFixedWidth(hostFixedWidth);
		}
	}

	/**
	 * Closes the FTP connection with the specified hosts.
	 * 
	 * @param hosts
	 *            The array of hosts to close the connection with
	 * @throws IOException
	 *             If an I/O error occurs while either sending a command to the
	 *             server or receiving a reply from the server.
	 */
	public static void closeFTPConnections(Host... hosts) throws IOException {
		String[] ids = new String[hosts.length];

		for (int i = 0; i < ids.length; i++)
			ids[i] = hosts[i].identifier();

		closeFTPConnections(ids);
	}

	private static void closeFTPConnections(String... hostsIds)
			throws IOException {
		for (String id : hostsIds) {
			Host host = hosts.get(id);
			host.closeFTPConnection();

			logger.info("FTP connection for " + host
					+ " was successfully closed");
		}
	}

	/**
	 * Closes the SSH connection with the specified hosts. Before closing the
	 * connections, all FraSCAti executions are stopped.
	 * 
	 * @param hosts
	 *            The array of hosts to close the connection with
	 * @throws IOException
	 *             If I/O error occurs.
	 */
	public static void closeSSHConnections(Host... hosts) throws IOException {
		String[] ids = new String[hosts.length];

		for (int i = 0; i < ids.length; i++)
			ids[i] = hosts[i].identifier();

		closeSSHConnections(ids);
	}

	private static void closeSSHConnections(String... hostsIds)
			throws IOException {
		for (String id : hostsIds) {
			Host host = hosts.get(id);
			host.closeSSHConnection();

			logger.info("SSH connection for " + host
					+ " was successfully closed");
		}
	}

	/**
	 * Stops FraSCAti components in execution, within the specified hosts.
	 * 
	 * @param hosts
	 *            The array of hosts
	 * @throws IOException
	 *             If I/O error occurs.
	 */
	public static void stopExecutions(Host... hosts) throws IOException {
		String[] ids = new String[hosts.length];

		for (int i = 0; i < ids.length; i++)
			ids[i] = hosts[i].identifier();

		stopExecutions(ids);
	}

	private static void stopExecutions(String... hostsIds) throws IOException {
		for (String id : hostsIds) {
			Host host = hosts.get(id);
			host.stopExecutions();
		}
	}

	/**
	 * Terminates the execution
	 */
	public static void shutdown(boolean stopExecutedComponents) {
		if(!shuttingDown) {
			// Prevent shutting down more than once
			shuttingDown = true;
			
			Log.heading("Starting deployment shutdown");
			try {
				String[] _hosts = hosts.keySet().toArray(new String[0]);
	
				if (stopExecutedComponents)
					stopExecutions(_hosts);
	
				closeFTPConnections(_hosts);
				closeSSHConnections(_hosts);
	
			} catch (IOException e) {
				Log.error("Deployment shutdown unsuccessful; see logs for more information");
				Log.info("Shutting system down abruptly");
	
				logger.error(e);
			} finally {
				Log.heading("Deployment shutdown successful");
				System.exit(0);
			}
		}
	}

	/**
	 * @return registered hosts
	 */
	public static Map<String, Host> hosts() {
		return hosts;
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
	 * TODO: When only some of the properties are set, the rest will be null.
	 * Instead of setting the default values when the file is not found, set
	 * variables for each variable when it is null.
	 * 
	 * Reads configuration properties
	 */
	private static void readConfiguration() {
		Properties config = new Properties();
		InputStream input = null;
		boolean ok = false;

		try {
			input = Amelia.class.getClassLoader().getResourceAsStream(
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
				config.put("connection_timeout", "10000"); // 10s. 0 for no timeout
				config.put("execution_timeout", "10000"); // 10s
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
