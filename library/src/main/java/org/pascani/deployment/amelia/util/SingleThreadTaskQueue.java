package org.pascani.deployment.amelia.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

public class SingleThreadTaskQueue extends Thread {
	
	private interface Callback<V> {
		public void complete(V result);
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
				this.callback.complete(result);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	private final ExecutorService executor;
	private final BlockingQueue<Runnable> dispatchQueue;
	
	public SingleThreadTaskQueue() {
		this.executor = Executors.newSingleThreadExecutor();
		this.dispatchQueue = new LinkedBlockingDeque<Runnable>();
	}
	
	public void run() {
		while(true) {
			try {
				this.executor.submit(dispatchQueue.take()).get();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	public <V> V execute(final Callable<V> task) throws InterruptedException {
		
		final CountDownLatch signal = new CountDownLatch(1);
		final List<V> _return = new ArrayList<V>();

		this.dispatchQueue.add(new CallbackTask<V>(task, new Callback<V>() {
			public void complete(V result) {
				_return.add(result);
				signal.countDown();
			}
		}));

		signal.await();
	
		return _return.get(0);
	}

}
