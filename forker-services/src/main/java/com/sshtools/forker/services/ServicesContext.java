package com.sshtools.forker.services;

import java.io.Closeable;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public interface ServicesContext extends Closeable {
	ScheduledFuture<?> schedule(Runnable runnable, long initialDelay, long delay, TimeUnit units);

	void call(Callable<?> callable);
}
