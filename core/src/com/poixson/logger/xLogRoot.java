package com.poixson.logger;

import java.util.concurrent.atomic.AtomicReference;

import com.poixson.app.xVars;
import com.poixson.logger.printers.xLogPrinter;
import com.poixson.logger.printers.xLogPrinter_stdio;
import com.poixson.tools.Keeper;
import com.poixson.tools.remapped.OutputStreamLineRemapper;
import com.poixson.utils.Utils;


public class xLogRoot extends xLog {

	public static final xLevel DEFAULT_LEVEL = xLevel.STATS;
	public static final boolean OVERRIDE_STDIO = true;

	// root logger
	private static final AtomicReference<xLogRoot> root =
			new AtomicReference<xLogRoot>(null);

	private final AtomicReference<xLogPrinter> defaultPrinter =
			new AtomicReference<xLogPrinter>(null);



	// get root logger
	public static xLogRoot Get() {
		// existing root logger
		{
			final xLogRoot log = root.get();
			if (log != null)
				return log;
		}
		// new root logger instance
		{
			final xLogRoot log = new xLogRoot();
			if ( ! root.compareAndSet(null, log) )
				return root.get();
			// init root logger
			{
				log.setLevel(DEFAULT_LEVEL);
				Keeper.add(log);
			}
			return log;
		}
	}
	public static xLog Get(final String logName) {
		return Get()
				.get(logName);
	}
	public static xLogRoot Peek() {
		return root.get();
	}



	protected xLogRoot() {
		super(null, null);
		// override stdio
		if (OVERRIDE_STDIO) {
			// be sure this gets inited first
			xVars.isDebug();
			// capture std-out
			System.setOut(
				OutputStreamLineRemapper.toPrintStream(
					new OutputStreamLineRemapper() {
						@Override
						public void line(final String line) {
							xLogRoot.get()
								.stdout(line);
						}
					}
				)
			);
			// capture std-err
			System.setErr(
				OutputStreamLineRemapper.toPrintStream(
					new OutputStreamLineRemapper() {
						@Override
						public void line(final String line) {
							xLogRoot.get()
								.stderr(line);
						}
					}
				)
			);
		}
	}



	// ------------------------------------------------------------------------------- //
	// config



	@Override
	public xLevel getLevel() {
		if (xVars.isDebug())
			return xLevel.DETAIL;
		final xLevel level = super.level.get();
		if (level != null)
			return level;
		return DEFAULT_LEVEL;
	}

	@Override
	public boolean isRoot() {
		return true;
	}



	// ------------------------------------------------------------------------------- //
	// printer handlers



	public xLogPrinter[] getPrinters() {
		final xLogPrinter[] printers =
			super.getPrinters();
		if (Utils.notEmpty(printers))
			return printers;
		return
			new xLogPrinter[] {
				getDefaultPrinter()
			};
	}
	public xLogPrinter getDefaultPrinter() {
		// existing instance
		{
			final xLogPrinter printer = this.defaultPrinter.get();
			if (printer != null)
				return printer;
		}
		{
			final xLogPrinter printer = this.newDefaultPrinter();
			if ( ! this.defaultPrinter.compareAndSet(null, printer) )
				return this.defaultPrinter.get();
			return printer;
		}
	}
	protected xLogPrinter newDefaultPrinter() {
		return new xLogPrinter_stdio();
	}



	public void clearScreen() {
		final xConsole console = xVars.getConsole();
		if (console != null) {
			console.doClearScreen();
		}
	}
	public void beep() {
		final xConsole console = xVars.getConsole();
		if (console != null) {
			console.doBeep();
		}
	}



}
