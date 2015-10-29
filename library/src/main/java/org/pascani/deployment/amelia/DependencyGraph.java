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
package org.pascani.deployment.amelia;

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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import org.pascani.deployment.amelia.commands.Command;
import org.pascani.deployment.amelia.commands.CommandFactory;
import org.pascani.deployment.amelia.descriptors.AssetBundle;
import org.pascani.deployment.amelia.descriptors.CommandDescriptor;
import org.pascani.deployment.amelia.descriptors.Execution;
import org.pascani.deployment.amelia.descriptors.Host;
import org.pascani.deployment.amelia.util.Log;

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public class DependencyGraph extends
		HashMap<CommandDescriptor, List<CommandDescriptor>> {

	public class DependencyThread extends Thread implements Observer,
			Comparable<DependencyThread> {

		private final CommandDescriptor descriptor;
		private final SSHHandler handler;
		private final Callable<?> callable;
		private final List<CommandDescriptor> dependencies;
		private final CountDownLatch doneSignal;
		private final CountDownLatch mainDoneSignal;
		private final boolean isRollback;
		private volatile boolean shutdown;

		/**
		 * "dependencies" contains the descriptors on which this thread depends,
		 * while "actualDependencies" corresponds to the number of tasks
		 * executing those descriptors.
		 */
		public DependencyThread(final CommandDescriptor descriptor,
				final SSHHandler handler, final Callable<?> callable,
				final List<CommandDescriptor> dependencies,
				final int actualDependencies, final CountDownLatch doneSignal,
				boolean isRollback) {
			this.descriptor = descriptor;
			this.handler = handler;
			this.callable = callable;
			this.dependencies = dependencies;
			this.doneSignal = new CountDownLatch(actualDependencies);
			this.mainDoneSignal = doneSignal;
			this.isRollback = isRollback;
			this.shutdown = false;

			// Make this thread observe the corresponding dependencies
			for (CommandDescriptor dependency : this.dependencies) {
				dependency.addObserver(this);
			}
		}

		public DependencyThread(final CommandDescriptor descriptor,
				final SSHHandler handler, final Callable<?> callable,
				final List<CommandDescriptor> dependencies,
				final int actualDependencies, final CountDownLatch doneSignal) {
			this(descriptor, handler, callable, dependencies,
					actualDependencies, doneSignal, false);
		}

		public void run() {
			try {
				this.doneSignal.await();

				if (!this.shutdown) {
					this.handler.executeCommand(this.callable);
					this.descriptor.done(this.handler.host());

					if (!this.isRollback)
						Log.info(this.handler.host(),
								this.descriptor.doneMessage());

					// Release this dependency
					this.descriptor.notifyObservers();
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			} finally {
				// Notify to main thread
				this.mainDoneSignal.countDown();
			}
		}

		public void update(Observable o, Object arg) {
			this.doneSignal.countDown();
		}

		public int compareTo(DependencyThread o) {
			if (this.dependencies.size() < o.dependencies.size())
				return -1;
			else
				return 1;
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

	private final Map<CommandDescriptor, List<Command<?>>> tasks;

	private final Set<Host> sshHosts;

	private final Set<Host> ftpHosts;

	private final TreeSet<DependencyThread> threads;

	public DependencyGraph() {
		this.tasks = new HashMap<CommandDescriptor, List<Command<?>>>();
		this.sshHosts = new HashSet<Host>();
		this.ftpHosts = new HashSet<Host>();
		this.threads = new TreeSet<DependencyThread>();
	}

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
		this.tasks.put(a, new ArrayList<Command<?>>());

		// Add a executable task per host
		for (Host host : hosts) {
			Command<?> task = CommandFactory.getInstance().getCommand(host, a);
			this.tasks.get(a).add(task);
		}

		return true;
	}

	public boolean addDependency(CommandDescriptor a, CommandDescriptor b) {
		if (!containsKey(a) || !containsKey(b))
			return false;

		// FIXME: search for transitive dependencies
		if (get(b).contains(a))
			throw new RuntimeException(String.format(
					"Circular reference detected: %s <-> %s", a, b));

		get(a).add(b);
		return true;
	}

	public void resolve(boolean stopPreviousExecutions)
			throws InterruptedException, IOException {

		Amelia.setCurrentExecutionGraph(this);
		Log.printBanner();

		// Open SSH and FTP connections before dependencies resolution
		// TODO: validate if FTP/SSH is needed before opening connections
		Amelia.openSSHConnections(this.sshHosts.toArray(new Host[0]));
		Amelia.openFTPConnections(this.ftpHosts.toArray(new Host[0]));

		if (stopPreviousExecutions)
			stopExecutions();

		CountDownLatch doneSignal = new CountDownLatch(this.tasks.size());

		for (CommandDescriptor e : keySet()) {
			List<CommandDescriptor> dependencies = get(e);
			List<Command<?>> tasks = this.tasks.get(e);
			int deps = countDependencyThreads(dependencies, tasks);

			for (Command<?> task : tasks) {
				DependencyThread thread = new DependencyThread(e, task.host()
						.ssh(), task, dependencies, deps, doneSignal);

				// Handle uncaught exceptions
				thread.setUncaughtExceptionHandler(Amelia.exceptionHandler);
				threads.add(thread);
			}
		}

		Log.heading("Starting deployment (" + this.threads.size() + ")");
		for (DependencyThread thread : this.threads)
			thread.start();

		doneSignal.await();
	}

	private int countDependencyThreads(List<CommandDescriptor> dependencies,
			List<Command<?>> tasks) {
		int n = dependencies.size();

		for (CommandDescriptor e : dependencies)
			for (Command<?> c : tasks)
				if (c.descriptor().equals(e))
					++n;

		return n;
	}

	public void stopExecutions() throws IOException {

		Log.heading("Stopping previous executions");
		Map<Host, List<Execution>> executionsPerHost = new HashMap<Host, List<Execution>>();

		for (CommandDescriptor descriptor : this.tasks.keySet()) {
			if (descriptor instanceof Execution) {
				List<Command<?>> commands = this.tasks.get(descriptor);
				for (Command<?> command : commands) {
					if (!executionsPerHost.containsKey(command.host()))
						executionsPerHost.put(command.host(),
								new ArrayList<Execution>());

					executionsPerHost.get(command.host()).add(
							(Execution) descriptor);
				}
			}
		}

		for (Host host : executionsPerHost.keySet()) {
			host.stopExecutions(executionsPerHost.get(host));
		}
	}

	public Set<Host> hosts() {
		Set<Host> hosts = new TreeSet<Host>();

		hosts.addAll(this.ftpHosts);
		hosts.addAll(this.sshHosts);

		return hosts;
	}

	public void stopCurrentThreads() throws InterruptedException {
		Log.subheading("Waitting for current threads to stop");

		for (DependencyThread thread : this.threads)
			thread.shutdown();

		for (DependencyThread thread : this.threads)
			thread.join();
	}

}
