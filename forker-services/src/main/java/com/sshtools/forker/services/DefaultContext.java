package com.sshtools.forker.services;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DefaultContext implements ServicesContext {
	private ScheduledExecutorService executor;
	{
		executor = Executors.newScheduledThreadPool(1);
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable runnable, long initialDelay, long delay, TimeUnit units) {
		return executor.scheduleAtFixedRate(runnable, initialDelay, delay, units);
	}

	@Override
	public void call(Callable<?> callable) {
		executor.schedule(callable, 0, TimeUnit.MILLISECONDS);
	}

	@Override
	public void close() throws IOException {
		executor.shutdown();
	}
}
