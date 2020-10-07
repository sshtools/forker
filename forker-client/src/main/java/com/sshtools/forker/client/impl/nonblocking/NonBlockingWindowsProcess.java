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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sshtools.forker.client.EffectiveUser;
import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.client.NonBlockingProcessListener;
import com.sshtools.forker.client.impl.jna.win32.Kernel32;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.WinBase.OVERLAPPED;
import com.sun.jna.platform.win32.WinBase.PROCESS_INFORMATION;
import com.sun.jna.platform.win32.WinBase.SECURITY_ATTRIBUTES;
import com.sun.jna.platform.win32.WinBase.STARTUPINFO;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;

/**
 * @author Brett Wooldridge
 */
public final class NonBlockingWindowsProcess extends NonBlockingProcess {
	protected static final Logger LOGGER = Logger.getLogger(NonBlockingBasePosixProcess.class.getCanonicalName());
	private static final int BUFFER_SIZE = 65536;
	private static int processorRoundRobin;
	private static final String namedPipePathPrefix;
	private static final AtomicInteger namedPipeCounter;
	private volatile IEventProcessor<NonBlockingWindowsProcess> myProcessor;
	AtomicBoolean userWantsWrite;
	private volatile boolean writePending;
	private AtomicBoolean stdinClosing;
	private volatile PipeBundle stdinPipe;
	private volatile PipeBundle stdoutPipe;
	private volatile PipeBundle stderrPipe;
	private HANDLE hStdinWidow;
	private HANDLE hStdoutWidow;
	private HANDLE hStderrWidow;
	private final ByteBuffer pendingWriteStdinClosedTombstone;
	private volatile boolean inClosed;
	private volatile boolean outClosed;
	private volatile boolean errClosed;
	private PROCESS_INFORMATION processInfo;
	static {
		namedPipePathPrefix = "\\\\.\\pipe\\Forker-" + UUID.randomUUID().toString() + "-";
		namedPipeCounter = new AtomicInteger(100);
	}

	/**
	 * @param builder
	 * @param factory
	 * @param listener
	 * @throws IOException
	 */
	public NonBlockingWindowsProcess(ForkerBuilder builder, NonBlockingProcessFactory factory, NonBlockingProcessListener listener)
			throws IOException {
		super(builder, factory, listener);
		this.userWantsWrite = new AtomicBoolean();
		this.outClosed = true;
		this.errClosed = true;
		this.inClosed = true;
		this.stdinClosing = new AtomicBoolean();
		this.pendingWriteStdinClosedTombstone = ByteBuffer.allocate(1);
		EffectiveUser effectiveUser = builder.effectiveUser();
		if (effectiveUser != null) {
			effectiveUser.elevate(builder, null, builder.getCommand());
		}
		try {
			try {
				createPipes();
				char[] block = getEnvironment(mapToArray(builder.environment()));
				Memory env = new Memory(block.length * 3);
				env.write(0, block, 0, block.length);
				STARTUPINFO startupInfo = new STARTUPINFO();
				startupInfo.clear();
				startupInfo.cb = new DWORD(startupInfo.size());
				startupInfo.hStdInput = hStdinWidow;
				startupInfo.hStdError = hStderrWidow;
				startupInfo.hStdOutput = hStdoutWidow;
				startupInfo.dwFlags = WinNT.STARTF_USESTDHANDLES;
				processInfo = new PROCESS_INFORMATION();
				DWORD dwCreationFlags = new DWORD(
						WinNT.CREATE_NO_WINDOW | WinNT.CREATE_UNICODE_ENVIRONMENT | WinNT.CREATE_SUSPENDED);
				char[] cwdChars = (builder.directory() != null)
						? Native.toCharArray(builder.directory().toPath().toAbsolutePath().toString())
						: null;
				if (!Kernel32.INSTANCE.CreateProcessW(null, getCommandLine(builder.command()),
						null /* lpProcessAttributes */,
						null /* lpThreadAttributes */,
						true /* bInheritHandles */, dwCreationFlags, env, cwdChars, startupInfo, processInfo)) {
					int lastError = Native.getLastError();
					throw new IOException("CreateProcessW() failed, error: " + lastError);
				}
				initializeBuffers();
				registerProcess();
				Kernel32.INSTANCE.ResumeThread(processInfo.hThread);
			} finally {
				Kernel32.INSTANCE.CloseHandle(hStdinWidow);
				Kernel32.INSTANCE.CloseHandle(hStdoutWidow);
				Kernel32.INSTANCE.CloseHandle(hStderrWidow);
			}
		} finally {
			if (effectiveUser != null) {
				effectiveUser.descend(builder, null, builder.getCommand());
			}
		}
	}

