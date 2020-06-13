package com.poixson.tools.scheduler;

import java.lang.ref.SoftReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.poixson.abstractions.xEnableable;
import com.poixson.exceptions.RequiredArgumentException;
import com.poixson.logger.xLog;
import com.poixson.logger.xLogRoot;
import com.poixson.threadpool.xThreadPool;
import com.poixson.threadpool.types.xThreadPool_Main;
import com.poixson.tools.xTime;
import com.poixson.tools.remapped.xRunnable;
import com.poixson.utils.Utils;


public class xSchedulerTask extends xRunnable implements xEnableable {

	private final xScheduler sched;

	private static final AtomicInteger taskCount = new AtomicInteger(0);
	private final int taskIndex;

	// task config
	private final AtomicBoolean enabled = new AtomicBoolean(true);

	private final AtomicReference<xRunnable> run =
			new AtomicReference<xRunnable>(null);
	private final AtomicReference<xThreadPool> pool =
			new AtomicReference<xThreadPool>(null);

	// triggers
	private final Set<xSchedulerTrigger> triggers =
			new CopyOnWriteArraySet<xSchedulerTrigger>();

	// task run count
	private final AtomicLong runCount = new AtomicLong(0L);

	// wait functions
	private final ReentrantLock waitForNextRunStart_Lock     = new ReentrantLock();
	private final ReentrantLock waitForNextRunCompleted_Lock = new ReentrantLock();
	private final Condition waitForNextRunStart     = this.waitForNextRunStart_Lock.newCondition();
	private final Condition waitForNextRunCompleted = this.waitForNextRunCompleted_Lock.newCondition();



	public static xSchedulerTask getNew() {
		return new xSchedulerTask(null);
	}
	public xSchedulerTask(final xScheduler sched) {
		this.sched = (
			sched == null
			? xScheduler.getMainSched()
			: sched
		);
		this.taskIndex = taskCount.incrementAndGet();
	}



	public void register() {
		final xScheduler sched = this.sched;
		if (sched == null) throw new RequiredArgumentException("sched");
		sched.add(this);
	}
	public void unregister() {
		final Iterator<xSchedulerTrigger> it = this.triggers.iterator();
		while (it.hasNext()) {
			it.next()
				.unregister();
		}
	}



	public long untilSoonestTrigger(final long now) {
		if (this.notEnabled())
			return Long.MIN_VALUE;
		// run once has finished
		if (this.notRepeating()) {
			if (this.runCount.get() > 0) {
				return Long.MIN_VALUE;
			}
		}
		if (this.triggers.isEmpty())
			return Long.MIN_VALUE;
		final Iterator<xSchedulerTrigger> it = this.triggers.iterator();
		long lowest = Long.MAX_VALUE;
		while (it.hasNext()) {
			final xSchedulerTrigger trigger = it.next();
			final long untilNext = trigger.untilNextTrigger(now);
			if (untilNext < lowest) {
				lowest = untilNext;
				if (lowest <= 0L)
					break;
			}
		}
		if (lowest == Long.MAX_VALUE)
			return Long.MIN_VALUE;
		return lowest;
	}



	public void doTrigger() {
		if (this.notEnabled()) {
			this.log()
				.warning("Skipping disabled task.. this should only happen rarely.");
			return;
		}
		// run task
		this.runCount.incrementAndGet();
		final xThreadPool pool = this.getThreadPool();
		try {
			this.waitForNextRunStart.signalAll();
		} catch (IllegalMonitorStateException ignore) {}
//TODO: improve priorities
		pool.runTaskLater(this);
		try {
			this.waitForNextRunCompleted.signalAll();
		} catch (IllegalMonitorStateException ignore) {}
	}



	// run task
	@Override
	public void run() {
		if (this.run != null) {
			final xRunnable run = this.run.get();
			if (run != null) {
				run.run();
				return;
			}
		}
		this.setDisabled();
		throw new RequiredArgumentException("run");
	}



	// task run count
	public long getRunCount() {
		return this.runCount
				.get();
	}
	public long resetRunCount() {
		return this.runCount
				.getAndSet(0L);
	}



	// ------------------------------------------------------------------------------- //
	// wait functions



	public void waitForNextRunStart()
			throws InterruptedException {
		this.waitForNextRunStart_Lock.lockInterruptibly();
		try {
			this.waitForNextRunStart.await();
		} finally {
			this.waitForNextRunStart_Lock.unlock();
		}
	}
	public void waitForNextRunStart(final long time, final TimeUnit unit)
			throws InterruptedException {
		this.waitForNextRunStart_Lock.lockInterruptibly();
		try {
			this.waitForNextRunStart.await(time, unit);
		} finally {
			this.waitForNextRunStart_Lock.unlock();
		}
	}
	public void waitForNextRunStart(final String timeStr)
			throws InterruptedException {
		this.waitForNextRunStart_Lock.lockInterruptibly();
		try {
			this.waitForNextRunStart(xTime.getNew(timeStr));
		} finally {
			this.waitForNextRunStart_Lock.unlock();
		}
	}
	public void waitForNextRunStart(final xTime time)
			throws InterruptedException {
		this.waitForNextRunStart_Lock.lockInterruptibly();
		try {
			this.waitForNextRunStart(time.getMS(), TimeUnit.MILLISECONDS);
		} finally {
			this.waitForNextRunStart_Lock.unlock();
		}
	}



