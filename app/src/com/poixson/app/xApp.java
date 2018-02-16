package com.poixson.app;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import com.poixson.abstractions.xStartable;
import com.poixson.app.xAppStep.StepType;
import com.poixson.exceptions.RequiredArgumentException;
import com.poixson.logger.AttachedLogger;
import com.poixson.logger.xLog;
import com.poixson.logger.xLogRoot;
import com.poixson.threadpool.xThreadPool;
import com.poixson.threadpool.types.xThreadPool_Main;
import com.poixson.tools.AppProps;
import com.poixson.tools.HangCatcher;
import com.poixson.tools.Keeper;
import com.poixson.tools.xTime;
import com.poixson.tools.comparators.IntComparator;
import com.poixson.tools.remapped.xRunnable;
import com.poixson.utils.FileUtils;
import com.poixson.utils.ProcUtils;
import com.poixson.utils.StringUtils;
import com.poixson.utils.ThreadUtils;
import com.poixson.utils.Utils;


/*
 * Startup sequence
 *   10  prevent root        - xAppSteps_Tool
 *   50  load configs        - xAppSteps_Config
 *   70  lock file           - xAppSteps_LockFile
 *   80  display logo        - xAppSteps_Logo
 *   90  start console input - xAppSteps_Console
 *  100  sync clock          - xAppStandard
 *  200  startup time        - xAppStandard
 *
 * Shutdown sequence
 *  150  stop schedulers     - xAppSteps_Scheduler
 *  100  stop thread pools   - xAppStandard
 *   60  display uptime      - xAppStandard
 *   20  release lock file   - xAppSteps_LockFile
 *   30  stop console input  - xAppSteps_Console
 *   10  garbage collect     - xApp
 *   10  final garpage collect
 */
public abstract class xApp implements xStartable, AttachedLogger {

	private static final String ERR_ALREADY_STOPPING_EXCEPTION    = "Cannot start app, already stopping!";
	private static final String ERR_INVALID_STATE_EXCEPTION       = "Invalid startup/shutdown state!";
	private static final String ERR_INVALID_START_STATE_EXCEPTION = "Invalid state, cannot start: {}";
	private static final String ERR_INVALID_STOP_STATE_EXCEPTION  = "Invalid state, cannot shutdown: {}";

//TODO: use this?
//	// app instance
//	protected static final AtomicReference<xApp> instance =
//			new AtomicReference<xApp>(null);

	protected static final int STATE_OFF     = 0;
	protected static final int STATE_START   = 1;
	protected static final int STATE_STOP    = Integer.MIN_VALUE + 1;
	protected static final int STATE_RUNNING = Integer.MAX_VALUE;

	// startup/shutdown steps
	protected final AtomicInteger state = new AtomicInteger(0);
	protected final HashMap<Integer, List<xAppStepDAO>> currentSteps =
			new HashMap<Integer, List<xAppStepDAO>>();
	protected final Object runLock = new Object();
	protected volatile HangCatcher hangCatcher = null;

	// mvn properties
	protected final AppProps props;



	public xApp() {
		this._log = xLogRoot.get();
		this.props = new AppProps(this.getClass());
		// debug mode
		if (ProcUtils.isDebugWireEnabled()) {
			xVars.setDebug(true);
		}
		// search for .debug file
		if (Utils.notEmpty(xVars.SEARCH_DEBUG_FILES)) {
			final String result =
				FileUtils.SearchLocalFile(
					xVars.SEARCH_DEBUG_FILES,
					xVars.SEARCH_DEBUG_PARENTS
				);
			if (result != null)
				xVars.setDebug(true);
		}
		Keeper.add(this);
//TODO:
//		Failure.register(
//			new Runnable() {
//				@Override
//				public void run() {
//					xApp.this.fail();
//				}
//			}
//		);
//TODO:
//		// process command line arguments
//		final List<String> argsList = new LinkedList<String>();
//		argsList.addAll(Arrays.asList(args));
//		instance.processArgs(argsList);
//		instance.processDefaultArgs(argsList);
//		if (utils.notEmpty(argsList)) {
//			final StringBuilder str = new StringBuilder();
//			for (final String arg : argsList) {
//				if (utils.isEmpty(arg)) continue;
//				if (str.length() > 0)
//					str.append(" ");
//				str.append(arg);
//			}
//			if (str.length() > 0) {
//				xVars.getOriginalOut()
//					.println("Unknown arguments: "+str.toString());
//				System.exit(1);
//				return;
//			}
//		}
//		// handle command-line arguments
//		instance.displayStartupVars();
//		// main thread ended
//		Failure.fail("@|FG_RED Main process ended! (this shouldn't happen)|@");
//		System.exit(1);
	}



