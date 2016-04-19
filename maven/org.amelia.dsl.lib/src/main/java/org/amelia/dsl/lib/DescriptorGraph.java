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
package org.amelia.dsl.lib;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.amelia.dsl.lib.util.ScheduledTask;
import org.amelia.dsl.lib.util.Strings;

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
					if (Boolean.valueOf(System.getProperty("amelia.debug_mode"))
							&& this.descriptor.isExecution()) {
						Log.debug(this.handler.host(), "Composite '"
								+ getCompositeName(this.descriptor.toCommandString())
								+ "' has been sent to the execution queue");
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

	private final ExecutionManager executionManager;
	
	private final Configuration configuration;

	public DescriptorGraph(String subsystem) {
		this.configuration = new Configuration();
		this.configuration.setProperties();
		this.subsystem = subsystem;
		this.tasks = new HashMap<CommandDescriptor, List<ScheduledTask<?>>>();
		this.sshHosts = new HashSet<Host>();
		this.ftpHosts = new HashSet<Host>();
		this.threads = new TreeSet<DependencyThread>();
		this.executionManager = new ExecutionManager(this);
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
	 * @deprecated use {@link CommandDescriptor#runsOn(Host)} instead
	 * @param a
	 *            The command descriptor
	 * @param hosts
	 *            The hosts where the command runs
	 * @return Whether or not the command was added
	 */
	@Deprecated
	public boolean addDescriptor(CommandDescriptor a, Host... hosts) {
		if (containsKey(a))
			return false;

		// The only known use of the FTP connection is the AssetBundle
		if (a instanceof AssetBundle)
			this.ftpHosts.addAll(Arrays.asList(hosts));
		else
			this.sshHosts.addAll(Arrays.asList(hosts));

		// Add the element with an empty list of dependencies
		put(a, new ArrayList<CommandDescriptor>());
		this.tasks.put(a, new ArrayList<ScheduledTask<?>>());

		// Add an executable task per host
		for (Host host : hosts) {
			ScheduledTask<?> task = new ScheduledTask<Object>(host, a);
			this.tasks.get(a).add(task);
		}

		return true;
	}

	/**
	 * @deprecated Use {@link CommandDescriptor#dependsOn(CommandDescriptor)}
	 *             instead
	 * @param a
	 *            The dependent command
	 * @param b
	 *            The dependency
	 * @return Whether or not the dependency was added
	 */
	@Deprecated
	public boolean addDependency(CommandDescriptor a, CommandDescriptor b) {
		if (!containsKey(a) || !containsKey(b))
			return false;
		else if(a == b)
			return false;

		// FIXME: search for transitive dependencies
		if (get(b).contains(a))
			throw new RuntimeException(String
					.format("Circular reference detected: %s <-> %s", a, b));

		get(a).add(b);
		return true;
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
		// Open SSH and FTP connections before dependencies resolution
		final List<Boolean> connectionOk = new ArrayList<Boolean>();
		Thread setupThread = new Thread() {
			public void run() {
				// Handle setup errors
				setDefaultUncaughtExceptionHandler(ExecutionManager.exceptionHandler());
				try {
					validate();
					executionManager.openSSHConnections(sshHosts.toArray(new Host[0]));
					executionManager.openFTPConnections(ftpHosts.toArray(new Host[0]));
					connectionOk.add(true);
				} catch (Exception e) {
					connectionOk.add(false);
					throw new RuntimeException(e.getMessage(), e.getCause());
				}
			}
		};
		setupThread.start();
		setupThread.join();
		if (!connectionOk.get(0))
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

				// Handle uncaught exceptions
				thread.setUncaughtExceptionHandler(
						ExecutionManager.exceptionHandler());
				threads.add(thread);
			}
		}

		Log.info("Executing commands (" + totalTasks + ")");
		for (DependencyThread thread : this.threads)
			thread.start();

		doneSignal.await();
		
		if(shutdownAfterDeployment)
			this.executionManager.shutdown(stopExecutionsWhenFinish);
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
	
	public void stopExecutions(final String[] compositeNames) throws IOException {
		Map<Host, List<CommandDescriptor>> executionsPerHost = 
				new HashMap<Host, List<CommandDescriptor>>();
		for (CommandDescriptor descriptor : this.tasks.keySet()) {
			if (descriptor.isExecution()) {
				// FIXME: Can composite names contain other characters? (group 3)
				Pattern pattern = Pattern.compile(
						"(frascati run) (\\-r [0-9]+ )?([\\w\\-]+)(.*)");
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
	
	public ExecutionManager executionManager() {
		return this.executionManager;
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
	
	public Set<Host> sshHosts() {
		return this.sshHosts;
	}
	
	public Set<Host> ftpHosts() {
		return this.ftpHosts;
	}
}