	@Override
	protected IEventProcessor<? extends NonBlockingProcess> createProcessor() {
		return new ProcessCompletions(factory);
	}

	@Override
	public void wantWrite() {
		if (hStdinWidow != null && !WinNT.INVALID_HANDLE_VALUE.getPointer().equals(hStdinWidow.getPointer())) {
			userWantsWrite.set(true);
			myProcessor.wantWrite(this);
		}
	}

	/** {@inheritDoc} */
	@Override
	public synchronized void writeStdin(ByteBuffer buffer) {
		if (hStdinWidow != null && !WinNT.INVALID_HANDLE_VALUE.getPointer().equals(hStdinWidow.getPointer())) {
			pendingWrites.add(buffer);
			if (!writePending) {
				myProcessor.wantWrite(this);
			}
		} else {
			throw new IllegalStateException("closeStdin() method has already been called.");
		}
	}

	/** {@inheritDoc} */
	@Override
	public void closeStdin(boolean force) {
		if (force) {
			stdinClose();
		} else {
			if (stdinClosing.compareAndSet(false, true)) {
				pendingWrites.add(pendingWriteStdinClosedTombstone);
				if (!writePending) {
					myProcessor.wantWrite(this);
				}
			} else {
				throw new IllegalStateException("closeStdin() method has already been called.");
			}
		}
	}

	/** {@inheritDoc} */
	@Override
	public boolean hasPendingWrites() {
		return !pendingWrites.isEmpty();
	}

	@Override
	public void destroy() {
		Kernel32.INSTANCE.TerminateProcess(processInfo.hProcess, Integer.MAX_VALUE);
	}

	@Override
	public int getPID() {
		// PointerByReference pointer = new PointerByReference();
		// return Kernel32.User32DLL.GetWindowThreadProcessId(null, null);
		return Kernel32.INSTANCE.GetProcessId(this.getPidHandle());
	}

	// ************************************************************************
	// Package-scoped methods
	// ************************************************************************
	HANDLE getPidHandle() {
		return processInfo.hProcess;
	}

	PipeBundle getStdinPipe() {
		return stdinPipe;
	}

	PipeBundle getStdoutPipe() {
		return stdoutPipe;
	}

	PipeBundle getStderrPipe() {
		return stderrPipe;
	}

	void readStdout(int transferred) {
		if (outClosed) {
			return;
		}
		try {
			if (transferred < 0) {
				outClosed = true;
				stdoutPipe.buffer.flip();
				if (listener != null)
					listener.onStdout(this, stdoutPipe.buffer, true);
				return;
			} else if (transferred == 0) {
				return;
			}
			final ByteBuffer buffer = stdoutPipe.buffer;
			buffer.limit(buffer.position() + transferred);
			buffer.position(0);
			if (listener != null)
				listener.onStdout(this, buffer, false);
			buffer.compact();
		} catch (Exception e) {
			if (listener == null)
				LOGGER.log(Level.WARNING, "Exception thrown from handler", e);
			else
				listener.onError(e, this, false);
		}
		if (!stdoutPipe.buffer.hasRemaining()) {
			// The caller's onStdout() callback must set the buffer's position
			// to indicate how many bytes were consumed, or else it will
			// eventually run out of capacity.
			throw new RuntimeException("stdout buffer has no bytes remaining");
		}
	}

	void readStderr(int transferred) {
		if (errClosed) {
			return;
		}
		try {
			if (transferred < 0) {
				errClosed = true;
				stderrPipe.buffer.flip();
				if (listener != null)
					if (builder.redirectErrorStream())
						listener.onStdout(this, stderrPipe.buffer, true);
					else
						listener.onStderr(this, stderrPipe.buffer, true);
				return;
			} else if (transferred == 0) {
				return;
			}
			final ByteBuffer buffer = stderrPipe.buffer;
			buffer.limit(buffer.position() + transferred);
			buffer.position(0);
			if (listener != null)
				if (builder.redirectErrorStream())
					listener.onStdout(this, buffer, false);
				else
					listener.onStderr(this, buffer, false);
			buffer.compact();
		} catch (Exception e) {
			if (listener == null)
				LOGGER.log(Level.WARNING, "Exception thrown from handler", e);
			else
				listener.onError(e, this, false);
		}
		if (!stderrPipe.buffer.hasRemaining()) {
			// The caller's onStdout() callback must set the buffer's position
			// to indicate how many bytes were consumed, or else it will
			// eventually run out of capacity.
			throw new RuntimeException("stderr buffer has no bytes remaining");
		}
	}

