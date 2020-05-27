package com.poixson.tools.abstractions;


public interface xStartable extends Runnable {


	public void start();
	public void stop();

	@Override
	public void run();

	public boolean isRunning();
	public boolean isStopping();


}
