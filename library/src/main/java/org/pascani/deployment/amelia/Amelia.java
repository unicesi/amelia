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
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pascani.deployment.amelia.descriptors.CommandDescriptor;
import org.pascani.deployment.amelia.descriptors.Host;
import org.pascani.deployment.amelia.util.Log;

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public class Amelia {

	private static int hostFixedWidth;

	/**
	 * A map to store initial configuration parameters
	 */
	private static Map<String, String> configuration;

	/**
	 * The current execution graph
	 */
	private static DependencyGraph<? extends CommandDescriptor> currentExecutionGraph;

	/**
	 * The variable indicating whether the current deployment is in trance of
	 * aborting
	 */
	public static volatile boolean aborting;

	/**
	 * The variable indicating whether the current deployment is being shutting
	 * down
	 */
	public static volatile boolean shuttingDown;

	/**
	 * An uncaught exception handler
	 */
	public static Thread.UncaughtExceptionHandler exceptionHandler;

	/**
	 * The logger
	 */
	private final static Logger logger = LogManager.getLogger(Amelia.class);


	static {
		reset();
	}
	
	/**
	 * Initializes all the non-final class members
	 */
	public static void reset() {
		hostFixedWidth = 0;
		aborting = false;
		shuttingDown = false;
		exceptionHandler = new Thread.UncaughtExceptionHandler() {
			public void uncaughtException(Thread t, Throwable e) {
				if (!aborting && !shuttingDown) {
					aborting = true;
					String message = e.getMessage().replaceAll(
							"^((\\w)+(\\.\\w+)+:\\s)*", "");

					logger.error(e.getMessage(), e);
					Log.error("Stopping deployment: " + message);

					Amelia.shutdown(true);
				}
			}
		};
	}

	/**
	 * Opens FTP connections with the specified hosts
	 * 
	 * @param hosts
	 *            The array of hosts containing the FTP handler
	 * @throws InterruptedException
	 *             If any thread interrupts any of the handler threads
	 */
	public static void openFTPConnections(Host... hosts)
			throws InterruptedException {

		if (hosts.length > 0)
			Log.heading("Establishing FTP connections (" + hosts.length + ")");

		for (Host host : hosts) {
			if (host.ftp().client().isConnected())
				continue;

			host.openFTPConnection();

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
	public static void openSSHConnections(Host... hosts)
			throws InterruptedException {

		if (hosts.length > 0)
			Log.heading("Establishing SSH connections (" + hosts.length + ")");

		for (Host host : hosts) {
			if (host.ssh().isConnected())
				continue;

			host.openSSHConnection();

			if (hostFixedWidth < host.toString().length())
				hostFixedWidth = host.toString().length();

			if (host.ssh().isConnected())
				logger.info("SSH connection for " + host
						+ " was successfully established");
		}

		// set a common (fixed) with for all hosts
		for (Host host : currentExecutionGraph.hosts()) {
			host.setFixedWidth(hostFixedWidth);
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
		if (hosts.length > 0)
			Log.subheading("Closing FTP connections");

		for (Host host : hosts) {
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
		if (hosts.length > 0)
			Log.subheading("Closing SSH connections");

		for (Host host : hosts) {
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
		for (Host host : hosts)
			host.stopExecutions();
	}

	/**
	 * Terminates the execution
	 */
	public static void shutdown(boolean stopCurrentExecutions) {
		// Prevent shutting down more than once
		if (!shuttingDown) {
			shuttingDown = true;
			Log.heading("Starting deployment shutdown");

			try {
				Host[] _hosts = currentExecutionGraph.hosts().toArray(
						new Host[0]);

				if(stopCurrentExecutions)
					currentExecutionGraph.stopExecutions();
				
				currentExecutionGraph.stopCurrentThreads();
				closeFTPConnections(_hosts);
				closeSSHConnections(_hosts);
			} catch (Exception e) {
				Log.error("Deployment shutdown unsuccessful; see logs for more information");
				Log.error("Shutting system down abruptly");
				logger.error(e);
			} finally {
				Log.heading("Deployment shutdown successful");
				// FIXME: Stop all threads instead of this
				// System.exit(0);
			}
		}
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
	 * Instead of setting the default values when the file is not found, set
	 * variables for each variable when it is null.
	 * 
	 * Reads configuration properties
	 */
	private static void readConfiguration() {
		Properties config = new Properties();
		InputStream input = null;

		try {
			input = Amelia.class.getClassLoader().getResourceAsStream(
					"amelia.properties");
			if (input != null)
				config.load(input);

		} catch (FileNotFoundException e) {
			logger.warn("No configuration file was found. Execution is started with default values");
		} catch (IOException e) {
			logger.error("Error loading configuration file. Execution is started with default values");
		} finally {
			String home = System.getProperty("user.home");

			// Set defaults
			if (!config.containsKey("identity"))
				config.put("identity", home + "/.ssh/id_rsa");
			if (!config.containsKey("known_hosts"))
				config.put("known_hosts", home + "/.ssh/known_hosts");
			if (!config.containsKey("connection_timeout"))
				config.put("connection_timeout", "10000"); // 10s. 0 for no
															// timeout
			if (!config.containsKey("execution_timeout"))
				config.put("execution_timeout", "15000"); // 15s

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

	public static void setCurrentExecutionGraph(
			DependencyGraph<? extends CommandDescriptor> graph) {
		currentExecutionGraph = graph;
	}

}
