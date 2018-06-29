package com.poixson.tools.remapped;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.poixson.exceptions.RequiredArgumentException;
import com.poixson.utils.ReflectUtils;
import com.poixson.utils.Utils;


public class RemappedMethod<V> extends xRunnable {

	public final Object container;
	public final Method method;
	public final Object[] args;

	protected final AtomicReference<V> result = new AtomicReference<V>(null);
	protected final AtomicBoolean done = new AtomicBoolean(false);



	public RemappedMethod(final Object container,
			final String methodName, final Object...args) {
		this(
			null,
			container,
			ReflectUtils.getMethodByName(container, methodName, args),
			args
		);
	}
	public RemappedMethod(final Object container,
			final Method methodName, final Object...args) {
		this(
			null,
			container,
			methodName,
			args
		);
	}
	public RemappedMethod(final String taskName, final Object container,
			final String methodName, final Object...args) {
		this(
			taskName,
			container,
			ReflectUtils.getMethodByName(container, methodName, args),
			args
		);
	}
	public RemappedMethod(final String taskName, final Object container,
			final Method method, final Object...args) {
		if (container == null) throw new RequiredArgumentException("container");
		if (method == null)    throw new RequiredArgumentException("method");
		this.container = container;
		this.method    = method;
		this.args      = args;
		// static or instance class
		this.setTaskName(
			Utils.isEmpty(taskName)
			? method.getName()+"()"
			: taskName
		);
	}



	// invoke stored method
	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		try {
			this.result.set(
				(V)ReflectUtils.InvokeMethod(
					this.container,
					this.method,
					this.args
				)
			);
		} finally {
			this.done.set(true);
		}
	}



	public V getResult() {
		if ( ! this.done.get() )
			return null;
		return this.result.get();
	}
	public boolean isDone() {
		return this.done.get();
	}



}
