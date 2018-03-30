package com.poixson.logger.console;

import com.poixson.abstractions.xStartable;
import com.poixson.logger.commands.xCommandHandler;


public interface xConsole extends xStartable {


	public String getName();

	@Override
	public void start();
	@Override
	public void stop();

	@Override
	public void run();
	@Override
	public boolean isRunning();
	@Override
	public boolean isStopping();

	public Object getPrintLockObject();

	public void clear();
	public void clearLine();
	public void flush();

	public void println(final String line);
	public void println();

	public String getPrompt();
	public void setPrompt(final String prompt);
	public void drawPrompt();

	public Character getMask();
	public void setMask(final Character mask);

	public xCommandHandler getCommandHandler();


}