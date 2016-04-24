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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public class SingleThreadTaskQueue extends Thread {

	private interface Callback<V> {
		public void onComplete(V result);

		public void onCancel();
	}

	private class CallbackTask<V> implements Runnable {

		private final Callable<V> task;
		private final Callback<V> callback;

		public CallbackTask(Callable<V> task, Callback<V> callback) {
			this.task = task;
			this.callback = callback;
		}

		public void run() {
			try {
				V result = this.task.call();
				this.callback.onComplete(result);
			} catch (Exception e) {
				this.callback.onCancel();
				throw new RuntimeException(e.getMessage(), e.getCause());
			}
		}
	}

	private final ExecutorService executor;
	private final LinkedBlockingDeque<CallbackTask<?>> dispatchQueue;
	private volatile boolean shutdown;

	public SingleThreadTaskQueue() {
		this.executor = Executors.newSingleThreadExecutor();
		this.dispatchQueue = new LinkedBlockingDeque<CallbackTask<?>>();
	}

	public void run() {
		while (!this.shutdown) {
			CallbackTask<?> task = null;
			try {
				task = this.dispatchQueue.pollFirst(10, TimeUnit.MILLISECONDS);
				if (task != null)
					this.executor.submit(task).get();
			} catch (Exception e) {
				throw new RuntimeException(e.getMessage(), e.getCause());
			}
		}
	}

	public synchronized void shutdown() {
		if (!this.shutdown) {
			this.shutdown = true;
			this.executor.shutdown();
			// release locks of canceled tasks
			for (CallbackTask<?> task : dispatchQueue) {
				task.callback.onCancel();
			}
		}
	}

	public <V> V execute(final Callable<V> task) throws InterruptedException {
		final CountDownLatch signal = new CountDownLatch(1);
		final List<V> _return = new ArrayList<V>();
		_return.add(0, null);

		this.dispatchQueue.add(new CallbackTask<V>(task, new Callback<V>() {
			public void onComplete(V result) {
				_return.add(0, result);
				signal.countDown();
			}
			public void onCancel() {
				signal.countDown();
			}
		}));

		signal.await();
		return _return.get(0);
	}

}