	boolean writeStdin(int transferred) {
		if (writePending && transferred == 0) {
			return false;
		}
		stdinPipe.buffer.position(stdinPipe.buffer.position() + transferred);
		if (stdinPipe.buffer.hasRemaining()) {
			Kernel32.INSTANCE.WriteFile(stdinPipe.pipeHandle, stdinPipe.buffer, stdinPipe.buffer.remaining(), null,
					stdinPipe.overlapped);
			writePending = true;
			return false;
		}
		writePending = false;
		if (!pendingWrites.isEmpty()) {
			stdinPipe.buffer.clear();
			// copy the next buffer into our direct buffer (inBuffer)
			ByteBuffer byteBuffer = pendingWrites.peek();
			if (byteBuffer == pendingWriteStdinClosedTombstone) {
				closeStdin(true);
				userWantsWrite.set(false);
				pendingWrites.clear();
				return false;
			} else if (byteBuffer.remaining() > BUFFER_CAPACITY) {
				ByteBuffer slice = byteBuffer.slice();
				slice.limit(BUFFER_CAPACITY);
				stdinPipe.buffer.put(slice);
				byteBuffer.position(byteBuffer.position() + BUFFER_CAPACITY);
			} else {
				stdinPipe.buffer.put(byteBuffer);
				pendingWrites.poll();
			}
			stdinPipe.buffer.flip();
			if (stdinPipe.buffer.hasRemaining()) {
				return true;
			}
		}
		if (userWantsWrite.compareAndSet(true, false)) {
			try {
				final ByteBuffer buffer = stdinPipe.buffer;
				buffer.clear();
				if (listener != null)
					userWantsWrite.set(listener.onStdinReady(this, buffer));
				return true;
			} catch (Exception e) {
				// Don't let an exception thrown from the user's handler
				// interrupt us
				e.printStackTrace();
				return false;
			}
		}
		return false;
	}

	void onExit(int statusCode) {
		if (exitPending.getCount() == 0) {
			return;
		}
		try {
			isRunning = false;
			exitCode.set(statusCode);
			if (stdoutPipe != null && stdoutPipe.buffer != null && !outClosed) {
				stdoutPipe.buffer.flip();
				if (listener != null) {
					listener.onStdout(this, stdoutPipe.buffer, true);
				}
			}
			if (stderrPipe != null && stderrPipe.buffer != null && !errClosed) {
				stderrPipe.buffer.flip();
				if (listener != null) {
					if (builder.redirectErrorStream())
						listener.onStdout(this, stderrPipe.buffer, true);
					else
						listener.onStderr(this, stderrPipe.buffer, true);
				}
			}
			if (statusCode != Integer.MAX_VALUE - 1) {
				if (listener != null)
					listener.onExit(statusCode, this);
			}
		} catch (Exception e) {
			if (listener == null)
				LOGGER.log(Level.SEVERE, "Failed to handle exit gracefully.", e);
			else
				listener.onError(e, this, true);
		} finally {
			exitPending.countDown();
			if (stdinPipe != null) {
				if (!inClosed) {
					Kernel32.INSTANCE.CloseHandle(stdinPipe.pipeHandle);
				}
				// Once the last reference to the buffer is gone, Java will
				// finalize the buffer
				// and release the native memory we allocated in
				// initializeBuffers().
				stdinPipe.buffer = null;
			}
			if (stdoutPipe != null) {
				Kernel32.INSTANCE.CloseHandle(stdoutPipe.pipeHandle);
				stdoutPipe.buffer = null;
			}
			if (stderrPipe != null) {
				Kernel32.INSTANCE.CloseHandle(stderrPipe.pipeHandle);
				stderrPipe.buffer = null;
			}
			if (processInfo != null) {
				Kernel32.INSTANCE.CloseHandle(processInfo.hThread);
				Kernel32.INSTANCE.CloseHandle(processInfo.hProcess);
			}
			stderrPipe = null;
			stdoutPipe = null;
			stdinPipe = null;
		}
	}

	boolean isSoftExit() {
		return (outClosed && errClosed && factory.isSoftExitDetection());
	}

	void stdinClose() {
		if (!inClosed && stdinPipe != null) {
			Kernel32.INSTANCE.CloseHandle(stdinPipe.pipeHandle);
		}
		inClosed = true;
	}

