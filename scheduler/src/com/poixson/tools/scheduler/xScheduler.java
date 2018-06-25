package com.poixson.tools.scheduler;

import java.lang.ref.SoftReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

import com.poixson.abstractions.xStartable;
import com.poixson.logger.xLog;
import com.poixson.logger.xLogRoot;
import com.poixson.tools.Keeper;
import com.poixson.tools.xClock;
import com.poixson.tools.xTime;
import com.poixson.utils.NumberUtils;
import com.poixson.utils.ThreadUtils;
import com.poixson.utils.Utils;


public class xScheduler implements xStartable {

	private static final String LOG_NAME        = "xSched";
	private static final String MAIN_SCHED_NAME = "main";

	private static final ConcurrentMap<String, xScheduler> instances =
			new ConcurrentHashMap<String, xScheduler>();

	private final String schedName;
	private final Set<xSchedulerTask> tasks = new CopyOnWriteArraySet<xSchedulerTask>();

	// manager thread
	private final Thread thread;
	private final AtomicBoolean running = new AtomicBoolean(false);
	private volatile boolean stopping = false;

	// manager thread sleep
	private final xTime threadSleepTime = xTime.getNew("5s");
	private final double threadSleepInhibitPercent = 0.95;
	private volatile boolean sleeping = false;
	private volatile boolean changes  = false;



	public static xScheduler getMainSched() {
		return get(MAIN_SCHED_NAME);
	}
	public static xScheduler get(final String schedName) {
		final String name = (
			Utils.isBlank(schedName)
			? MAIN_SCHED_NAME
			: schedName
		);
		// existing scheduler
		{
			final xScheduler sched = instances.get(name);
			if (sched != null)
				return sched;
		}
		// new scheduler instance
		{
			final xScheduler sched = new xScheduler(name);
			final xScheduler existing =
				instances.putIfAbsent(name, sched);
			if (existing != null) {
				Keeper.remove(sched);
				return existing;
			}
			return sched;
		}
	}



	private xScheduler(final String schedName) {
		if (Utils.isBlank(schedName)) throw new IllegalArgumentException("shedName");
		this.schedName = schedName;
		this.thread = new Thread(this);
		this.thread.setDaemon(false);
		this.thread.setName(LOG_NAME);
		Keeper.add(this);
	}
	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}



	@Override
	public void start() {
		if (this.stopping)    throw new RuntimeException("Scheduler already stopping");
		if (this.isRunning()) throw new RuntimeException("Scheduler already running");
		this.thread.start();
	}
	@Override
	public void stop() {
		this.stopping = true;
		this.wakeManager();
	}



