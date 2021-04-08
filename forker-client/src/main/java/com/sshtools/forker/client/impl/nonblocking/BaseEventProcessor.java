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

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sshtools.forker.client.impl.jna.posix.LibC;
import com.sun.jna.ptr.IntByReference;

/**
 * @author Brett Wooldridge
 * @param <T>
 */
public abstract class BaseEventProcessor<T extends NonBlockingBasePosixProcess> implements IEventProcessor<T> {
	/**
	* 
	*/
	private final int lingerIterations;
	protected Map<Integer, T> pidToProcessMap;
	protected Map<Integer, T> fildesToProcessMap;
	protected volatile boolean shutdown;
	protected NonBlockingProcessFactory factory;
	private CyclicBarrier startBarrier;
	private AtomicBoolean isRunning;
	static {
	}

	/**
	 * @param factory
	 */
	public BaseEventProcessor(NonBlockingProcessFactory factory) {
		this(factory, factory.getLingerIterations());
		this.factory = factory;
	}

	/**
	 * @param factory 
	 * @param lingerIterations
	 */
	public BaseEventProcessor(NonBlockingProcessFactory factory, int lingerIterations) {
		this.lingerIterations = lingerIterations;
		this.factory = factory;
		pidToProcessMap = new ConcurrentHashMap<Integer, T>();
		fildesToProcessMap = new ConcurrentHashMap<Integer, T>();
		isRunning = new AtomicBoolean();
	}

	/**
	 * The primary run loop of the event processor.
	 */
	@Override
	public void run() {
		try {
			startBarrier.await();
			int idleCount = 0;
			while (!isRunning.compareAndSet(idleCount > lingerIterations && pidToProcessMap.isEmpty(), false)) {
				idleCount = (!shutdown && process()) ? 0 : (idleCount + 1);
			}
		} catch (Exception e) {
			// TODO: how to handle this error?
			isRunning.set(false);
		}
	}

	/** {@inheritDoc} */
	@Override
	public CyclicBarrier getSpawnBarrier() {
		startBarrier = new CyclicBarrier(2);
		return startBarrier;
	}

	/** {@inheritDoc} */
	@Override
	public boolean checkAndSetRunning() {
		return isRunning.compareAndSet(false, true);
	}

	/** {@inheritDoc} */
	@Override
	public void shutdown() {
		shutdown = true;
		Collection<T> processes = pidToProcessMap.values();
		IntByReference exitCode = new IntByReference();
		for (T process : processes) {
			LibC.kill(process.getPID(), LibC.SIGTERM);
			process.onExit(Integer.MAX_VALUE - 1);
			LibC.waitpid(process.getPID(), exitCode, LibC.WNOHANG);
		}
	}

	/**
	 * Close the process's STDIN pipe.
	 *
	 * @param process the process whose STDIN pipe should be closed
	 */
	abstract void closeStdin(T process);
}