	private void createPipes() {
		SECURITY_ATTRIBUTES sattr = new SECURITY_ATTRIBUTES();
		sattr.dwLength = new DWORD(sattr.size());
		sattr.bInheritHandle = true;
		sattr.lpSecurityDescriptor = null;
		// ################ STDOUT PIPE ################
		long ioCompletionKey = namedPipeCounter.getAndIncrement();
		WString pipeName = new WString(namedPipePathPrefix + ioCompletionKey);
		hStdoutWidow = Kernel32.INSTANCE.CreateNamedPipeW(pipeName, Kernel32.PIPE_ACCESS_INBOUND,
				0 /* dwPipeMode */, 1 /* nMaxInstances */, BUFFER_SIZE, BUFFER_SIZE,
				0 /* nDefaultTimeOut */, sattr);
		checkHandleValidity(hStdoutWidow);
		HANDLE stdoutHandle = Kernel32.INSTANCE.CreateFile(pipeName, WinNT.GENERIC_READ, WinNT.FILE_SHARE_READ, null,
				WinNT.OPEN_EXISTING, WinNT.FILE_ATTRIBUTE_NORMAL | WinNT.FILE_FLAG_OVERLAPPED,
				null /* hTemplateFile */);
		checkHandleValidity(stdoutHandle);
		stdoutPipe = new PipeBundle(stdoutHandle, ioCompletionKey);
		checkPipeConnected(Kernel32.INSTANCE.ConnectNamedPipe(hStdoutWidow, null));
		// ################ STDERR PIPE ################
		ioCompletionKey = namedPipeCounter.getAndIncrement();
		pipeName = new WString(namedPipePathPrefix + ioCompletionKey);
		hStderrWidow = Kernel32.INSTANCE.CreateNamedPipeW(pipeName, Kernel32.PIPE_ACCESS_INBOUND,
				0 /* dwPipeMode */, 1 /* nMaxInstances */, BUFFER_SIZE, BUFFER_SIZE,
				0 /* nDefaultTimeOut */, sattr);
		checkHandleValidity(hStderrWidow);
		HANDLE stderrHandle = Kernel32.INSTANCE.CreateFile(pipeName, WinNT.GENERIC_READ, WinNT.FILE_SHARE_READ, null,
				WinNT.OPEN_EXISTING, WinNT.FILE_ATTRIBUTE_NORMAL | WinNT.FILE_FLAG_OVERLAPPED,
				null /* hTemplateFile */);
		checkHandleValidity(stderrHandle);
		stderrPipe = new PipeBundle(stderrHandle, ioCompletionKey);
		checkPipeConnected(Kernel32.INSTANCE.ConnectNamedPipe(hStderrWidow, null));
		// ################ STDIN PIPE ################
		ioCompletionKey = namedPipeCounter.getAndIncrement();
		pipeName = new WString(namedPipePathPrefix + ioCompletionKey);
		hStdinWidow = Kernel32.INSTANCE.CreateNamedPipeW(pipeName, Kernel32.PIPE_ACCESS_OUTBOUND,
				0 /* dwPipeMode */, 1 /* nMaxInstances */, BUFFER_SIZE, BUFFER_SIZE,
				0 /* nDefaultTimeOut */, sattr);
		checkHandleValidity(hStdinWidow);
		HANDLE stdinHandle = Kernel32.INSTANCE.CreateFile(pipeName, WinNT.GENERIC_WRITE, WinNT.FILE_SHARE_WRITE, null,
				WinNT.OPEN_EXISTING, WinNT.FILE_ATTRIBUTE_NORMAL | WinNT.FILE_FLAG_OVERLAPPED,
				null /* hTemplateFile */);
		checkHandleValidity(stdinHandle);
		stdinPipe = new PipeBundle(stdinHandle, ioCompletionKey);
		checkPipeConnected(Kernel32.INSTANCE.ConnectNamedPipe(hStdinWidow, null));
	}

	protected void initializeBuffers() {
		super.inializeBuffers();
		pendingWrites = new ConcurrentLinkedQueue<ByteBuffer>();
		outClosed = false;
		errClosed = false;
		inClosed = false;
		isRunning = true;
		stdoutPipe.buffer = ByteBuffer.allocateDirect(BUFFER_CAPACITY);
		stderrPipe.buffer = ByteBuffer.allocateDirect(BUFFER_CAPACITY);
		stdinPipe.buffer = ByteBuffer.allocateDirect(BUFFER_CAPACITY);
		// Ensure stdin initially has 0 bytes pending write. We'll
		// update this before invoking onStdinReady.
		stdinPipe.buffer.limit(0);
		if(listener != null)
			listener.onStarted(this);
	}

