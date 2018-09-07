/**
 * Copyright © 2015 - 2018 SSHTOOLS Limited (support@sshtools.com)
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sshtools.forker.client.impl.jna.win32.Kernel32;
import com.sshtools.forker.client.impl.nonblocking.NonBlockingWindowsProcess.PipeBundle;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.BaseTSD.ULONG_PTR;
import com.sun.jna.platform.win32.BaseTSD.ULONG_PTRByReference;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

/**
 */
public final class ProcessCompletions implements IEventProcessor<NonBlockingWindowsProcess> {
	private static final int DEADPOOL_POLL_INTERVAL;
	private static final int LINGER_ITERATIONS;
	private static final int STDOUT = 0;
	private static final int STDERR = 1;
	private HANDLE ioCompletionPort;
	private List<NonBlockingWindowsProcess> deadPool;
	private BlockingQueue<NonBlockingWindowsProcess> pendingPool;
	private BlockingQueue<NonBlockingWindowsProcess> wantsWrite;
	private Map<Long, NonBlockingWindowsProcess> completionKeyToProcessMap;
	private volatile CyclicBarrier startBarrier;
	private volatile boolean shutdown;
	private AtomicBoolean isRunning;
	private IntByReference numberOfBytes;
	private ULONG_PTRByReference completionKey;
	private PointerByReference lpOverlapped;
	private NonBlockingProcessFactory factory;
	static {
		int lingerTimeMs = Math.max(1000, Integer.getInteger("com.zaxxer.nuprocess.lingerTimeMs", 2500));
		DEADPOOL_POLL_INTERVAL = Math.min(lingerTimeMs,
				Math.max(100, Integer.getInteger("com.zaxxer.nuprocess.deadPoolPollMs", 250)));
		LINGER_ITERATIONS = lingerTimeMs / DEADPOOL_POLL_INTERVAL;
	}

	ProcessCompletions(NonBlockingProcessFactory factory) {
		this.factory = factory;
		completionKeyToProcessMap = new HashMap<Long, NonBlockingWindowsProcess>();
		wantsWrite = new ArrayBlockingQueue<NonBlockingWindowsProcess>(512);
		pendingPool = new LinkedBlockingQueue<NonBlockingWindowsProcess>();
		deadPool = new LinkedList<NonBlockingWindowsProcess>();
		isRunning = new AtomicBoolean();
		numberOfBytes = new IntByReference();
		completionKey = new ULONG_PTRByReference();
		lpOverlapped = new PointerByReference();
		initCompletionPort();
	}

	/**
	 * The primary run loop of the kqueue event processor.
	 */
	@Override
	public void run() {
		try {
			startBarrier.await();
			int idleCount = 0;
			while (!isRunning.compareAndSet(
					idleCount > LINGER_ITERATIONS && deadPool.isEmpty() && completionKeyToProcessMap.isEmpty(), false)) {
				idleCount = (!shutdown && process()) ? 0 : (idleCount + 1);
			}
		} catch (Exception e) {
			// TODO: how to handle this error?
			e.printStackTrace();
			isRunning.set(false);
		}
	}

