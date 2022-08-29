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

import static java.util.concurrent.locks.LockSupport.parkNanos;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.client.NonBlockingProcess;
import com.sshtools.forker.client.NonBlockingProcessFactory;
import com.sshtools.forker.client.NonBlockingProcessListener;
import com.sshtools.forker.client.impl.jna.posix.LibC;
import com.sun.jna.Memory;
import com.sun.jna.Native;

/**
 * Base implementation of {@link NonBlockingProcess} for Posix systems.
 */
public abstract class NonBlockingBasePosixProcess extends NonBlockingProcess {
	protected static final Logger LOGGER = Logger.getLogger(NonBlockingBasePosixProcess.class.getCanonicalName());
	private static final ByteBuffer STDIN_CLOSED_PENDING_WRITE_TOMBSTONE = ByteBuffer.allocate(1);
	protected static int processorRoundRobin;
	@SuppressWarnings("unused")
	private int exitcode; // set from native code in JDK 7
	protected BaseEventProcessor<NonBlockingBasePosixProcess> myProcessor;
	protected volatile int pid;
	/**
	* 
	*/
	public final AtomicBoolean cleanlyExitedBeforeProcess;
	protected AtomicBoolean userWantsWrite;
	// ******* Input/Output Buffers
	private Memory outBufferMemory;
	private Memory errBufferMemory;
	private Memory inBufferMemory;
	protected ByteBuffer outBuffer;
	protected ByteBuffer errBuffer;
	protected ByteBuffer inBuffer;
	// ******* Stdin/Stdout/Stderr pipe handles
	protected ReferenceCountedFileDescriptor stdin;
	protected ReferenceCountedFileDescriptor stdout;
	protected ReferenceCountedFileDescriptor stderr;
	protected volatile int stdinWidow;
	protected volatile int stdoutWidow;
	protected volatile int stderrWidow;
	protected AtomicBoolean stdinClosing;
	protected boolean outClosed;
	protected boolean errClosed;

	protected NonBlockingBasePosixProcess(ForkerBuilder builder, NonBlockingProcessFactory factory,
			NonBlockingProcessListener listener) {
		super(builder, factory, listener);
		this.userWantsWrite = new AtomicBoolean();
		this.cleanlyExitedBeforeProcess = new AtomicBoolean();
		this.stdin = new ReferenceCountedFileDescriptor(-1);
		this.stdout = new ReferenceCountedFileDescriptor(-1);
		this.stderr = new ReferenceCountedFileDescriptor(-1);
		this.stdinClosing = new AtomicBoolean();
		this.outClosed = true;
		this.errClosed = true;
	}

	/**
	 * Check the launched process and return {@code true} if launch was
	 * successful, or {@code false} if there was an error in launch.
	 *
	 * @return {@code true} on success, {@code false} on failure
	 */
	protected boolean checkLaunch() {
		// Can be overridden by subclasses for post-launch checks
		return true;
	}

	@Override
	public void destroy() {
		if (isRunning) {
			checkReturnCode(LibC.kill(pid, LibC.SIGTERM), "Sending signal failed");
		}
	}

	@Override
	public Process destroyForcibly() {
		if (isRunning) {
			checkReturnCode(LibC.kill(pid, LibC.SIGKILL), "Sending signal failed");
		}
		return this;
	}

	@Override
	public void wantWrite() {
		try {
			int fd = stdin.acquire();
			if (fd != -1) {
				userWantsWrite.set(true);
				myProcessor.queueWrite(this);
			} else {
				throw new IllegalStateException("closeStdin() method has already been called.");
			}
		} finally {
			stdin.release();
		}
	}

	@Override
	public void closeStdin(boolean force) {
		if (force) {
			try {
				int fd = stdin.acquire();
				if (fd != -1) {
					if (myProcessor != null) {
						myProcessor.closeStdin(this);
					}
					stdin.close();
				}
			} finally {
				stdin.release();
			}
		} else {
			if (stdinClosing.compareAndSet(false, true)) {
				pendingWrites.add(STDIN_CLOSED_PENDING_WRITE_TOMBSTONE);
				myProcessor.queueWrite(this);
			} else {
				throw new IllegalStateException("closeStdin() method has already been called.");
			}
		}
	}

