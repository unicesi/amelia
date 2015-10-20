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
package org.pascani.deployment.amelia.util;

import static org.pascani.deployment.amelia.util.Strings.ascii;

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
import java.util.concurrent.CountDownLatch;

import org.pascani.deployment.amelia.Amelia;
import org.pascani.deployment.amelia.SSHHandler;
import org.pascani.deployment.amelia.commands.Command;
import org.pascani.deployment.amelia.commands.Compile;
import org.pascani.deployment.amelia.commands.PrerequisiteCheck;
import org.pascani.deployment.amelia.commands.Run;
import org.pascani.deployment.amelia.commands.Transfer;
import org.pascani.deployment.amelia.descriptors.AssetBundle;
import org.pascani.deployment.amelia.descriptors.CommandDescriptor;
import org.pascani.deployment.amelia.descriptors.Compilation;
import org.pascani.deployment.amelia.descriptors.Execution;
import org.pascani.deployment.amelia.descriptors.Host;
import org.pascani.deployment.amelia.descriptors.Prerequisites;

public class DependencyGraph<T extends CommandDescriptor> extends
		HashMap<T, List<T>> {

	public class DependencyThread extends Thread implements Observer {

		private final T element;
		private final SSHHandler handler;
		private final Command<?> command;
		private final CountDownLatch doneSignal;
		private final CountDownLatch mainDoneSignal;

		public DependencyThread(final T element, final SSHHandler handler,
				final Command<?> command, final int dependencies,
				final CountDownLatch doneSignal) {
			this.element = element;
			this.handler = handler;
			this.command = command;
			this.doneSignal = new CountDownLatch(dependencies);
			this.mainDoneSignal = doneSignal;
		}

		public void run() {
			try {
				this.doneSignal.await();
				if (!Amelia.aborting) {
					this.handler.executeCommand(this.command);
					// Release this dependency
					this.element.done(this.handler.host());
					this.element.notifyObservers();

					// Notify to main thread
					this.mainDoneSignal.countDown();
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		public void update(Observable o, Object arg) {
			this.doneSignal.countDown();
		}
	}

	/**
	 * Serial version UID
	 */
	private static final long serialVersionUID = -6806533450294013309L;

	private final Map<T, List<Command<?>>> tasks;

	private final Set<Host> hosts;

	public DependencyGraph() {
		this.tasks = new HashMap<T, List<Command<?>>>();
		this.hosts = new HashSet<Host>();
	}

	public void addDescriptor(T a, Host... hosts) {
		// Store the hosts
		this.hosts.addAll(Arrays.asList(hosts));

		// Add the element with an empty list of dependencies
		put(a, new ArrayList<T>());
		this.tasks.put(a, new ArrayList<Command<?>>());

		// Add a executable task per host
		for (Host host : hosts) {
			Command<?> task = null;

			// TODO: Create a Factory
			if (a instanceof Compilation)
				task = new Compile(host, (Compilation) a);
			else if (a instanceof Execution)
				task = new Run(host, (Execution) a);
			else if (a instanceof AssetBundle)
				task = new Transfer(host, (AssetBundle) a);
			else if (a instanceof Prerequisites)
				task = new PrerequisiteCheck(host, (Prerequisites) a);
			else
				task = new Command.Simple(host, a);

			this.tasks.get(a).add(task);
		}
	}

	public boolean addDependency(T a, T b) {
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

		Log.printBanner();

		// Open SSH and FTP connections before dependencies resolution
		// TODO: validate if FTP/SSH is needed before opening connections
		Host[] _hosts = this.hosts.toArray(new Host[0]);
		Amelia.openSSHConnections(_hosts);
		Amelia.openFTPConnections(_hosts);

		if (stopPreviousExecutions) {
			stopPreviousExecutions();
		}

		Log.heading("Starting deployment (" + this.tasks.size() + ")");
		CountDownLatch doneSignal = new CountDownLatch(keySet().size());

		for (T e : keySet()) {
			List<T> dependencies = get(e);
			List<Command<?>> tasks = this.tasks.get(e);
			int deps = countDependencyThreads(dependencies, tasks);

			for (Command<?> task : tasks) {
				DependencyThread thread = new DependencyThread(e, task.host()
						.ssh(), task, deps, doneSignal);

				// Make the thread observe the corresponding dependencies
				for (T dependency : get(e))
					dependency.addObserver(thread);

				// Handle uncaught exceptions
				thread.setUncaughtExceptionHandler(Amelia.exceptionHandler);
				thread.start();
			}
		}

		doneSignal.await();
	}

	private void stopPreviousExecutions() throws IOException {
		Log.heading("Stopping previous executions");
		int stopped = 0;

		Map<Host, List<Execution>> executionsPerHost = new HashMap<Host, List<Execution>>();

		for (T descriptor : this.tasks.keySet()) {
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
			List<Execution> executions = new ArrayList<Execution>();
			executions.addAll(executionsPerHost.get(host));

			stopped += host.stopExecutions(executions);
		}
		
		if(stopped == 0)
			Log.info("  " + ascii(10003) + " 0 components were running");
			
	}

	private int countDependencyThreads(List<T> dependencies,
			List<Command<?>> tasks) {
		int n = dependencies.size();

		for (T e : dependencies)
			for (Command<?> c : tasks)
				if (c.descriptor().equals(e))
					++n;

		return n;
	}

}