//TODO: get list of tasks to run and trigger in proper order
	// manager loop
	@Override
	public void run() {
		if (this.stopping)
			return;
		if (!this.running.compareAndSet(false, true))
			return;
		this.log().fine("Starting schedule manager..");
		final long threadSleep = this.threadSleepTime.getMS();
		final Set<xSchedulerTask> finishedTasks = new HashSet<xSchedulerTask>();
		while (true) {
			if (this.stopping || !this.isRunning())
				break;
			long sleep = threadSleep;
			finishedTasks.clear();
			// check task triggers
			{
				final Iterator<xSchedulerTask> it = this.tasks.iterator();
				this.changes = false;
				final long now = getClockMillis();
				while (it.hasNext()) {
					final xSchedulerTask task = it.next();
					final long untilNext = task.untilSoonestTrigger(now);
					// disabled
					if (untilNext == Long.MIN_VALUE)
						continue;
					// trigger now
					if (untilNext <= 0L) {
						// clear thread interrupt
						Thread.interrupted();
						task.doTrigger();
						// mark for removal
						if (task.notRepeating()) {
							finishedTasks.add(task);
						}
						// running again soon
						if (task.untilSoonestTrigger(now) < 0L) {
							this.changes = true;
							continue;
						}
					}
					if (untilNext < sleep) {
						sleep = untilNext;
					}
				}
			}
			// remove finished tasks
			if (!finishedTasks.isEmpty()) {
				final Iterator<xSchedulerTask> it = finishedTasks.iterator();
				while (it.hasNext()) {
					final xSchedulerTask task = it.next();
					task.unregister();
					this.tasks.remove(task);
				}
			}
			// no sleep needed
			if (this.changes || sleep <= 0L)
				continue;
			// calculate sleep
			final long sleepLess = (
				sleep <= threadSleep
				? (long) Math.ceil( ((double) sleep) * this.threadSleepInhibitPercent )
				: threadSleep
			);
			// no sleep needed
			if (this.changes || sleep <= 0L)
				continue;
			// log sleep time
			if (this.isDetailedLogging()) {
				final double sleepLessSec = ((double)sleepLess) / 1000.0;
				log().finest(
					"Sleeping.. {} s",
					NumberUtils.FormatDecimal("0.000", sleepLessSec)
				);
			}
			// sleep until next check
			this.sleeping = true;
			if (!this.changes) {
				ThreadUtils.Sleep(sleepLess);
			}
			this.sleeping = false;
		}
		finishedTasks.clear();
		log().fine("Stopped scheduler manager thread");
		this.stopping = true;
		this.running.set(false);
		Keeper.remove(this);
	}
	public void wakeManager() {
		this.changes = true;
		if (this.sleeping) {
			try {
				this.thread.interrupt();
			} catch (Exception ignore) {}
		}
	}



	@Override
	public boolean isRunning() {
		if (this.stopping)
			return false;
		return this.running.get();
	}
	@Override
	public boolean isStopping() {
		return this.stopping;
	}



	// ------------------------------------------------------------------------------- //
	// scheduler config



	// scheduler name
	public String getName() {
		return this.schedName;
	}



	// tasks
	public void add(final xSchedulerTask task) {
		this.tasks.add(task);
		this.wakeManager();
	}
//TODO:
/*
	public boolean hasTask(final String taskName) {
		if (Utils.isBlank(taskName))
			return false;
		final Iterator<xSchedulerTask> it = this.tasks.iterator();
		while (it.hasNext()) {
			final String name = it.next().getTaskName();
			if (taskName.equals(name))
				return true;
			if (it.next().taskNameEquals(taskName)) {
				return true;
			}
		}
		return false;
	}
*/



	public boolean cancel(final String taskName) {
		if (Utils.isBlank(taskName))
			return false;
		boolean found = false;
		final Iterator<xSchedulerTask> it = this.tasks.iterator();
		while (it.hasNext()) {
			final String name = it.next().getTaskName();
			if (taskName.equals(name)) {
				it.remove();
				found = true;
			}
		}
		return found;
	}
	public boolean cancel(final xSchedulerTask task) {
		if (task == null)
			return false;
		if (this.tasks.contains(task)) {
			task.unregister();
			return this.tasks.remove(task);
		}
		return false;
	}



	// ------------------------------------------------------------------------------- //



	private static volatile SoftReference<xClock> _clock = null;

	public static long getClockMillis() {
		return getClock()
				.millis();
	}
	public static xClock getClock() {
		if (_clock != null) {
			final xClock clock = _clock.get();
			if (clock != null)
				return clock;
		}
		final xClock clock = xClock.get(false);
		_clock = new SoftReference<xClock>(clock);
		return clock;
	}



	// logger
	private volatile SoftReference<xLog> _log = null;
	public xLog log() {
		if (this._log != null) {
			final xLog log = this._log.get();
			if (log != null)
				return log;
		}
		final xLog log =
			xLogRoot.Get(LOG_NAME)
				.getWeak(this.getName());
		this._log = new SoftReference<xLog>(log);
		return log;
	}



	// cached log level
	private volatile SoftReference<Boolean> _detail = null;
	public boolean isDetailedLogging() {
		if (this._detail != null) {
			final Boolean detail = this._detail.get();
			if (detail != null)
				return detail.booleanValue();
		}
		final boolean detail = this.log().isDetailLoggable();
		this._detail = new SoftReference<Boolean>(Boolean.valueOf(detail));
		return detail;
	}



}