	public void waitForNextRunCompleted()
			throws InterruptedException {
		this.waitForNextRunCompleted_Lock.lockInterruptibly();
		try {
			this.waitForNextRunCompleted.await();
		} finally {
			this.waitForNextRunCompleted_Lock.unlock();
		}
	}
	public void waitForNextRunCompleted(final long time, final TimeUnit unit)
			throws InterruptedException {
		this.waitForNextRunCompleted_Lock.lockInterruptibly();
		try {
			this.waitForNextRunCompleted.await(time, unit);
		} finally {
			this.waitForNextRunCompleted_Lock.unlock();
		}
	}
	public void waitForNextRunCompleted(final String timeStr)
			throws InterruptedException {
		this.waitForNextRunCompleted_Lock.lockInterruptibly();
		try {
			this.waitForNextRunCompleted(xTime.getNew(timeStr));
		} finally {
			this.waitForNextRunCompleted_Lock.unlock();
		}
	}
	public void waitForNextRunCompleted(final xTime time)
			throws InterruptedException {
		this.waitForNextRunCompleted_Lock.lockInterruptibly();
		try {
			this.waitForNextRunCompleted(time.getMS(), TimeUnit.MILLISECONDS);
		} finally {
			this.waitForNextRunCompleted_Lock.unlock();
		}
	}



	// ------------------------------------------------------------------------------- //
	// task config



	// task enabled
	@Override
	public boolean isEnabled() {
		if ( ! this.enabled.get() )
			return false;
		final Iterator<xSchedulerTrigger> it = this.triggers.iterator();
		while (it.hasNext()) {
			final xSchedulerTrigger trigger = it.next();
			if (trigger.isEnabled())
				return true;
		}
		return false;
	}
	@Override
	public boolean notEnabled() {
		return ! this.isEnabled();
	}
	@Override
	public void setEnabled() {
		this.setEnabled(true);
	}
	@Override
	public void setDisabled() {
		this.setEnabled(false);
	}
	@Override
	public void setEnabled(boolean enabled) {
		this.enabled.set(enabled);
	}



	// repeating triggers
	public boolean isRepeating() {
		final Iterator<xSchedulerTrigger> it = this.triggers.iterator();
		while (it.hasNext()) {
			if (it.next().isRepeating())
				return true;
		}
		return false;
	}
	public boolean notRepeating() {
		return ! this.isRepeating();
	}



	// runnable
	public xRunnable getRunnable() {
		return this.run.get();
	}
	public xSchedulerTask setRunnable(final Runnable run) {
		this.run.set(
			xRunnable.cast(run)
		);
		return this;
	}



	// task name
	@Override
	public String getTaskName() {
		if (this.run != null) {
			final xRunnable run = this.run.get();
			if (run != null) {
				final String runTaskName = run.getTaskName();
				if (Utils.notEmpty(runTaskName)) {
					return runTaskName;
				}
			}
		}
		final String taskName = super.getTaskName();
		return Utils.ifEmpty(taskName, "task"+Integer.toString(this.taskIndex));
	}
	@Override
	public void setTaskName(final String taskName) {
		super.setTaskName(taskName);
		this._log.set(null);
		final xRunnable run = this.run.get();
		if (run != null) {
			run.setTaskName(taskName);
		}
	}
	public xSchedulerTask setName(final String taskName) {
		this.setTaskName(taskName);
		return this;
	}
	@Override
	public boolean taskNameEquals(final String taskName) {
		final xRunnable run = this.run.get();
		if (run != null) {
			final String runTaskName = run.getTaskName();
			if (Utils.isEmpty(runTaskName))
				return Utils.isEmpty(runTaskName);
			return runTaskName.equals(taskName);
		}
		return super.taskNameEquals(taskName);
	}



	// thread pool
	public xThreadPool getThreadPool() {
		final xThreadPool pool = this.pool.get();
		return (
			pool == null
			? xThreadPool_Main.get()
			: pool
		);
	}
	public xSchedulerTask setThreadPool(final xThreadPool pool) {
		this.pool.set(pool);
		return this;
	}



	// trigger
	public xSchedulerTrigger[] getTriggers() {
		return this.triggers.toArray(new xSchedulerTrigger[0]);
	}
	public int getTriggersCount() {
		return this.triggers.size();
	}
	public xSchedulerTask addTrigger(final xSchedulerTrigger trigger) {
		if (trigger != null) {
			this.triggers.add(trigger);
		}
		return this;
	}
	public xSchedulerTask clearTriggers() {
		if (!this.triggers.isEmpty()) {
			final Iterator<xSchedulerTrigger> it = this.triggers.iterator();
			final Set<xSchedulerTrigger> removing = new HashSet<xSchedulerTrigger>();
			while (it.hasNext()) {
				final xSchedulerTrigger trigger = it.next();
				trigger.unregister();
				removing.add(trigger);
			}
			for (final xSchedulerTrigger trigger : removing) {
				this.triggers.remove(trigger);
			}
		}
		return this;
	}



	// ------------------------------------------------------------------------------- //
	// logger



	private final AtomicReference<SoftReference<xLog>> _log =
			new AtomicReference<SoftReference<xLog>>(null);

	public xLog log() {
		// cached logger
		final SoftReference<xLog> ref = this._log.get();
		if (ref != null) {
			final xLog log = ref.get();
			if (log != null)
				return log;
		}
		// get logger
		{
			final String taskName = this.getTaskName();
			final xLog log =
				xLogRoot.Get()
					.getWeak(taskName);
			this._log.set(
				new SoftReference<xLog>(
					log
				)
			);
			return log;
		}
	}



}