	@Override
	public void writeStdin(ByteBuffer buffer) {
		try {
			int fd = stdin.acquire();
			boolean closing = stdinClosing.get();
			if (fd != -1 && !closing) {
				pendingWrites.add(buffer);
				myProcessor.queueWrite(this);
			} else {
				throw new IllegalStateException("closeStdin() method has already been called.");
			}
		} finally {
			stdin.release();
		}
	}

	@Override
	public boolean hasPendingWrites() {
		return !pendingWrites.isEmpty();
	}

	@Override
	public int getPID() {
		return pid;
	}

	/**
	 * Get the standard input file handle.
	 * 
	 * @return standard input file handle
	 */
	public ReferenceCountedFileDescriptor getStdin() {
		return stdin;
	}

	/**
	 * Get the standard output file handle.
	 * 
	 * @return standard output file handle
	 */
	public ReferenceCountedFileDescriptor getStdout() {
		return stdout;
	}

	/**
	 * Get the standard error file handle.
	 * 
	 * @return standard error file handle
	 */
	public ReferenceCountedFileDescriptor getStderr() {
		return stderr;
	}

	/**
	 * Get if this was a soft exit.
	 * 
	 * @return soft exit
	 */
	public boolean isSoftExit() {
		return (factory.isSoftExitDetection() && outClosed && errClosed);
	}

	protected void onExit(int statusCode) {
		if (exitPending.getCount() == 0) {
			// TODO: handle SIGCHLD
			return;
		}
		try {
			closeStdin(true);
			stdout.close();
			stderr.close();
			isRunning = false;
			exitCode.set(statusCode);
			if (outBuffer != null && !outClosed) {
				outBuffer.flip();
				if (listener != null)
					listener.onStdout(this, outBuffer, true);
			}
			if (errBuffer != null && !errClosed) {
				errBuffer.flip();
				if (listener != null)
					if (builder.redirectErrorStream())
						listener.onStdout(this, errBuffer, true);
					else
						listener.onStderr(this, errBuffer, true);
			}
			if (statusCode != Integer.MAX_VALUE - 1) {
				if (listener != null)
					listener.onExit(statusCode, this);
			}
		} catch (Exception e) {
			// Don't let an exception thrown from the user's handler interrupt
			// us
			if (listener == null)
				LOGGER.log(Level.SEVERE, "Failed to handle exit gracefully.", e);
			else
				listener.onError(e, this, false);
		} finally {
			exitPending.countDown();
			// Once the last reference to the buffer is gone, Java will finalize
			// the buffer
			// and release the native memory we allocated in
			// initializeBuffers().
			outBufferMemory = null;
			errBufferMemory = null;
			inBufferMemory = null;
			outBuffer = null;
			errBuffer = null;
			inBuffer = null;
			Memory.purge();
		}
	}

	protected void readStdout(int availability, int fd) {
		if (outClosed || availability == 0) {
			return;
		}
		try {
			if (availability < 0) {
				outClosed = true;
				outBuffer.flip();
				if (listener != null)
					listener.onStdout(this, outBuffer, true);
				return;
			}
			int read = LibC.read(fd, outBuffer, Math.min(availability, outBuffer.remaining()));
			if (read == -1) {
				outClosed = true;
				throw new RuntimeException("Unexpected eof");
				// EOF?
			}
			outBuffer.limit(outBuffer.position() + read);
			outBuffer.position(0);
			if (listener != null)
				listener.onStdout(this, outBuffer, false);
			outBuffer.compact();
		} catch (Exception e) {
			// Don't let an exception thrown from the user's handler interrupt
			// us
			if (listener == null)
				LOGGER.log(Level.WARNING, "Exception thrown from lsitener.", e);
			else
				listener.onError(e, this, false);
		}
		if (!outBuffer.hasRemaining()) {
			// The caller's onStdout() callback must set the buffer's position
			// to indicate how many bytes were consumed, or else it will
			// eventually run out of capacity.
			throw new RuntimeException("stdout buffer has no bytes remaining");
		}
	}