	// ------------------------------------------------------------------------------- //
	// start/stop app



	protected abstract Object[] getStepObjects(final StepType type);



	@Override
	public void start() {
		if (Failure.hasFailed()) return;
		// check state (should be 0 stopped)
		{
			final int stepInt = this.state.get();
			if (stepInt != STATE_OFF) {
				// <0 already stopping
				if (stepInt < STATE_OFF) {
					this.warning(
						ERR_ALREADY_STOPPING_EXCEPTION,
						stepInt
					);
				}
				// >0 already starting or running
				return;
			}
		}
		// set starting state
		if ( ! this.state.compareAndSet(STATE_OFF, STATE_START)) {
			this.warning(
				ERR_INVALID_START_STATE_EXCEPTION,
				this.state.get()
			);
			return;
		}
//TODO:
//		// register shutdown hook
//		xThreadPool.addShutdownHook(
//			new RemappedMethod(this, "stop")
//		);
		if (Failure.hasFailed()) return;
		this.publish();
		this.title(
			new String[] { "Starting {}.." },
			this.getTitle()
		);
		// start hang catcher
		this.startHangCatcher();
		// load startup steps
		{
			final xThreadPool_Main pool = xThreadPool_Main.get();
			pool.runTaskNow(
				new xRunnable("Load startup steps") {
					private volatile xThreadPool pool = null;
					public xRunnable init(final xThreadPool pool) {
						this.pool = pool;
						return this;
					}
					@Override
					public void run() {
						if (Failure.hasFailed()) return;
						final xApp app = xApp.this;
						// prepare startup steps
						synchronized (app.currentSteps) {
							app.currentSteps.clear();
							app.loadSteps(StepType.STARTUP);
						}
						if (Failure.hasFailed()) return;
						// queue startup sequence
						final int stepInt = xApp.this.state.get();
						xApp.QueueNextStep(xApp.this, this.pool, stepInt);
					}
				}.init(pool)
			);
		}
	}
	@Override
	public void stop() {
		// check state
		{
			final int stepInt = this.state.get();
			// <=0 already stopping or stopped
			if (stepInt <= STATE_OFF)
				return;
			// set stopping state
			if ( ! this.state.compareAndSet(stepInt, STATE_STOP) ) {
				this.warning(
					ERR_INVALID_STOP_STATE_EXCEPTION,
					this.state.get()
				);
				return;
			}
		}
		this.publish();
		this.title(
			new String[] { "Stopping {}.." },
			this.getTitle()
		);
		// start hang catcher
		this.startHangCatcher();
		// load shutdown steps
		{
			final xThreadPool_Main pool = xThreadPool_Main.get();
			pool.runTaskNow(
				new xRunnable("Load shutdown steps") {
					private volatile xThreadPool pool = null;
					public xRunnable init(final xThreadPool pool) {
						this.pool = pool;
						return this;
					}
					@Override
					public void run() {
						if (Failure.hasFailed()) return;
						final xApp app = xApp.this;
						// prepare shutdown steps
						synchronized (app.currentSteps) {
							app.currentSteps.clear();
							app.loadSteps(StepType.SHUTDOWN);
						}
						if (Failure.hasFailed()) return;
						// queue shutdown sequence
						final int stepInt = xApp.this.state.get();
						xApp.QueueNextStep(xApp.this, this.pool, stepInt);
					}
				}.init(pool)
			);
		}
	}



	public void join() {
		xThreadPool_Main.get()
			.joinWorkers();
	}



	// run next step
	@Override
	public void run() {
		if (Failure.hasFailed()) return;
		synchronized (this.runLock) {
			// finished startup/shutdown
			if (this.currentSteps.isEmpty()) {
				this.stopHangCatcher();
				final int stepInt = this.state.get();
				if (stepInt == STATE_START || stepInt == STATE_STOP) {
					this.state.set(STATE_OFF);
					this.log()
						.severe(
							"No {} steps found!",
							(stepInt > STATE_OFF ? "startup" : "shutdown")
						);
				} else
				if (stepInt > STATE_OFF) {
					this.state.set(STATE_RUNNING);
					this.info("{} is ready!", this.getTitle());
				} else {
					this.state.set(STATE_OFF);
					this.info("{} has finished stopping.", this.getTitle());
				}
				return;
			}
			// get current step
			final xAppStepDAO step = this.grabNextStepDAO();
			if (Failure.hasFailed()) return;
			final int stepInt = this.state.get();
			if (step != null) {
				if (this.log().isDetailLoggable()) {
					this.fine(
						"{} step {}.. {}",
						( stepInt > STATE_OFF ? "Startup" : "Shutdown" ),
						stepInt,
						step.title
					);
				}
				// run current step
				this.resetHangCatcher();
				step.run();
				if (Failure.hasFailed()) return;
				this.resetHangCatcher();
			}
			// queue next step
			final xThreadPool_Main pool = xThreadPool_Main.get();
			QueueNextStep(this, pool, stepInt);
		}
	}



