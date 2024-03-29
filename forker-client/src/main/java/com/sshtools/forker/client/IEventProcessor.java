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
package com.sshtools.forker.client;

import java.util.concurrent.CyclicBarrier;

import com.sshtools.forker.client.impl.nonblocking.NonBlockingBasePosixProcess;

/**
 * This class is internal.
 *
 * @author Brett Wooldridge
 *
 * @param <T> a subclass of {@link NonBlockingBasePosixProcess}
 */
public interface IEventProcessor<T extends NonBlockingProcess> extends Runnable {
	/**
	 * Check whether the processor instance is currently running, and if not set
	 * it to the running state.
	 *
	 * @return true if the processor was already running, false otherwise
	 */
	boolean checkAndSetRunning();

	/**
	 * Get the CyclicBarrier that this thread should join, along with the
	 * NuProcess start thread that is starting this processor. Used to cause the
	 * NonBlockingOsxProcess to wait until the processor is up and running before returning
	 * from start() to the user.
	 *
	 * @return the CyclicBarrier to join to ensure the processor is running
	 *         before registering processes with it
	 */
	CyclicBarrier getSpawnBarrier();

	/**
	 * Register a process for handling by the event processor.
	 *
	 * @param process the process to register
	 */
	void registerProcess(T process);

	/**
	 * Express that the client desires to write data into the STDIN stream as
	 * soon as possible.
	 *
	 * @param process the process that wants to write to STDIN
	 */
	void queueWrite(T process);

	/**
	 * Called by the event-loop to process asynchronous I/O events.
	 *
	 * @return true if events were processed, false if an idle timeout occurred
	 */
	boolean process();

	/**
	 * Cleanly shutdown the processors and cleanup all resources.
	 */
	void shutdown();
	
	/**
	 * Want write
	 * 
	 * @param process process
	 */
	void wantWrite(NonBlockingProcess process);
}