	protected void readStderr(int availability, int fd) {
		if (errClosed || availability == 0) {
			return;
		}
		try {
			if (availability < 0) {
				errClosed = true;
				errBuffer.flip();
				if (listener != null)
					if (builder.redirectErrorStream())
						listener.onStdout(this, errBuffer, true);
					else
						listener.onStderr(this, errBuffer, true);
				return;
			}
			int read = LibC.read(fd, errBuffer, Math.min(availability, errBuffer.remaining()));
			if (read == -1) {
				// EOF?
				errClosed = true;
				throw new RuntimeException("Unexpected eof");
			}
			errBuffer.limit(errBuffer.position() + read);
			errBuffer.position(0);
			if (listener != null)
				if (builder.redirectErrorStream())
					listener.onStdout(this, errBuffer, false);
				else
					listener.onStderr(this, errBuffer, false);
			errBuffer.compact();
		} catch (Exception e) {
			// Don't let an exception thrown from the user's handler interrupt
			// us
			if (listener == null)
				LOGGER.log(Level.WARNING, "Exception thrown from handler", e);
			else
				listener.onError(e, this, false);
		}
		if (!errBuffer.hasRemaining()) {
			// The caller's onStderr() callback must set the buffer's position
			// to indicate how many bytes were consumed, or else it will
			// eventually run out of capacity.
			throw new RuntimeException("stderr buffer has no bytes remaining");
		}
	}

	protected boolean writeStdin(int availability, int fd) {
		if (availability <= 0 || fd == -1) {
			return false;
		}
		if (inBuffer.hasRemaining()) {
			int wrote;
			do {
				wrote = LibC.write(fd, inBuffer, Math.min(availability, inBuffer.remaining()));
				if (wrote < 0) {
					int errno = Native.getLastError();
					if (errno == 11
							/* EAGAIN on MacOS */ || errno == 35 /*
																	 * EAGAIN on
																	 * Linux
																	 */) {
						availability /= 4;
						continue;
					}
					// EOF?
					stdin.close();
					return false;
				}
			} while (wrote < 0);
			availability -= wrote;
			inBuffer.position(inBuffer.position() + wrote);
			if (inBuffer.hasRemaining()) {
				return true;
			}
		}
		if (!pendingWrites.isEmpty()) {
			inBuffer.clear();
			// copy the next buffer into our direct buffer (inBuffer)
			ByteBuffer byteBuffer = pendingWrites.peek();
			if (byteBuffer == STDIN_CLOSED_PENDING_WRITE_TOMBSTONE) {
				// We've written everything the user requested, and the user
				// wants to close stdin now.
				closeStdin(true);
				userWantsWrite.set(false);
				pendingWrites.clear();
				return false;
			} else if (byteBuffer != null && byteBuffer.remaining() > BUFFER_CAPACITY) {
				ByteBuffer slice = byteBuffer.slice();
				slice.limit(BUFFER_CAPACITY);
				inBuffer.put(slice);
				byteBuffer.position(byteBuffer.position() + BUFFER_CAPACITY);
			} else if (byteBuffer != null) {
				inBuffer.put(byteBuffer);
				pendingWrites.poll();
			}
			inBuffer.flip();
			// Recurse
			if (inBuffer.hasRemaining()) {
				if (availability <= 0) {
					// We can't write now, so we want to be called back again
					// once there is more availability.
					return true;
				}
				return writeStdin(availability, fd);
			}
		}
		if (!userWantsWrite.get()) {
			return false;
		}
		if (listener == null)
			return false;
		try {
			inBuffer.clear();
			boolean wantMore = listener.onStdinReady(this, inBuffer);
			userWantsWrite.set(wantMore);
			if (inBuffer.hasRemaining() && availability > 0) {
				// Recurse
				return writeStdin(availability, fd);
			} else {
				return true;
			}
		} catch (Exception e) {
			if (listener == null)
				LOGGER.log(Level.SEVERE, "Exception thrown handling writes to stdin.", e);
			else
				listener.onError(e, this, false);
			return false;
		}
	}

	// ************************************************************************
	// Private methods
	// ************************************************************************
	protected void afterStart() {
		final long testSleep = Integer.getInteger("nuprocess.test.afterStartSleep", 0);
		if (testSleep > 0) {
			parkNanos(testSleep);
		}
		isRunning = true;
		if(listener != null)
			listener.onStarted(this);
	}

