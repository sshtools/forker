package com.sshtools.forker.client;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
	private PipedInputStream pipeIn;
	private PipedInputStream pipeErr;

	/**
	 * Constructor
	 * 
	 * @param builder  builder
	 * @param factory  factory
	 * @param listener listener
	 */
	public NonBlockingProcess(final ForkerBuilder builder, NonBlockingProcessFactory factory,
			NonBlockingProcessListener listener) {
		if (listener == null) {
			/*
			 * If there is no listener, the we create one so that getInputStream() / getErrorStream() can be
			 * used instead if wanted.
			 */
			pipeIn = new PipedInputStream();
			try {
				PipedOutputStream pipeOut = new PipedOutputStream(pipeIn);
				PipedOutputStream pipeErrOut = null;
				if(!builder.redirectErrorStream()) {
					pipeErr = new PipedInputStream();
					pipeErrOut = new PipedOutputStream(pipeErr);
				}
				final PipedOutputStream fPipeErrOut = pipeErrOut;
				this.listener = new NonBlockingProcessListener() {

					@Override
					public void onStdout(NonBlockingProcess process, ByteBuffer buffer, boolean closed) {
						if (!closed) {
							byte[] bytes = new byte[buffer.remaining()];
							/* Consume bytes from buffer (so position is updated) */
							buffer.get(bytes);
							try {
								pipeOut.write(bytes);
							} catch (IOException e) {
								throw new IllegalStateException(
										"Failed to write back to pipe for simulated non-blocking I/O.", e);
							}
						}
					}

					@Override
					public boolean onStdinReady(NonBlockingProcess process, ByteBuffer buffer) {
						return false;
					}

					@Override
					public void onStderr(NonBlockingProcess process, ByteBuffer buffer, boolean closed) {
						if(!closed) {
							if(builder.redirectErrorStream()) {
								onStdout(process, buffer, closed);
							}
							else {
								byte[] bytes = new byte[buffer.remaining()];
								/* Consume bytes from buffer (so position is updated) */
								buffer.get(bytes);
								try {
									fPipeErrOut.write(bytes);
								} catch (IOException e) {
									throw new IllegalStateException(
											"Failed to write back to pipe for simulated non-blocking I/O.", e);
								}
							}
						}
					}

					@Override
					public void onStarted(NonBlockingProcess process) {
					}

					@Override
					public void onStart(NonBlockingProcess process) {
					}

					@Override
					public void onExit(int exitCode, NonBlockingProcess process) {
						try {
							pipeOut.close();
						} catch (IOException e) {
						}
					}

					@Override
					public void onError(Exception exception, NonBlockingProcess process, boolean exiting) {
					}
				};
			} catch (IOException ioe) {
				throw new IllegalStateException("Joining of input pipe failed.", ioe);
			}
		} else
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
						ByteBuffer buf = factory.isAllocateDirect() ? ByteBuffer.allocate(len)
								: ByteBuffer.allocate(len);
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
		if (pipeIn == null)
			throw new UnsupportedOperationException(
					"This process is a non-blocking one. Please use NonBlockProcess.listen() instead.");
		else
			return pipeIn;
	}

	@Override
	public InputStream getErrorStream() {
		if (pipeErr == null)
			throw new UnsupportedOperationException(
					"This process is a non-blocking one. Please use NonBlockProcess.listen() instead.");
		else
			return pipeErr;
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