	protected static void QueueNextStep(final xApp app,
			final xThreadPool pool, final int stepInt) {
		if (Failure.hasFailed()) return;
		final String taskName =
			StringUtils.ReplaceTags(
				"{}({})",
				( stepInt > STATE_OFF ? "Startup" : "Shutdown" ),
				stepInt
			);
		pool.runTaskLater(taskName, app);
	}
	protected xAppStepDAO grabNextStepDAO() {
		if (Failure.hasFailed()) return null;
		synchronized (this.currentSteps) {
			// is finished
			if (this.currentSteps.isEmpty())
				return null;
			final int stepInt = this.state.get();
			// check current step
			final List<xAppStepDAO> steps = this.currentSteps.get( Integer.valueOf(stepInt) );
			if (steps != null && !steps.isEmpty()) {
				// run next task in current step
				final xAppStepDAO nextStep;
				synchronized (this.currentSteps) {
					nextStep = steps.get(0);
					if (nextStep == null)
						throw new RuntimeException("Failed to get next startup step!");
					steps.remove(0);
				}
				return nextStep;
			}
			// find next step int
			this.currentSteps.remove( Integer.valueOf(stepInt) );
			if (this.currentSteps.isEmpty())
				return null;
			int nextStepInt;
			if (stepInt == STATE_OFF)         throw new IllegalStateException(ERR_INVALID_STATE_EXCEPTION);
			if (stepInt == Integer.MIN_VALUE) throw new IllegalStateException(ERR_INVALID_STATE_EXCEPTION);
			if (stepInt == Integer.MAX_VALUE) throw new IllegalStateException(ERR_INVALID_STATE_EXCEPTION);
			final Iterator<Integer> it = this.currentSteps.keySet().iterator();
			// startup
			if (stepInt > STATE_OFF) {
				nextStepInt = Integer.MAX_VALUE;
				while (it.hasNext()) {
					final int index = it.next().intValue();
					if (index < nextStepInt) {
						nextStepInt = index;
					}
				}
				// no steps left
				if (nextStepInt == Integer.MAX_VALUE)
					return null;
			// shutdown
			} else {
				nextStepInt = Integer.MIN_VALUE;
				while (it.hasNext()) {
					final int index = it.next().intValue();
					if (index > nextStepInt) {
						nextStepInt = index;
					}
				}
				// no steps left
				if (nextStepInt == Integer.MIN_VALUE)
					return null;
			}
			this.state.set(nextStepInt);
			return this.grabNextStepDAO();
		}
	}



	protected void loadSteps(final StepType type) {
		this.loadSteps(
			type,
			this.getStepObjects(type)
		);
	}
	protected void loadSteps(final StepType type, final Object[] containers) {
		this.loadSteps(type, this);
		for (final Object obj : containers) {
			this.loadSteps(type, obj);
		}
		// log loaded steps
		if (this.log().isDetailLoggable()) {
			final List<String> lines = new ArrayList<String>();
			lines.add("Found {} {} steps:");
			// list steps in order
			final IntComparator compare =
				new IntComparator(
					StepType.SHUTDOWN.equals(type)
				);
			final TreeSet<Integer> orderedValues =
				new TreeSet<Integer>( compare );
			orderedValues.addAll(
				this.currentSteps.keySet()
			);
			int count = 0;
			//ORDERED_LOOP:
			for (final Integer stepInt : orderedValues) {
				final List<xAppStepDAO> list = this.currentSteps.get(stepInt);
				if (Utils.isEmpty(list))
					continue;
				//LIST_LOOP:
				for (final xAppStepDAO dao : list) {
					count++;
					lines.add(
						(new StringBuilder())
							.append(
								StringUtils.PadFront(
									5,
									stepInt.toString(),
									' '
								)
							)
							.append(" - ")
							.append(dao.title)
							.toString()
					);
				} // end LIST_LOOP
			} // end ORDERED_LOOP
			this.log()
				.detail(
					lines.toArray(new String[0]),
					count,
					( StepType.STARTUP.equals(type) ? "Startup" : "Shutdown" )
				);
		} // end log steps
	}
	protected void loadSteps(final StepType type, final Object container) {
		if (type      == null) throw new RequiredArgumentException("type");
		if (container == null) throw new RequiredArgumentException("container");
		if (Failure.hasFailed()) return;
		synchronized (this.currentSteps) {
			// find annotations
			final Class<?> clss = container.getClass();
			if (clss == null) throw new RuntimeException("Failed to get app step container class!");
			final Method[] methods = clss.getMethods();
			if (Utils.isEmpty(methods)) throw new RuntimeException("Failed to get app methods!");
			for (final Method m : methods) {
				final xAppStep anno = m.getAnnotation(xAppStep.class);
				if (anno == null) continue;
				// found step method
				if (type.equals(anno.Type())) {
					final xAppStepDAO dao =
						new xAppStepDAO(
							this,
							container,
							m,
							anno
						);
					// add to existing list or new list
					this.currentSteps.computeIfAbsent(
						Integer.valueOf(dao.stepValue),
						key -> new ArrayList<xAppStepDAO>()
					).add(dao);
				}
			}
		}
	}