	@SuppressWarnings("unchecked")
	private void registerProcess() {
		int mySlot = 0;
		synchronized (factory.getProcessors(this)) {
			mySlot = processorRoundRobin;
			processorRoundRobin = (processorRoundRobin + 1) % factory.getProcessors(this).size();
		}
		myProcessor = (IEventProcessor<NonBlockingWindowsProcess>) factory.getProcessors(this).get(mySlot);
		myProcessor.registerProcess(this);
		if (myProcessor.checkAndSetRunning()) {
			CyclicBarrier spawnBarrier = myProcessor.getSpawnBarrier();
			Thread t = new Thread(myProcessor, "ProcessIoCompletion" + mySlot);
			t.setDaemon(true);
			t.start();
			try {
				spawnBarrier.await();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	private char[] getCommandLine(List<String> commands) {
		StringBuilder sb = new StringBuilder();
		boolean isFirstCommand = true;
		for (String command : commands) {
			if (isFirstCommand) {
				isFirstCommand = false;
			} else {
				// Prepend a space before the second and subsequent components
				// of the command line.
				sb.append(' ');
			}
			// It's OK to apply CreateProcess escaping to even the first item in
			// the commands
			// list (the path to execute). Since Windows paths cannot contain
			// double-quotes
			// (really!), the logic in WindowsCreateProcessEscape.quote() will
			// either do nothing
			// or simply add double-quotes around the path.
			WindowsCreateProcessEscape.quote(sb, command);
		}
		return Native.toCharArray(sb.toString());
	}

	private char[] getEnvironment(String[] environment) {
		Map<String, String> env = new HashMap<String, String>();
		final String SYSTEMROOT = "SystemRoot";
		String systemRootValue = System.getenv(SYSTEMROOT);
		if (systemRootValue != null) {
			env.put(SYSTEMROOT, systemRootValue);
		}
		for (String entry : environment) {
			int ndx = entry.indexOf('=');
			if (ndx != -1) {
				env.put(entry.substring(0, ndx), (ndx < entry.length() ? entry.substring(ndx + 1) : ""));
			}
		}
		return getEnvironmentBlock(env).toCharArray();
	}

	private String getEnvironmentBlock(Map<String, String> env) {
		// Sort by name using UPPERCASE collation
		List<Map.Entry<String, String>> list = new ArrayList<Map.Entry<String, String>>(env.entrySet());
		Collections.sort(list, new EntryComparator());
		StringBuilder sb = new StringBuilder(32 * env.size());
		for (Map.Entry<String, String> e : list) {
			sb.append(e.getKey()).append('=').append(e.getValue()).append('\u0000');
		}
		// Add final NUL termination
		sb.append('\u0000').append('\u0000');
		return sb.toString();
	}

	private void checkHandleValidity(HANDLE handle) {
		if (WinNT.INVALID_HANDLE_VALUE.getPointer().equals(handle)) {
			throw new RuntimeException("Unable to create pipe, error " + Native.getLastError());
		}
	}

	private void checkPipeConnected(boolean status) {
		int lastError;
		if (!status && ((lastError = Native.getLastError()) != WinNT.ERROR_PIPE_CONNECTED)) {
			throw new RuntimeException("Unable to connect pipe, error: " + lastError);
		}
	}

	private static final class NameComparator implements Comparator<String> {
		@Override
		public int compare(String s1, String s2) {
			int len1 = s1.length();
			int len2 = s2.length();
			for (int i = 0; i < Math.min(len1, len2); i++) {
				char c1 = s1.charAt(i);
				char c2 = s2.charAt(i);
				if (c1 != c2) {
					c1 = Character.toUpperCase(c1);
					c2 = Character.toUpperCase(c2);
					if (c1 != c2) {
						return c1 - c2;
					}
				}
			}
			return len1 - len2;
		}
	}

	private static final class EntryComparator implements Comparator<Map.Entry<String, String>> {
		static NameComparator nameComparator = new NameComparator();

		@Override
		public int compare(Map.Entry<String, String> e1, Map.Entry<String, String> e2) {
			return nameComparator.compare(e1.getKey(), e2.getKey());
		}
	}

	static final class PipeBundle {
		final OVERLAPPED overlapped;
		final long ioCompletionKey;
		final HANDLE pipeHandle;
		ByteBuffer buffer;
		boolean registered;

		PipeBundle(HANDLE pipeHandle, long ioCompletionKey) {
			this.pipeHandle = pipeHandle;
			this.ioCompletionKey = ioCompletionKey;
			this.overlapped = new OVERLAPPED();
		}
	}
}