	/**
	 * @return processed
	 */
	public boolean process() {
		try {
			if (!Kernel32.INSTANCE.GetQueuedCompletionStatus(ioCompletionPort, numberOfBytes, completionKey, lpOverlapped,
					DEADPOOL_POLL_INTERVAL) && lpOverlapped.getValue() == null) // timeout
			{
				checkWaitWrites();
				checkPendingPool();
				return false;
			}
			final long key = completionKey.getValue().longValue();
			// explicit wake up by us to process pending want writes and
			// registrations
			if (key == 0) {
				checkWaitWrites();
				checkPendingPool();
				return true;
			}
			final NonBlockingWindowsProcess process = completionKeyToProcessMap.get(key);
			if (process == null) {
				return true;
			}
			int transferred = numberOfBytes.getValue();
			if (process.getStdoutPipe() != null && process.getStdoutPipe().ioCompletionKey == key) {
				if (transferred > 0) {
					process.readStdout(transferred);
					queueRead(process, process.getStdoutPipe(), STDOUT);
				} else {
					process.readStdout(-1);
				}
			} else if (process.getStdinPipe() != null && process.getStdinPipe().ioCompletionKey == key) {
				if (process.writeStdin(transferred)) {
					queueWrite(process);
				}
			} else if (process.getStderrPipe() != null && process.getStderrPipe().ioCompletionKey == key) {
				if (transferred > 0) {
					process.readStderr(transferred);
					queueRead(process, process.getStderrPipe(), STDERR);
				} else {
					process.readStderr(-1);
				}
			}
			if (process.isSoftExit()) {
				cleanupProcess(process);
			}
			return true;
		} finally {
			checkDeadPool();
		}
	}

	@Override
	public void shutdown() {
		shutdown = true;
		Collection<NonBlockingWindowsProcess> processes = completionKeyToProcessMap.values();
		for (NonBlockingWindowsProcess process : processes) {
			Kernel32.INSTANCE.TerminateProcess(process.getPidHandle(), Integer.MAX_VALUE - 1);
			process.onExit(Integer.MAX_VALUE - 1);
		}
	}

	@Override
	public CyclicBarrier getSpawnBarrier() {
		startBarrier = new CyclicBarrier(2);
		return startBarrier;
	}

	@Override
	public boolean checkAndSetRunning() {
		return isRunning.compareAndSet(false, true);
	}

	/**
	 * @param process
	 */
	public void wantWrite(final NonBlockingProcess process) {
		try {
			wantsWrite.put((NonBlockingWindowsProcess) process);
			Kernel32.INSTANCE.PostQueuedCompletionStatus(ioCompletionPort, 0, new ULONG_PTR(0).toPointer(), null);
		} catch (InterruptedException e) {
			// ignore
		}
	}

	@Override
	public void registerProcess(final NonBlockingWindowsProcess process) {
		if (shutdown) {
			return;
		}
		try {
			pendingPool.put(process);
			Kernel32.INSTANCE.PostQueuedCompletionStatus(ioCompletionPort, 0, new ULONG_PTR(0).toPointer(), null);
		} catch (InterruptedException e) {
			// ignore
		}
	}

	@Override
	public void queueWrite(final NonBlockingWindowsProcess process) {
		if (shutdown) {
			return;
		}
		final PipeBundle stdinPipe = process.getStdinPipe();
		if (!stdinPipe.registered) {
			HANDLE completionPort = Kernel32.INSTANCE.CreateIoCompletionPort(stdinPipe.pipeHandle, ioCompletionPort,
					new ULONG_PTR(stdinPipe.ioCompletionKey).toPointer(), factory.getNumberOfIOThreads());
			if (!ioCompletionPort.equals(completionPort)) {
				throw new RuntimeException("CreateIoCompletionPort() failed, error code: " + Native.getLastError());
			}
			completionKeyToProcessMap.put(stdinPipe.ioCompletionKey, process);
			stdinPipe.registered = true;
		}
		if (Kernel32.INSTANCE.WriteFile(stdinPipe.pipeHandle, stdinPipe.buffer, 0, null, stdinPipe.overlapped) == 0
				&& Native.getLastError() != WinNT.ERROR_IO_PENDING) {
			process.stdinClose();
		}
	}

