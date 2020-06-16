package com.sshtools.forker.services;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public interface ServicesContext {
	void schedule(Runnable runnable, long initialDelay, long delay, TimeUnit units);

	<T> T call(Callable<T> callable);
}
