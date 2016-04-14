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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.amelia.dsl.lib.util.Configuration;
import org.amelia.dsl.lib.util.Log;

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public class SubsystemGraph extends HashMap<Subsystem, List<Subsystem>> {

	public class DependencyThread extends Thread
			implements Observer, Comparable<DependencyThread> {

		private final Subsystem subsystem;
		private final List<Subsystem> dependencies;
		private final CountDownLatch doneSignal;
		private final CountDownLatch mainDoneSignal;
		private volatile boolean shutdown;
		private final SingleThreadTaskQueue taskQueue;

		public DependencyThread(final Subsystem subsystem,
				final List<Subsystem> dependencies,
				final CountDownLatch doneSignal,
				final SingleThreadTaskQueue taskQueue) {
			this.subsystem = subsystem;
			this.dependencies = dependencies;
			this.doneSignal = new CountDownLatch(dependencies.size());
			this.mainDoneSignal = doneSignal;
			this.shutdown = false;
			this.taskQueue = taskQueue;

			// Make this thread observe the corresponding dependencies
			for (Subsystem dependency : this.dependencies)
				dependency.deployment().addObserver(this);
		}

		public void run() {
			try {
				this.doneSignal.await();
				if (!this.shutdown) {
					taskQueue.execute(new Callable<Void>() {
						public Void call() throws Exception {
							subsystem.start();
							subsystem.deployment().deploy(subsystem.alias(),
									dependencies);
							if (!shutdown)
								subsystem.done();
							else
								subsystem.error();
							return null;
						}
					});
					this.subsystem.deployment().setChanged();
					this.subsystem.deployment().notifyObservers();
				}
			} catch (Exception e) {
				this.subsystem.error();
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
			this.subsystem.deployment().deleteObserver(this);

			while (this.doneSignal.getCount() > 0)
				this.doneSignal.countDown();
		}
	}

	/**
	 * Serial version UID
	 */
	private static final long serialVersionUID = -6806533450294013309L;

	private final List<Subsystem> subsystems;

	private final TreeSet<DependencyThread> threads;

	private final SingleThreadTaskQueue taskQueue;

	private volatile boolean shutdown;

	private static SubsystemGraph instance;

	private SubsystemGraph() {
		new Configuration().setProperties();
		this.subsystems = new ArrayList<Subsystem>();
		this.threads = new TreeSet<DependencyThread>();
		this.taskQueue = new SingleThreadTaskQueue();
		this.taskQueue.start();
	}

	public static SubsystemGraph getInstance() {
		if (instance == null)
			instance = new SubsystemGraph();

		return instance;
	}
	
	/*
	 * FIXME: search for transitive dependencies
	 */
	public boolean addSubsystems(Subsystem... subsystems) {
		boolean all = true;
		for (Subsystem subsystem : subsystems) {
			if (this.subsystems.contains(subsystem)) {
				all = false;
				continue;
			}
			this.subsystems.add(subsystem);
			put(subsystem, subsystem.dependencies());
		}
		return all;
	}

	/**
	 * @deprecated use {@link #addSubsystems(Subsystem...)} instead
	 * @param subsystem
	 *            The subsystem to add
	 * @return Whether or not the subsystem was added
	 */
	@Deprecated
	public boolean addSubsystem(Subsystem subsystem) {
		if (containsKey(subsystem))
			return false;

		// Add the element with an empty list of dependencies
		put(subsystem, new ArrayList<Subsystem>());
		return this.subsystems.add(subsystem);
	}

	/**
	 * @deprecated use {@link Subsystem #dependsOn(Subsystem...)} instead
	 * @param a
	 *            The dependent subsystem
	 * @param b
	 *            The dependency
	 * @return Whether or not the dependency was added
	 */
	@Deprecated
	public boolean addDependency(Subsystem a, Subsystem b) {
		if (!containsKey(a) || !containsKey(b))
			return false;

		// FIXME: search for transitive dependencies
		if (get(b).contains(a))
			throw new RuntimeException(String
					.format("Circular reference detected: %s <-> %s", a, b));

		return get(a).add(b);
	}
	
	/**
	 * @return whether or not all of the subsystem dependencies are satisfiable
	 */
	public boolean validate() {
		boolean valid = true;
		for (Subsystem subsystem : keySet()) {
			for (Subsystem dependency : subsystem.dependencies()) {
				if (!keySet().contains(dependency)) {
					Log.error("Unmeetable subsystem dependency '"
							+ dependency.alias()
							+ "'. The graph must contain all of the subsystems.");
					valid = false;
					break;
				}
			}
		}
		return valid;
	}

	/**
	 * @return whether the execution was successful or not
	 */
	public boolean execute(boolean stopExecutedComponents) throws InterruptedException {
		boolean successful = false;
		Log.printBanner();
		if (validate()) {
			CountDownLatch doneSignal = new CountDownLatch(this.subsystems.size());
			for (Subsystem subsystem : this.subsystems) {
				List<Subsystem> dependencies = get(subsystem);
				DependencyThread thread = new DependencyThread(subsystem,
						dependencies, doneSignal, this.taskQueue);
				threads.add(thread);
			}
			Log.info("Resolving subsystems (" + this.subsystems.size() + ")");
			long start = System.nanoTime();
			for (DependencyThread thread : this.threads) {
				thread.start();
			}
			// Wait for all threads to finish
			doneSignal.await();
			shutdown(stopExecutedComponents);
			printExecutionSummary(start, System.nanoTime());
			successful = !ExecutionManager.isAnySubsystemAborting();
			ExecutionManager.reset();
		} else {
			shutdown(stopExecutedComponents);
		}
		return successful;
	}
	
	private void printExecutionSummary(final long start, final long end) {
		Log.print(Log.SEPARATOR_LONG);
		if (!ExecutionManager.isAnySubsystemAborting()) {
			Log.print("DEPLOYMENT SUCCESS");
		} else {
			Log.print("DEPLOYMENT ERROR");
		}
		Log.print(Log.SEPARATOR_LONG);
		Log.print("Total time: "
				+ TimeUnit.SECONDS.convert(end - start, TimeUnit.NANOSECONDS) + "s");
		Log.print("Finished at: " + new Date());
		Log.print(Log.SEPARATOR_LONG);
	}

	public void shutdown(final boolean stopExecutedComponents) {
		if(!this.shutdown) {
			this.shutdown = true;
			this.taskQueue.shutdown();
			for (DependencyThread thread : this.threads)
				thread.shutdown();
			boolean any = false;
			for (Subsystem subsystem : this.subsystems) {
				if(!subsystem.deployment().isShutdown()) {
					if (!any) {
						any = true;
						Log.print(Log.SEPARATOR_LONG);
					}
					subsystem.deployment().shutdown(stopExecutedComponents);
				}
			}
			instance = null;
		}
	}

}
