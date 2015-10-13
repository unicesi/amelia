package org.pascani.deployment.amelia.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.CountDownLatch;

import org.pascani.deployment.amelia.Amelia;
import org.pascani.deployment.amelia.descriptors.CommandDescriptor;
import org.pascani.deployment.amelia.descriptors.CompilationDescriptor;
import org.pascani.deployment.amelia.descriptors.ExecutionDescriptor;
import org.pascani.deployment.amelia.descriptors.Host;
import org.pascani.deployment.amelia.process.Command;
import org.pascani.deployment.amelia.process.Compile;
import org.pascani.deployment.amelia.process.Run;
import org.pascani.deployment.amelia.process.SSHHandler;

public class DependencyGraph<T extends CommandDescriptor> extends HashMap<T, List<T>> {

	public class DependencyThread extends Thread implements Observer {

		private final T element;
		private final SSHHandler handler;
		private final Command<?> command;
		private final CountDownLatch doneSignal;
		private final CountDownLatch mainDoneSignal;

		public DependencyThread(final T element, final SSHHandler handler, final Command<?> command,
				final int dependencies, final CountDownLatch doneSignal) {
			this.element = element;
			this.handler = handler;
			this.command = command;
			this.doneSignal = new CountDownLatch(dependencies);
			this.mainDoneSignal = doneSignal;
		}

		public void run() {
			try {
				this.doneSignal.await();
				this.handler.executeCommand(this.command);

				// Release this dependency
				this.element.done(this.handler.host());
				this.element.notifyObservers();

				// Notify to main thread
				this.mainDoneSignal.countDown();
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

	public DependencyGraph() {
		this.tasks = new HashMap<T, List<Command<?>>>();
	}

	public void addElement(T a, Host... hosts) {
		// Add the element with an empty list of dependencies
		put(a, new ArrayList<T>());
		this.tasks.put(a, new ArrayList<Command<?>>());

		// Add a executable task per host
		for (Host host : hosts) {
			Command<?> task = null;
			
			if(a instanceof CompilationDescriptor)
				task = new Compile(host, (CompilationDescriptor) a);
			else if(a instanceof ExecutionDescriptor)
				task = new Run(host, (ExecutionDescriptor) a);
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
			throw new RuntimeException(
					String.format("Circular reference detected: %s <-> %s", a, b));

		get(a).add(b);

		return true;
	}

	public void resolve() throws InterruptedException {
		Log.heading("Starting deployment");
		CountDownLatch doneSignal = new CountDownLatch(keySet().size());

		for (T e : keySet()) {
			List<T> dependencies = get(e);
			List<Command<?>> tasks = this.tasks.get(e);
			int deps = countDependencyThreads(dependencies, tasks);

			for(Command<?> task : tasks) {
				DependencyThread thread = 
						new DependencyThread(e, task.host().ssh(), task, deps, doneSignal);

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
	
	private int countDependencyThreads(List<T> dependencies, List<Command<?>> tasks) {
		int n = dependencies.size();
		
		for(T e : dependencies)
			for(Command<?> c : tasks)
				if(c.descriptor().equals(e))
					++n;

		return n;
	}

}
