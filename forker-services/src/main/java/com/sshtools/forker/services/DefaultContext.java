/**
 * Copyright © 2015 - 2021 SSHTOOLS Limited (support@sshtools.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
