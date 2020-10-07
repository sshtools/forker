package com.sshtools.forker.services;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DefaultContext implements ServicesContext {
	private ScheduledExecutorService executor;
	{
		executor = Executors.newScheduledThreadPool(1);
	}

	@Override
	public void schedule(Runnable runnable, long initialDelay, long delay, TimeUnit units) {
		executor.scheduleAtFixedRate(runnable, initialDelay, delay, units);
	}

	@Override
	public void call(Callable<?> callable) {
		executor.schedule(callable, 0, TimeUnit.MILLISECONDS);
	}
}
