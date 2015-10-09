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

import static org.pascani.deployment.amelia.util.Strings.ascii;

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
import org.pascani.deployment.amelia.process.FTPHandler;
import org.pascani.deployment.amelia.process.SSHHandler;

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

	/**
	 * A map to store initial configuration parameters
	 */
	private static Map<String, String> configuration;

	/**
	 * A map to store SSH handlers per host (host id)
	 */
	private static Map<String, SSHHandler> sshConnections = new Hashtable<String, SSHHandler>();

	/**
	 * A map to store FTP handlers per host (host id)
	 */
	private static Map<String, FTPHandler> ftpConnections = new Hashtable<String, FTPHandler>();

	/**
	 * An uncaught exception handler
	 */
	public static final Thread.UncaughtExceptionHandler exceptionHandler = new Thread.UncaughtExceptionHandler() {
		public void uncaughtException(Thread t, Throwable e) {

			String message = e
					.getMessage()
					.replace("java.util.concurrent.ExecutionException: ", "")
					.replace("java.lang.RuntimeException: ", "")
					.replace("org.pascani.deployment.amelia.DeploymentException: ", "");

			logger.error(message, e);

			System.out.println("Stopping deployment. Cause: " + message);
			Amelia.shutdown();
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
	 *            The array of hosts containing the FTP connection data
	 * @throws InterruptedException
	 *             If any thread interrupts any of the handler threads
	 */
	public static void openFTPConnections(Host... hosts)
			throws InterruptedException {

		for (Host host : hosts) {
			FTPHandler ftpHandler = new FTPHandler(host);
			ftpConnections.put(host.identifier(), ftpHandler);
			ftpHandler.run();
			ftpHandler.join();

			logger.info("FTP connection for " + host
					+ " was successfully established");
		}
	}

	/**
	 * Opens SSH connections with the specified hosts
	 * 
	 * @param hosts
	 *            The array of hosts containing the SSH connection data
	 * @throws InterruptedException
	 *             If any thread interrupts any of the handler threads
	 */
	public static void openSSHConnections(Host... hosts)
			throws InterruptedException {

		for (Host host : hosts) {
			SSHHandler sshHandler = new SSHHandler(host);
			sshConnections.put(host.identifier(), sshHandler);
			sshHandler.run();
			sshHandler.join();

			logger.info("SSH connection for " + host
					+ " was successfully established");
		}
	}

	/**
	 * Closes the FTP connection with the specified hosts, and removes it from
	 * the FTP connections.
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
			FTPHandler handler = ftpConnections.get(id);
			handler.close();
			ftpConnections.remove(id);

			logger.info("FTP connection for " + handler.host()
					+ " was successfully closed");
		}
	}

	/**
	 * Closes the SSH connection with the specified hosts, and removes it from
	 * the SSH connections. Before closing the connections, all FraSCAti
	 * executions are killed.
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
			SSHHandler handler = sshConnections.get(id);
			handler.stopExecutions();
			handler.close();

			sshConnections.remove(handler.host().identifier());
			logger.info("SSH connection for " + handler.host()
					+ " was successfully closed");
		}
	}

	/**
	 * Terminates the execution
	 */
	public static void shutdown() {
		String message = "Shutting down deployment";
		try {
			String[] sshHosts = sshConnections.keySet().toArray(new String[0]);
			String[] ftpHosts = ftpConnections.keySet().toArray(new String[0]);

			closeSSHConnections(sshHosts);
			closeFTPConnections(ftpHosts);
		} catch (IOException e) {
			String str = "Deployment shutdown unsuccessful. Shutting system down abruptly";
			message = str;
			logger.error(str);
		} finally {
			System.out.println(message);
			System.exit(0);
		}
	}

	/**
	 * @return the handlers of the FTP connections
	 */
	public static Map<String, FTPHandler> ftpConnections() {
		return ftpConnections;
	}

	/**
	 * @return the handlers of the SSH connections
	 */
	public static Map<String, SSHHandler> sshConnections() {
		return sshConnections;
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
				config.put("connection_timeout", "0"); // No timeout
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