	protected void initializeBuffers() {
		super.inializeBuffers();
		outClosed = false;
		errClosed = false;
		outBufferMemory = new Memory(BUFFER_CAPACITY);
		outBuffer = outBufferMemory.getByteBuffer(0, outBufferMemory.size()).order(ByteOrder.nativeOrder());
		errBufferMemory = new Memory(BUFFER_CAPACITY);
		errBuffer = errBufferMemory.getByteBuffer(0, outBufferMemory.size()).order(ByteOrder.nativeOrder());
		inBufferMemory = new Memory(BUFFER_CAPACITY);
		inBuffer = inBufferMemory.getByteBuffer(0, outBufferMemory.size()).order(ByteOrder.nativeOrder());
		// Ensure stdin initially has 0 bytes pending write. We'll
		// update this before invoking onStdinReady.
		inBuffer.limit(0);
	}

	@SuppressWarnings("unchecked")
	protected void registerProcess() {
		int mySlot;
		synchronized (factory.getProcessors(this)) {
			mySlot = processorRoundRobin;
			processorRoundRobin = (processorRoundRobin + 1) % factory.getProcessors(this).size();
		}
		myProcessor = (BaseEventProcessor<NonBlockingBasePosixProcess>) factory.getProcessors(this).get(mySlot);
		myProcessor.registerProcess(this);
		if (myProcessor.checkAndSetRunning()) {
			CyclicBarrier spawnBarrier = myProcessor.getSpawnBarrier();
			Thread t = new Thread(myProcessor, "ProcessQueue" + mySlot);
			t.setDaemon(true);
			t.start();
			try {
				spawnBarrier.await();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	protected int[] createPipes() {
		int rc;
		int[] in = new int[2];
		int[] out = new int[2];
		int[] err = new int[2];
		try {
			rc = LibC.pipe(in);
			checkReturnCode(rc, "Create stdin pipe() failed");
			rc = LibC.pipe(out);
			checkReturnCode(rc, "Create stdout pipe() failed");
			rc = LibC.pipe(err);
			checkReturnCode(rc, "Create stderr pipe() failed");
			setNonBlocking(in[1], out[0], err[0]);
			stdin = new ReferenceCountedFileDescriptor(in[1]);
			stdout = new ReferenceCountedFileDescriptor(out[0]);
			stderr = new ReferenceCountedFileDescriptor(err[0]);
			stdinWidow = in[0];
			stdoutWidow = out[1];
			stderrWidow = err[1];
			return new int[] { in[1], out[0], err[0] };
		} catch (RuntimeException e) {
			if (listener == null)
				LOGGER.log(Level.SEVERE, "Error creating pipes", e);
			else
				listener.onError(e, this, true);
			initFailureCleanup(in, out, err);
			throw e;
		}
	}

	protected void initFailureCleanup(int[] in, int[] out, int[] err) {
		Set<Integer> unique = new HashSet<Integer>();
		if (in != null) {
			unique.add(in[0]);
			unique.add(in[1]);
		}
		if (out != null) {
			unique.add(out[0]);
			unique.add(out[1]);
		}
		if (err != null) {
			unique.add(err[0]);
			unique.add(err[1]);
		}
		for (int fildes : unique) {
			if (fildes != 0) {
				LibC.close(fildes);
			}
		}
	}

	protected static void checkReturnCode(int rc, String failureMessage) {
		if (rc != 0) {
			throw new RuntimeException(failureMessage + ", return code: " + rc + ", last error: " + Native.getLastError());
		}
	}

	private void setNonBlocking(int in, int out, int err) {
		int rc = LibC.fcntl(in, LibC.F_SETFL, LibC.fcntl(in, LibC.F_GETFL) | LibC.O_NONBLOCK);
		checkReturnCode(rc, "fnctl on stdin handle failed");
		rc = LibC.fcntl(out, LibC.F_SETFL, LibC.fcntl(out, LibC.F_GETFL) | LibC.O_NONBLOCK);
		checkReturnCode(rc, "fnctl on stdout handle failed");
		rc = LibC.fcntl(err, LibC.F_SETFL, LibC.fcntl(err, LibC.F_GETFL) | LibC.O_NONBLOCK);
		checkReturnCode(rc, "fnctl on stderr handle failed");
	}
}