	// ------------------------------------------------------------------------------- //
	// hang catcher



	private void startHangCatcher() {
		if (ProcUtils.isDebugWireEnabled())
			return;
		final HangCatcher catcher =
			new HangCatcher(
				xTime.getNew("10s").getMS(),
				100L,
				new Runnable() {
					@Override
					public void run() {
//TODO: improve this
						xApp.this.publish(
							new String[] {
								"",
								" *********************** ",
								" *  Startup has hung!  * ",
								" *********************** ",
								""
							}
						);
						ThreadUtils.DisplayStillRunning();
						System.exit(1);
					}
				}
		);
		catcher.start();
		this.hangCatcher = catcher;
	}
	private void resetHangCatcher() {
		final HangCatcher catcher = this.hangCatcher;
		if (catcher != null) {
			catcher.resetTimeout();
		}
	}
	private void stopHangCatcher() {
		final HangCatcher catcher = this.hangCatcher;
		if (catcher != null) {
			catcher.stop();
		}
	}



	// ------------------------------------------------------------------------------- //
	// state



	@Override
	public boolean isRunning() {
		return (this.state.get() == STATE_RUNNING);
	}
	public boolean isStarting() {
		return (this.state.get() > STATE_OFF);
	}
	@Override
	public boolean isStopping() {
		return (this.state.get() < STATE_OFF);
	}
	public boolean isStopped() {
		return (this.state.get() == STATE_OFF);
	}



	// ------------------------------------------------------------------------------- //
	// config



	// mvn properties
	public String getName() {
		return this.props.name;
	}
	public String getTitle() {
		return this.props.title;
	}
	public String getFullTitle() {
		return this.props.titleFull;
	}
	public String getVersion() {
		return this.props.version;
	}
	public String getCommitHashFull() {
		return this.props.commitHashFull;
	}
	public String getCommitHashShort() {
		return this.props.commitHashShort;
	}
	public String getURL() {
		return this.props.url;
	}
	public String getOrgName() {
		return this.props.orgName;
	}
	public String getOrgURL() {
		return this.props.orgUrl;
	}
	public String getIssueName() {
		return this.props.issueName;
	}
	public String getIssueURL() {
		return this.props.issueUrl;
	}



	// ------------------------------------------------------------------------------- //
	// startup steps



	// ------------------------------------------------------------------------------- //
	// shutdown steps



	// garbage collect
	@xAppStep( Type=StepType.SHUTDOWN, Title="Garbage Collect", StepValue=10 )
	public void __SHUTDOWN_gc(final xApp app, final xLog log) {
		Keeper.remove(this);
		ThreadUtils.Sleep(50L);
		System.gc();
		xVars.getOriginalOut()
			.println();
		xVars.getOriginalOut()
			.flush();
//TODO: is this useful?
//		xScheduler.clearInstance();
//		if (xScheduler.hasLoaded()) {
//			this.warning("xScheduler hasn't fully unloaded!");
//		} else {
//			this.finest("xScheduler has been unloaded");
//		}
	}



//TODO: this will be replaced with ShutdownTask
	@xAppStep( Type=StepType.SHUTDOWN, Title="Exit", StepValue=2)
	public void __SHUTDOWN_exit() {
		final Thread stopThread =
			new Thread() {
				@Override
				public void run() {
					ThreadUtils.Sleep(250L);
					System.exit(0);
				}
			};
		stopThread.start();
	}



	// ------------------------------------------------------------------------------- //
	// logger



	private final xLog _log;
	@Override
	public xLog log() {
		return this._log;
	}



}
