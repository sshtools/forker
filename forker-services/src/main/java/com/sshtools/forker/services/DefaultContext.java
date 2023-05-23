package com.sshtools.forker.services;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DefaultContext implements ServicesContext {
    final static Logger LOG = Logger.getLogger(DefaultContext.class.getName());
    
	private ScheduledExecutorService executor;
	{
		executor = Executors.newScheduledThreadPool(1);
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable runnable, long initialDelay, long delay, TimeUnit units) {
		return executor.scheduleWithFixedDelay(runnable, initialDelay, delay, units);
	}

	@Override
	public void call(Callable<?> callable) {
		executor.execute(() -> {
            try {
                callable.call();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Scheduled task failed.", e);
            }
        });
	}

	@Override
	public void close() throws IOException {
		executor.shutdown();
	}
}
