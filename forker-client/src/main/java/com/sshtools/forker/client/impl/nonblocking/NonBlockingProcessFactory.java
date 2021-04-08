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
package com.sshtools.forker.client.impl.nonblocking;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.SystemUtils;

import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.client.ForkerProcess;
import com.sshtools.forker.client.ForkerProcessFactory;
import com.sshtools.forker.client.ForkerProcessListener;
import com.sshtools.forker.client.NonBlockingProcessListener;
import com.sshtools.forker.common.IO;

/**
 * Creates {@link NonBlockingProcess}.
 */
public class NonBlockingProcessFactory implements ForkerProcessFactory {
	private int numberOfIOThreads;
	private boolean shutdownHook = true;
	private boolean softExitDetection = true;
	private boolean firstRun;
	private Object lock = new Object();
	private boolean allocateDirect;
	private int lingerTimeMs = 2500;
	private int deadPoolPollMs = 250;
	private List<IEventProcessor<? extends NonBlockingProcess>> processors = null;
	{
		numberOfIOThreads = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
	}

	/**
	 * Get linger time in milliseconds
	 * 
	 * @return linger ms
	 */
	public int getLingerTimeMs() {
		return lingerTimeMs;
	}

	/**
	 * Set linger time in ms
	 * 
	 * @param lingerTimeMs linger ms
	 */
	public void setLingerTimeMs(int lingerTimeMs) {
		this.lingerTimeMs = lingerTimeMs;
	}

	/**
	 * Get dead pool poll milliseconds
	 * 
	 * @return dead pool poll milliseconds
	 */
	public int getDeadPoolPollMs() {
		return deadPoolPollMs;
	}

	/**
	 * Set dead pool poll milliseconds
	 * 
	 * @param deadPoolPollMs dead pool poll milliseconds
	 */
	public void setDeadPoolPollMs(int deadPoolPollMs) {
		this.deadPoolPollMs = deadPoolPollMs;
	}

	/**
	 * Get whether to use {@link ByteBuffer#allocateDirect(int)} where possible
	 * when creating internal byte buffers.
	 * 
	 * @return allocate direct
	 */
	public boolean isAllocateDirect() {
		return allocateDirect;
	}

	/**
	 * Set whether to use {@link ByteBuffer#allocateDirect(int)} where possible
	 * when creating internal byte buffers.
	 * 
	 * @param allocateDirect allocate direct
	 */
	public void setAllocateDirect(boolean allocateDirect) {
		this.allocateDirect = allocateDirect;
	}

	/**
	 * Get the processors.
	 * 
	 * @param process
	 * @return processors
	 */
	public List<IEventProcessor<? extends NonBlockingProcess>> getProcessors(NonBlockingProcess process) {
		synchronized (lock) {
			if (processors == null) {
				processors = new ArrayList<IEventProcessor<? extends NonBlockingProcess>>(numberOfIOThreads);
				for (int i = 0; i < numberOfIOThreads; i++)
					processors.add(process.createProcessor());
			}
			return processors;
		}
	}

	/**
	 * Get whether the shutdown hook will run when the JVM exits. This closes
	 * down any current process handlers and is on by default.
	 * 
	 * @return shutdown hook
	 */
	public boolean isShutdownHook() {
		return shutdownHook;
	}

	/**
	 * Set whether the shutdown hook will run when the JVM exits. This closes
	 * down any current process handlers and is on by default. This must be used
	 * before the first process is created or an exception will be thrown.
	 * 
	 * 
	 * @param shutdownHook
	 * @throws IllegalStateException if first process has already been created.
	 */
	public void setShutdownHook(boolean shutdownHook) {
		if (firstRun)
			throw new IllegalStateException("First process has been created, cannot change whether shutdown hook will be used.");
		synchronized (lock) {
			this.shutdownHook = shutdownHook;
		}
	}

	/**
	 * Get the number of I/O threads to use. By default, this will use half of
	 * the number of cores available.
	 * 
	 * @return number of IO threads
	 */
	public int getNumberOfIOThreads() {
		return numberOfIOThreads;
	}

	/**
	 * Set the number of I/O threads to use. Cannot be used after the first I/O
	 * thread has been started, i.e. after the first process is created.
	 * 
	 * @param numberOfIOThreads number of I/O threads
	 */
	public synchronized void setNumberOfIOThreads(int numberOfIOThreads) {
		if (firstRun)
			throw new IllegalStateException("Cannot set number of threads after processors have been created.");
		synchronized (lock) {
			this.numberOfIOThreads = numberOfIOThreads;
		}
	}

	/**
	 * Get if to use to use soft exit detection.
	 * 
	 * @return soft exit detection
	 */
	public boolean isSoftExitDetection() {
		return softExitDetection;
	}

	/**
	 * Set if to use to use soft exit detection.
	 * 
	 * @param softExitDetection soft exit detection
	 */
	public void setSoftExitDetection(boolean softExitDetection) {
		this.softExitDetection = softExitDetection;
	}

	@Override
	public ForkerProcess createProcess(ForkerBuilder builder, ForkerProcessListener listener) throws IOException {
		synchronized (lock) {
			if (!firstRun) {
				firstRun = true;
				if (shutdownHook) {
					Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
						@Override
						public void run() {
							if (processors != null) {
								for (IEventProcessor<? extends NonBlockingProcess> processor : processors) {
									if (processor != null) {
										processor.shutdown();
									}
								}
							}
						}
					}));
				}
			}
		}
		NonBlockingProcess process;
		try {
			if (SystemUtils.IS_OS_LINUX && (builder.io() == IO.NON_BLOCKING))
				process = new NonBlockingLinuxProcess(builder, this, (NonBlockingProcessListener) listener);
			else if (SystemUtils.IS_OS_MAC_OSX && (builder.io() == IO.NON_BLOCKING))
				process = new NonBlockingOsxProcess(builder, this, (NonBlockingProcessListener) listener);
			else if (SystemUtils.IS_OS_WINDOWS && (builder.io() == IO.NON_BLOCKING))
				process = new NonBlockingWindowsProcess(builder, this, (NonBlockingProcessListener) listener);
			else
				return null;
			if (listener != null) {
				((NonBlockingProcessListener) listener).onStart(process);
			}
		} catch (ClassCastException cce) {
			throw new IllegalArgumentException(
					String.format("For a %s, the listener supplied must be a %s.", getClass(), NonBlockingProcess.class), cce);
		}
		return process;
	}

	/**
	 * Get linger iterations
	 * 
	 * @return linger iterations
	 */
	public int getLingerIterations() {
		return (int) (lingerTimeMs / deadPoolPollMs);
	}
}
