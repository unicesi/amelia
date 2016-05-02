/*
 * Copyright © 2015 Universidad Icesi
 * 
 * This file is part of the Amelia project.
 * 
 * The Amelia project is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * The Amelia project is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the Amelia project. If not, see <http://www.gnu.org/licenses/>.
 */
package org.amelia.dsl.lib;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.amelia.dsl.lib.descriptors.AssetBundle;
import org.amelia.dsl.lib.descriptors.CommandDescriptor;
import org.amelia.dsl.lib.descriptors.Host;
import org.amelia.dsl.lib.util.Configuration;
import org.amelia.dsl.lib.util.Log;
import org.amelia.dsl.lib.util.Strings;
import org.amelia.dsl.lib.util.Threads;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jcraft.jsch.JSchException;

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public class DescriptorGraph
		extends HashMap<CommandDescriptor, List<CommandDescriptor>> {

	public class DependencyThread extends Thread
			implements Observer, Comparable<DependencyThread> {
		
		private final UUID internalId;
		private final CommandDescriptor descriptor;
		private final SSHHandler handler;
		private final ScheduledTask<?> command;
		private final List<CommandDescriptor> dependencies;
		private final CountDownLatch doneSignal;
		private final CountDownLatch mainDoneSignal;
		private volatile boolean shutdown;

		/**
		 * "dependencies" contains the descriptors on which this thread depends,
		 * while "actualDependencies" corresponds to the number of tasks
		 * executing those descriptors.
		 */
		public DependencyThread(final CommandDescriptor descriptor,
				final SSHHandler handler, final ScheduledTask<?> command,
				final List<CommandDescriptor> dependencies,
				final int actualDependencies, final CountDownLatch doneSignal) {
			this.internalId = UUID.randomUUID();
			this.descriptor = descriptor;
			this.handler = handler;
			this.command = command;
			this.dependencies = dependencies;
			this.doneSignal = new CountDownLatch(actualDependencies);
			this.mainDoneSignal = doneSignal;
			this.shutdown = false;

			// Make this thread observe the corresponding dependencies
			for (CommandDescriptor dependency : this.dependencies) {
				dependency.addObserver(this);
			}
		}

		public void run() {
			try {
				this.doneSignal.await();
				if (!this.shutdown) {
					if (Boolean.valueOf(System.getProperty("amelia.debug_mode"))) {
						if (this.descriptor.isExecution()) {
							Log.debug(this.handler.host(), "Composite awaiting execution: "
									+ getCompositeName(this.descriptor.toCommandString()));
						} else {
							Log.debug(this.handler.host(), "Command awaiting execution: "
									+ this.descriptor.toCommandString());
						}
					}
					this.handler.executeCommand(this.descriptor, this.command);
					this.descriptor.done(this.handler.host());
					// Release this dependency
					if (this.descriptor.isExecution()) {
						// FIXME: Temporary workaround to avoid service-not-bound
						// errors (RMI)
						Thread.sleep(2000);
					}
					this.descriptor.notifyObservers();
				}
			} catch (Exception e) {
				throw new RuntimeException(e.getMessage(), e.getCause());
			} finally {
				// Notify to main thread
				this.mainDoneSignal.countDown();
			}
		}
		
		private String getCompositeName(final String runCommand) {
			String command = runCommand;
			Pattern pattern = Pattern.compile("(frascati run) (\\-r [0-9]+ )?(.*)");
			Matcher matcher = pattern.matcher(command);
			if (matcher.find()) {
				command = matcher.group(3);
			}
			return command.split(" ")[0];
		}

		public synchronized void update(Observable o, Object arg) {
			this.doneSignal.countDown();
		}
		
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			else if ((null == obj) || (obj.getClass() != this.getClass()))
				return false;
			return this.compareTo((DependencyThread) obj) == 0;
		}

		public int compareTo(DependencyThread o) {
			return this.internalId.compareTo(o.internalId);
		}

		public void shutdown() {
			this.shutdown = true;
			this.descriptor.deleteObserver(this);
			this.handler.shutdownTaskQueue();
			while (this.doneSignal.getCount() > 0)
				this.doneSignal.countDown();
		}
	}

	/**
	 * Serial version UID
	 */
	private static final long serialVersionUID = -6806533450294013309L;
	
	private final String subsystem;

	private final Map<CommandDescriptor, List<ScheduledTask<?>>> tasks;

	private final Set<Host> sshHosts;

	private final Set<Host> ftpHosts;

	private final TreeSet<DependencyThread> threads;
	
	private final Configuration configuration;

	/**
	 * The variable indicating whether the current deployment is being shutting
	 * down
	 */
	private volatile boolean shuttingDown;
	
	/**
	 * The logger
	 */
	private static Logger logger = LogManager.getLogger(DescriptorGraph.class);

	public DescriptorGraph(String subsystem) {
		this.configuration = new Configuration();
		this.configuration.setProperties();
		this.subsystem = subsystem;
		this.tasks = new HashMap<CommandDescriptor, List<ScheduledTask<?>>>();
		this.sshHosts = new HashSet<Host>();
		this.ftpHosts = new HashSet<Host>();
		this.threads = new TreeSet<DependencyThread>();
		this.shuttingDown = false;
	}
	
	public DescriptorGraph() {
		this("default");
	}
	
	/**
	 * FIXME: search for transitive dependencies
	 *
	 * @param descriptors The array of descriptors to add
	 * @return {@code true} is all descriptors were added, {@code false} otherwise
	 */
	public boolean addDescriptors(CommandDescriptor... descriptors) {
		boolean added = true;
		for (CommandDescriptor descriptor : descriptors) {
			if (containsKey(descriptor)) {
				added = false;
				continue;
			}
			// The only known use of the FTP connection is the AssetBundle
			if (descriptor instanceof AssetBundle)
				this.ftpHosts.addAll(descriptor.hosts());
			else
				this.sshHosts.addAll(descriptor.hosts());
			
			put(descriptor, descriptor.dependencies());
			this.tasks.put(descriptor, new ArrayList<ScheduledTask<?>>());

			// Add an executable task per host
			for (Host host : descriptor.hosts()) {
				ScheduledTask<?> task = new ScheduledTask<Object>(host, descriptor);
				this.tasks.get(descriptor).add(task);
			}
		}
		return added;
	}
	
	/**
	 * Checks that all of the command dependencies are satisfiable
	 */
	public void validate() throws Exception {
		for (CommandDescriptor descriptor : keySet()) {
			for (CommandDescriptor dependency : descriptor.dependencies()) {
				if (!keySet().contains(dependency)) {
					throw new Exception("Unmeetable command dependency found. "
							+ "The graph must contain all of the command "
							+ "descriptors.");
				}
			}
		}
	}

	/**
	 * Resolve dependencies and do nothing after deployment has been finished
	 */
	public void execute(final boolean stopPreviousExecutions)
			throws InterruptedException, IOException {
		execute(stopPreviousExecutions, false, false);
	}

	public void execute(final boolean stopPreviousExecutions,
			final boolean shutdownAfterDeployment,
			final boolean stopExecutionsWhenFinish)
					throws InterruptedException, IOException {
		if (!establishConnections())
			return;
		if (stopPreviousExecutions && this.sshHosts.size() > 0)
			stopAllExecutions();
		
		int totalTasks = countTotalTasks();
		CountDownLatch doneSignal = new CountDownLatch(totalTasks);
		
		for (CommandDescriptor e : keySet()) {
			List<CommandDescriptor> dependencies = get(e);
			List<ScheduledTask<?>> tasks = this.tasks.get(e);
			int deps = countDependencyThreads(dependencies);
			for (ScheduledTask<?> task : tasks) {
				DependencyThread thread = new DependencyThread(e,
						task.host().ssh(), task, dependencies, deps, doneSignal);
				thread.setUncaughtExceptionHandler(
						Threads.exceptionHandler());
				threads.add(thread);
			}
		}
		Log.info("Executing commands (" + totalTasks + ")");
		for (DependencyThread thread : this.threads) {
			thread.start();
		}
		doneSignal.await();
		if(shutdownAfterDeployment)
			shutdown(stopExecutionsWhenFinish);
	}
		
	private boolean establishConnections() throws InterruptedException {
		final List<Boolean> connectionOk = new ArrayList<Boolean>();
		Thread setupThread = new Thread() {
			public void run() {
				// Handle setup errors
				setDefaultUncaughtExceptionHandler(Threads.exceptionHandler());
				try {
					validate();
					establishFixedWidth();
					openSSHConnections();
					openFTPConnections();
					connectionOk.add(true);
				} catch (Exception e) {
					connectionOk.add(false);
					throw new RuntimeException(e.getMessage(), e.getCause());
				}
			}
		};
		setupThread.start();
		setupThread.join();
		return connectionOk.get(0);
	}
	
	private int countTotalTasks() {
		int total = 0;
		for(List<ScheduledTask<?>> tasks : this.tasks.values()) {
			total += tasks.size();
		}
		return total;
	}
	
	private int countDependencyThreads(List<CommandDescriptor> dependencies) {
		int n = 0;
		for (CommandDescriptor e : dependencies)
			n += this.tasks.get(e).size();
		return n;
	}

	/**
	 * Closes the FTP connection with the corresponding hosts.
	 * 
	 * @throws IOException
	 *             If an I/O error occurs while either sending a command to the
	 *             server or receiving a reply from the server.
	 */
	private void closeFTPConnections() throws IOException {
		if (!this.ftpHosts.isEmpty())
			Log.info("Closing FTP connections");
		for (Host host : this.ftpHosts) {
			boolean connected = host.ftp() != null && host.ftp().isConnected();
			if (host.closeFTPConnection() && connected) {
				logger.info("FTP connection for " + host
						+ " was successfully closed");
			}
		}
	}

	/**
	 * Closes the SSH connection with the corresponding hosts.
	 * 
	 * @throws IOException
	 *             If I/O error occurs.
	 */
	private void closeSSHConnections() throws IOException {
		if (!this.sshHosts.isEmpty())
			Log.info("Closing SSH connections");
		for (Host host : this.sshHosts) {
			boolean connected = host.ssh() != null && host.ssh().isConnected();
			if (host.closeSSHConnection() && connected) {
				logger.info("SSH connection for " + host
						+ " was successfully closed");
			}
		}
	}
	
	/**
	 * (For pretty printing purposes) Finds the width of the longest host name
	 * and update it in each host.
	 */
	private void establishFixedWidth() {
		int hostFixedWidth = 0;
		Set<Host> hosts = hosts();
		for (Host host : hosts) {
			if (hostFixedWidth < host.toString().length())
				hostFixedWidth = host.toString().length();
		}
		// set a common (fixed) width for all hosts
		for (Host host : hosts) {
			host.setFixedWidth(hostFixedWidth);
		}
	}
	
	/**
	 * Opens FTP connections with the corresponding hosts
	 * 
	 * @throws InterruptedException
	 *             If any thread interrupts any of the handler threads
	 * @throws IOException
	 *             If there is a connection error
	 * @throws SocketException
	 *             If there is a connection error
	 */
	private void openFTPConnections()
			throws InterruptedException, SocketException, IOException {
		if (!this.ftpHosts.isEmpty())
			Log.info("Establishing FTP connections (" + this.ftpHosts.size() + ")");
		for (Host host : this.ftpHosts) {
			boolean connected = host.ftp() != null && host.ftp().isConnected();
			if (host.openFTPConnection() && !connected) {
				logger.info("FTP connection for " + host
						+ " was successfully established");
			}
		}
	}

	/**
	 * Opens SSH connections with the corresponding hosts
	 * 
	 * @throws InterruptedException
	 *             If any thread interrupts any of the handler threads
	 * @throws IOException
	 *             If there is an error while initiating the SSH connection
	 * @throws JSchException
	 *             If there is an error establishing the SSH connection
	 */
	private void openSSHConnections()
			throws InterruptedException, JSchException, IOException {
		if (!this.sshHosts.isEmpty())
			Log.info("Establishing SSH connections (" + this.sshHosts.size() + ")");
		for (Host host : this.sshHosts) {
			boolean connected = host.ssh() != null && host.ssh().isConnected();
			if (host.openSSHConnection(this.subsystem) && !connected) {
				logger.info("SSH connection for " + host
						+ " was successfully established");
			}
		}
	}
	
	/**
	 * Stop the given executed components and then terminates the execution
	 */
	public void shutdown(String... compositeNames) {
		shutdown(false, compositeNames);
	}

	/**
	 * (if requested) Stop all of the executed components, and then terminates
	 * the execution
	 */
	public void shutdown(boolean stopAllCurrentExecutions) {
		shutdown(stopAllCurrentExecutions, new String[0]);
	}
	
	private void shutdown(boolean stopAllExecutedComponents,
			String[] compositeNames) {
		// Prevent shutting down more than once
		if (!shuttingDown) {
			shuttingDown = true;
			Log.info("Shutting down deployment (" + this.subsystem + ")");
			try {
				if (stopAllExecutedComponents)
					stopAllExecutions();
				else
					stopExecutions(compositeNames);
				stopCurrentThreads();
				closeFTPConnections();
				closeSSHConnections();
			} catch (Exception e) {
				Log.error("Deployment shutdown unsuccessful. See logs for more "
						+ "information");
				Log.error("Shutting system down abruptly");
				logger.error(e);
			}
		}
	}
	
	public void stopExecutions(final String[] compositeNames) throws IOException {
		Map<Host, List<CommandDescriptor>> executionsPerHost = 
				new HashMap<Host, List<CommandDescriptor>>();
		for (CommandDescriptor descriptor : this.tasks.keySet()) {
			if (descriptor.isExecution()) {
				// FIXME: Can composite names contain other characters? (group 3)
				Pattern pattern = Pattern.compile(
						"(frascati run) (\\-r [0-9]+ )?([\\w\\-\\.]+)(.*)");
				Matcher matcher = pattern.matcher(descriptor.toCommandString());
				if (matcher.find()) {
					if (Strings.containsAnyOf(matcher.group(3), compositeNames)) {
						for (ScheduledTask<?> command : this.tasks.get(descriptor)) {
							if (!executionsPerHost.containsKey(command.host()))
								executionsPerHost.put(command.host(),
										new ArrayList<CommandDescriptor>());
							executionsPerHost.get(command.host()).add(descriptor);
						}
					}
				}
			}
		}
		for (Host host : executionsPerHost.keySet()) {
			host.stopExecutions(executionsPerHost.get(host));
		}
	}
	
	public void stopAllExecutions() throws IOException {
		Map<Host, List<CommandDescriptor>> executionsPerHost = 
				new HashMap<Host, List<CommandDescriptor>>();
		for (CommandDescriptor descriptor : this.tasks.keySet()) {
			if (descriptor.isExecution()) {
				List<ScheduledTask<?>> commands = this.tasks.get(descriptor);
				for (ScheduledTask<?> command : commands) {
					if (!executionsPerHost.containsKey(command.host()))
						executionsPerHost.put(command.host(),
								new ArrayList<CommandDescriptor>());
					executionsPerHost.get(command.host()).add(descriptor);
				}
			}
		}
		for (Host host : executionsPerHost.keySet()) {
			host.stopExecutions(executionsPerHost.get(host));
		}
	}

	public void stopCurrentThreads() throws InterruptedException {
		for (DependencyThread thread : this.threads)
			thread.shutdown();
		for (DependencyThread thread : this.threads)
			thread.join();
	}
	
	public boolean isShuttingDown() {
		return shuttingDown;
	}
	
	public String subsystem() {
		return this.subsystem;
	}

	public Set<Host> hosts() {
		Set<Host> hosts = new TreeSet<Host>();
		hosts.addAll(this.ftpHosts);
		hosts.addAll(this.sshHosts);
		return hosts;
	}

}
