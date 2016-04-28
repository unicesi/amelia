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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.amelia.dsl.lib.util.ANSI;
import org.amelia.dsl.lib.util.Configuration;
import org.amelia.dsl.lib.util.Log;
import org.amelia.dsl.lib.util.Threads;

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public class SubsystemGraph extends HashMap<Subsystem, List<Subsystem>> {

	public class DependencyThread extends Thread
			implements Observer, Comparable<DependencyThread> {

		private final UUID internalId;
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
			this.internalId = UUID.randomUUID();
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
				throw new RuntimeException(e.getMessage(), e.getCause());
			} finally {
				// Notify to main thread
				this.mainDoneSignal.countDown();
			}
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
	public boolean execute(final boolean stopExecutedComponents)
			throws InterruptedException {
		return execute(stopExecutedComponents, true);
	}
	
	/**
	 * @return whether the execution was successful or not
	 */
	public boolean execute(final boolean stopExecutedComponents,
			final boolean shutdownAfterDeployment) throws InterruptedException {
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
			successful = !Threads.isAnySubsystemAborting();
			if (shutdownAfterDeployment || !successful) {
				shutdown(stopExecutedComponents);
				printExecutionSummary(start, System.nanoTime(), false);
			} else {
				Thread t = shutdownHook(stopExecutedComponents);
				printExecutionSummary(start, System.nanoTime(), true);
				t.join();
			}
			Threads.reset();
		} else {
			shutdown(stopExecutedComponents);
		}
		// Before return and continue the execution in the main thread, re-start
		// everything!
		instance = null;
		return successful;
	}
	
	private Thread shutdownHook(final boolean stopExecutedComponents) {
		Thread t = new Thread() {
			@Override public void run() {
				try {
					System.in.read();
				} catch (IOException e) {
				} finally {
					shutdown(stopExecutedComponents);
				}
			}
		};
		t.start();
		return t;
	}
	
	private void printExecutionSummary(final long start, final long end,
			final boolean waitAfterDeployment) throws InterruptedException {
		// FIXME: find out why this line is reached without closing all SSH
		// connections
		Thread.sleep(500);
		StringBuilder sb = new StringBuilder();
		sb.append(Log.SEPARATOR_LONG + "\n");
		if (!Threads.isAnySubsystemAborting()) {
			sb.append("DEPLOYMENT SUCCESS\n");
		} else {
			sb.append("DEPLOYMENT ERROR\n");
		}
		sb.append(Log.SEPARATOR_LONG + "\n");
		sb.append("Total time: "
				+ TimeUnit.SECONDS.convert(end - start, TimeUnit.NANOSECONDS) + "s\n");
		sb.append("Finished at: " + new Date());
		Log.print(sb.toString());
		if (waitAfterDeployment)
			Log.print(ANSI.RED.format("Press Enter to shutdown deployment..."));
		else
			Log.print(Log.SEPARATOR_LONG);
	}

	public void shutdown(final boolean stopExecutedComponents) {
		if(!this.shutdown) {
			this.shutdown = true;
			this.taskQueue.shutdown();
			for (DependencyThread thread : this.threads)
				thread.shutdown();
			
			// There is at least one subsystem to shutdown
			for (int i = 0, n = 0; n == 0 && i < this.subsystems.size(); i++)
				if (!this.subsystems.get(i).deployment().isShutdown()) {
					++n;
					Log.print(Log.SEPARATOR_LONG + "\nDEPLOYMENT SHUTDOWN\n"
							+ Log.SEPARATOR_LONG);
				}
					
			for (Subsystem subsystem : this.subsystems)
				if(!subsystem.deployment().isShutdown())
					subsystem.deployment().shutdown(stopExecutedComponents);
		}
	}

}