	private void queueRead(final NonBlockingWindowsProcess process, final NonBlockingWindowsProcess.PipeBundle pipe, final int stdX) {
		// The caller must call position() on the buffer to indicate how many
		// bytes
		// were read from stdout / stderr.
		if (!pipe.buffer.hasRemaining()) {
			throw new RuntimeException("stdout / stderr buffer has no bytes remaining");
		}
		if (Kernel32.INSTANCE.ReadFile(pipe.pipeHandle, pipe.buffer, pipe.buffer.remaining(), null, pipe.overlapped) == 0) {
			int lastError = Native.getLastError();
			switch (lastError) {
			case WinNT.ERROR_SUCCESS:
			case WinNT.ERROR_IO_PENDING:
				break;
			case WinNT.ERROR_BROKEN_PIPE:
			case WinNT.ERROR_PIPE_NOT_CONNECTED:
				if (stdX == STDOUT) {
					process.readStdout(-1 /* closed */);
				} else {
					process.readStderr(-1 /* closed */);
				}
				break;
			default:
				System.err.println("Some other error occurred reading the pipe: " + lastError);
				break;
			}
		}
	}

	private void checkPendingPool() {
		NonBlockingWindowsProcess process;
		while ((process = pendingPool.poll()) != null) {
			HANDLE completionPort1 = Kernel32.INSTANCE.CreateIoCompletionPort(process.getStdoutPipe().pipeHandle, ioCompletionPort,
					new ULONG_PTR(process.getStdoutPipe().ioCompletionKey).toPointer(), factory.getNumberOfIOThreads());
			if (!ioCompletionPort.equals(completionPort1)) {
				throw new RuntimeException("CreateIoCompletionPort() failed, error code: " + Native.getLastError());
			}
			HANDLE completionPort2 = Kernel32.INSTANCE.CreateIoCompletionPort(process.getStderrPipe().pipeHandle, ioCompletionPort,
					new ULONG_PTR(process.getStderrPipe().ioCompletionKey).toPointer(), factory.getNumberOfIOThreads());
			if (!ioCompletionPort.equals(completionPort2)) {
				throw new RuntimeException("CreateIoCompletionPort() failed, error code: " + Native.getLastError());
			}
			completionKeyToProcessMap.put(process.getStdoutPipe().ioCompletionKey, process);
			completionKeyToProcessMap.put(process.getStderrPipe().ioCompletionKey, process);
			queueRead(process, process.getStdoutPipe(), STDOUT);
			queueRead(process, process.getStderrPipe(), STDERR);
		}
	}

	private void checkWaitWrites() {
		NonBlockingWindowsProcess process;
		while ((process = wantsWrite.poll()) != null) {
			queueWrite(process);
		}
	}

	private void checkDeadPool() {
		if (deadPool.isEmpty()) {
			return;
		}
		IntByReference exitCode = new IntByReference();
		Iterator<NonBlockingWindowsProcess> iterator = deadPool.iterator();
		while (iterator.hasNext()) {
			NonBlockingWindowsProcess process = iterator.next();
			if (Kernel32.INSTANCE.GetExitCodeProcess(process.getPidHandle(), exitCode) && exitCode.getValue() != WinNT.STILL_ACTIVE) {
				iterator.remove();
				process.onExit(exitCode.getValue());
			}
		}
	}

	private void cleanupProcess(final NonBlockingWindowsProcess process) {
		completionKeyToProcessMap.remove(process.getStdinPipe().ioCompletionKey);
		completionKeyToProcessMap.remove(process.getStdoutPipe().ioCompletionKey);
		completionKeyToProcessMap.remove(process.getStderrPipe().ioCompletionKey);
		IntByReference exitCode = new IntByReference();
		if (Kernel32.INSTANCE.GetExitCodeProcess(process.getPidHandle(), exitCode) && exitCode.getValue() != WinNT.STILL_ACTIVE) {
			process.onExit(exitCode.getValue());
		} else {
			deadPool.add(process);
		}
	}

	private void initCompletionPort() {
		ioCompletionPort = Kernel32.INSTANCE.CreateIoCompletionPort(WinNT.INVALID_HANDLE_VALUE, null, new ULONG_PTR(0).toPointer(),
				factory.getNumberOfIOThreads());
		if (ioCompletionPort == null) {
			throw new RuntimeException("CreateIoCompletionPort() failed, error code: " + Native.getLastError());
		}
	}
}
