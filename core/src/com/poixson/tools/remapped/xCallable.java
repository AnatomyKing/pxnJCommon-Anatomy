package com.poixson.tools.remapped;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;


public class xCallable<V> extends xRunnable implements Callable<V> {

	public final Callable<V> call;

	protected final ThreadLocal<Boolean> callDepth = new ThreadLocal<Boolean>();

	protected final AtomicReference<V> result = new AtomicReference<V>(null);
	protected final AtomicReference<Exception> ex = new AtomicReference<Exception>(null);



	public xCallable() {
		super();
		this.call = null;
	}
	public void finalize() {
		this.releaseCallDepth();
	}
	public xCallable(final String taskName) {
		this(null, null, null);
		this.taskName.set(taskName);
	}
	public xCallable(final V result) {
		this(result, null, null);
	}
	public xCallable(final Runnable run) {
		this(null, run, null);
	}
	public xCallable(final Callable<V> call) {
		this(null, null, call);
	}
	public xCallable(final V result, final Runnable run) {
		this(result, run, null);
	}
	public xCallable(final V result, final Callable<V> call) {
		this(result, null, call);
	}
	protected xCallable(final V result,
			final Runnable run, final Callable<V> call) {
		super(run);
		if (run != null && call != null)
			throw new IllegalArgumentException("Cannot set runnable and callable at the same time!");
		this.call   = call;
		this.result.set(result);
	}



	// ------------------------------------------------------------------------------- //
	// cast



	@SuppressWarnings("unchecked")
	public static <V> xCallable<V> cast(final Object obj) {
		if (obj == null)
			return null;
		// already correct type
		if (obj instanceof xCallable)
			return (xCallable<V>) obj;
		// cast from runnable
		if (obj instanceof Runnable) {
			final Runnable run = (Runnable) obj;
			final xCallable<V> result = new xCallable<V>(run);
			// get name from interface
			if (run instanceof RunnableNamed) {
				result.setTaskName(
					((RunnableNamed) run).getTaskName()
				);
			}
			return result;
		} else
		// cast from callable
		if (obj instanceof Callable) {
			final Callable<V> call = (Callable<V>) obj;
			final xCallable<V> result = new xCallable<V>(call);
			// get name from interface
			if (call instanceof RunnableNamed) {
				result.setTaskName(
					((RunnableNamed) call).getTaskName()
				);
			}
			return result;
		}
		// unknown object
		throw new UnsupportedOperationException("Invalid object, cannot cast!");
	}



	// ------------------------------------------------------------------------------- //
	// run task



	@Override
	public void run() {
		if (this.task != null) {
			try {
				this.task.run();
			} catch (Exception e) {
				this.result.set(null);
				this.ex.set(e);
			}
			return;
		}
		try {
			this.checkCallDepth();
			this.result.set(
				this.call()
			);
		} catch (Exception e) {
			this.result.set(null);
			this.ex.set(e);
		} finally {
			this.releaseCallDepth();
		}
	}
	@Override
	public V call() {
		if (this.call != null) {
			try {
				this.result.set(
					this.call()
				);
			} catch (Exception e) {
				this.result.set(null);
				this.ex.set(e);
				return null;
			}
			return this.result.get();
		}
		try {
			this.checkCallDepth();
			this.run();
		} catch (Exception e) {
			this.result.set(null);
			this.ex.set(e);
			return null;
		} finally {
			this.releaseCallDepth();
		}
		return this.result.get();
	}
	private void checkCallDepth() {
		final Boolean depth = this.callDepth.get();
		if (depth == null) {
			this.callDepth.set(Boolean.TRUE);
			return;
		}
		if (depth.booleanValue())
			throw new UnsupportedOperationException("Must set or override run() or call()");
		this.callDepth.set(Boolean.TRUE);
	}
	private void releaseCallDepth() {
		this.callDepth.remove();
	}



	// ------------------------------------------------------------------------------- //
	// result



	public V getResult() {
		return this.result.get();
	}
	public void setResult(final V result) {
		this.result.set(result);
	}



	public Exception getException() {
		return this.ex.get();
	}



}
