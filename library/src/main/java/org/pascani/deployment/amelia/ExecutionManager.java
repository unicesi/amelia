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

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pascani.deployment.amelia.descriptors.Host;
import org.pascani.deployment.amelia.util.Log;

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public class ExecutionManager {

	private int hostFixedWidth;

	/**
	 * The current execution graph
	 */
	private DescriptorGraph executionGraph;

	/**
	 * The variable indicating whether the current deployment is in trance of
	 * abortion
	 */
	private volatile boolean aborting;

	/**
	 * The variable indicating whether the current deployment is being shutting
	 * down
	 */
	private volatile boolean shuttingDown;

	/**
	 * The uncaught exception handler for all threads
	 */
	private static Thread.UncaughtExceptionHandler exceptionHandler;

	/**
	 * The logger
	 */
	private static Logger logger = LogManager.getLogger(ExecutionManager.class);

	public ExecutionManager(DescriptorGraph executionGraph) {
		this.executionGraph = executionGraph;
		reset();
	}

	/**
	 * Initializes all the class members
	 */
	public void reset() {
		this.hostFixedWidth = 0;
		this.aborting = false;
		this.shuttingDown = false;
		exceptionHandler = new Thread.UncaughtExceptionHandler() {
			public void uncaughtException(Thread t, Throwable e) {
				if (!aborting && !shuttingDown) {
					aborting = true;
					String message = e.getMessage().replaceAll(
							"^((\\w)+(\\.\\w+)+:\\s)*", "");

					logger.error(e.getMessage(), e);
					Log.error("Stopping deployment: " + message);

					SubsystemGraph.getInstance().shutdown();
					shutdown(true);
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
	public void openFTPConnections(Host... hosts) throws InterruptedException {

		if (hosts.length > 0)
			Log.info("Establishing FTP connections (" + hosts.length + ")");

		for (Host host : hosts) {
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
	public void openSSHConnections(Host... hosts) throws InterruptedException {

		if (hosts.length > 0)
			Log.info("Establishing SSH connections (" + hosts.length + ")");

		for (Host host : hosts) {
			host.openSSHConnection();

			if (hostFixedWidth < host.toString().length())
				hostFixedWidth = host.toString().length();

			logger.info("SSH connection for " + host
					+ " was successfully established");
		}

		// set a common (fixed) width for all hosts
		for (Host host : executionGraph.hosts()) {
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
	public void closeFTPConnections(Host... hosts) throws IOException {
		if (hosts.length > 0)
			Log.info("Closing FTP connections");

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
	public void closeSSHConnections(Host... hosts) throws IOException {
		if (hosts.length > 0)
			Log.info("Closing SSH connections");

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
	public void stopExecutions(Host... hosts) throws IOException {
		for (Host host : hosts)
			host.stopExecutions();
	}

	/**
	 * Terminates the execution
	 */
	public void shutdown(boolean stopCurrentExecutions) {
		// Prevent shutting down more than once
		if (!shuttingDown) {
			shuttingDown = true;
			Log.info("Starting deployment shutdown");

			try {
				Host[] sshHosts = executionGraph.sshHosts()
						.toArray(new Host[0]);
				Host[] ftpHosts = executionGraph.ftpHosts()
						.toArray(new Host[0]);

				if (stopCurrentExecutions)
					executionGraph.stopExecutions();

				executionGraph.stopCurrentThreads();
				closeFTPConnections(ftpHosts);
				closeSSHConnections(sshHosts);
				Log.info("Deployment shutdown successful");
			} catch (Exception e) {
				Log.error("Deployment shutdown unsuccessful; see logs for more information");
				Log.error("Shutting system down abruptly");
				logger.error(e);
			} finally {
				reset();
			}
		}
	}

	public boolean isAborting() {
		return aborting;
	}

	public boolean isShuttingDown() {
		return shuttingDown;
	}

	public static Thread.UncaughtExceptionHandler exceptionHandler() {
		return exceptionHandler;
	}

}
