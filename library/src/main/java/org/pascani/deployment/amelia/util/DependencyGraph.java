package org.pascani.deployment.amelia.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import org.pascani.deployment.amelia.Amelia;
import org.pascani.deployment.amelia.descriptors.CommandDescriptor;
import org.pascani.deployment.amelia.descriptors.CompilationDescriptor;
import org.pascani.deployment.amelia.descriptors.ExecutionDescriptor;
import org.pascani.deployment.amelia.process.Command;
import org.pascani.deployment.amelia.process.Compile;
import org.pascani.deployment.amelia.process.Run;
import org.pascani.deployment.amelia.process.SSHHandler;

public class DependencyGraph<T extends CommandDescriptor> extends
		HashMap<T, Map.Entry<SSHHandler, List<T>>> {

	public class DependencyThread extends Thread implements Observer {

		private final T element;
		private final SSHHandler handler;
		private final Callable<?> callable;
		private final CountDownLatch doneSignal;
		private final CountDownLatch mainDoneSignal;

		public DependencyThread(final T element, final SSHHandler handler,
				final Callable<?> callable, final int dependencies,
				final CountDownLatch doneSignal) {
			this.element = element;
			this.handler = handler;
			this.callable = callable;
			this.doneSignal = new CountDownLatch(dependencies);
			this.mainDoneSignal = doneSignal;
		}

		public void run() {
			try {
				this.doneSignal.await();
				this.handler.executeCommand(this.callable);

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

	private class Entry<K, V> implements Map.Entry<K, V> {

		private K key;
		private V value;

		public Entry(K key, V value) {
			this.key = key;
			this.value = value;
		}

		public K getKey() {
			return this.key;
		}

		public V getValue() {
			return this.value;
		}

		public V setValue(V value) {
			this.value = value;
			return this.value;
		}
	}

	/**
	 * Serial version UID
	 */
	private static final long serialVersionUID = -6806533450294013309L;

	private final Map<T, Callable<?>> tasks;

	public DependencyGraph() {
		this.tasks = new HashMap<T, Callable<?>>();
	}

	public void addElement(T a, SSHHandler handler) {
		put(a, new Entry<SSHHandler, List<T>>(handler, new ArrayList<T>()));
		Callable<?> callable = null;

		if (a instanceof CompilationDescriptor)
			callable = new Compile(handler, (CompilationDescriptor) a);
		else if (a instanceof ExecutionDescriptor)
			callable = new Run(handler, (ExecutionDescriptor) a);
		else
			callable = new Command(handler, (CommandDescriptor) a);

		this.tasks.put(a, callable);
	}

	public boolean addDependency(T a, T b) {
		if (!containsKey(a) || !containsKey(b))
			return false;

		// FIXME: search for transitive dependencies
		if (get(b).getValue().contains(a))
			throw new RuntimeException(String.format(
					"Circular reference detected: %s <-> %s", a, b));

		get(a).getValue().add(b);

		return true;
	}

	public void resolve() throws InterruptedException {
		CountDownLatch doneSignal = new CountDownLatch(keySet().size());

		for (T e : keySet()) {
			DependencyThread thread = new DependencyThread(e, get(e).getKey(),
					this.tasks.get(e), get(e).getValue().size(), doneSignal);

			// Make the thread observe the configured dependencies
			for (T dependency : get(e).getValue()) {
				dependency.addObserver(thread);
			}
			
			thread.start();
		}

		doneSignal.await();
	}

}
