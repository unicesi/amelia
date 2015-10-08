package org.pascani.deployment.amelia.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.CountDownLatch;

public class DependencyTree<T extends Observable> extends HashMap<T, List<T>> {

	public class DependencyThread extends Thread implements Observer {

		private final T element;
		private final Runnable runnable;
		private final CountDownLatch doneSignal;

		public DependencyThread(final T element, final Runnable runnable,
				final int dependencies) {
			this.element = element;
			this.runnable = runnable;
			this.doneSignal = new CountDownLatch(dependencies);
		}

		public void run() {
			try {
				this.doneSignal.await();

				Thread t = new Thread(this.runnable);
				t.start();
				t.join();

				this.element.notifyObservers();

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

	private final Map<T, Runnable> tasks;

	public DependencyTree() {
		this.tasks = new HashMap<T, Runnable>();
	}

	public void addElement(T a, Runnable runnable) {
		this.tasks.put(a, runnable);
	}

	public boolean addDependency(T a, T b) {
		if (!containsKey(a) || !containsKey(b))
			return false;

		if (containsKey(b) && get(b).contains(a))
			throw new RuntimeException(String.format(
					"Circular reference detected: %s <-> %s", a, b));

		if (!containsKey(a))
			put(a, new ArrayList<T>());

		get(a).add(b);

		return true;
	}

	public void resolve() {
		for (T e : keySet()) {
			DependencyThread thread = new DependencyThread(e, this.tasks.get(e), get(e).size());
			e.addObserver(thread);
			
			thread.start();
		}
	}

}
