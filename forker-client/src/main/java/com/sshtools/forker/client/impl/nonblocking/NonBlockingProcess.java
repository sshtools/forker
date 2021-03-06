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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.sshtools.forker.client.AbstractOSProcess;
import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.client.ForkerProcess;
import com.sshtools.forker.client.NonBlockingProcessListener;

/**
 * Non-blocking implementation of {@link AbstractOSProcess}. Uses JNA.
 */
public abstract class NonBlockingProcess extends ForkerProcess {
	/**
	 * 
	 */
	public static int BUFFER_CAPACITY = 65536;
	protected volatile ForkerBuilder builder;
	protected NonBlockingProcessFactory factory;
	protected NonBlockingProcessListener listener = null;
	protected OutputStream legacyOut = null;
	protected AtomicInteger exitCode;
	protected CountDownLatch exitPending;
	protected ConcurrentLinkedQueue<ByteBuffer> pendingWrites;
	protected volatile boolean isRunning;
	private final Object lock = new Object();

	/**
	 * Constructor
	 * 
	 * @param builder builder
	 * @param factory factory
	 * @param listener listener
	 */
	public NonBlockingProcess(final ForkerBuilder builder, NonBlockingProcessFactory factory, NonBlockingProcessListener listener) {
		this.listener = listener;
		this.builder = builder;
		this.factory = factory;
		this.exitCode = new AtomicInteger();
		this.exitPending = new CountDownLatch(1);
	}

	@Override
	public int exitValue() {
		if (isRunning)
			throw new IllegalThreadStateException("Still running.");
		return exitCode.get();
	}

	@Override
	public boolean isAlive() {
		return isRunning;
	}

	@Override
	public int waitFor() throws InterruptedException {
		if (waitFor(0, TimeUnit.MILLISECONDS))
			return exitValue();
		else
			return Integer.MIN_VALUE;
	}

	@Override
	public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
		if (timeout == 0) {
			exitPending.await();
		} else if (!exitPending.await(timeout, unit)) {
			return false;
		}
		return true;
	}

	@Override
	public OutputStream getOutputStream() {
		synchronized (lock) {
			if (legacyOut == null) {
				legacyOut = new BufferedOutputStream(new OutputStream() {
					@Override
					public void write(int b) throws IOException {
						write(new byte[] { (byte) b });
					}

					@Override
					public void write(byte[] b) throws IOException {
						write(b, 0, b.length);
					}

					@Override
					public void write(byte[] b, int off, int len) throws IOException {
						ByteBuffer buf = factory.isAllocateDirect() ? ByteBuffer.allocate(len) : ByteBuffer.allocate(len);
						buf.put(b, off, len);
						buf.flip();
						writeStdin(buf);
					}

					@Override
					public void close() throws IOException {
						closeStdin(false);
					}
				});
			}
		}
		return legacyOut;
	}

	@Override
	public InputStream getInputStream() {
		throw new UnsupportedOperationException("This process is a non-blocking one. Please use NonBlockProcess.listen() instead.");
	}

	@Override
	public InputStream getErrorStream() {
		throw new UnsupportedOperationException("This process is a non-blocking one. Please use NonBlockProcess.listen() instead.");
	}

	/**
	 * Get the process ID.
	 * 
	 * @return process ID
	 */
	public abstract int getPID();

	protected abstract IEventProcessor<? extends NonBlockingProcess> createProcessor();

	protected void inializeBuffers() {
		pendingWrites = new ConcurrentLinkedQueue<ByteBuffer>();
	}

	protected static String[] mapToArray(Map<String, String> map) {
		String[] environment = new String[map.size()];
		int i = 0;
		for (Entry<String, String> entrySet : map.entrySet()) {
			environment[i++] = entrySet.getKey() + "=" + entrySet.getValue();
		}
		return environment;
	}
}