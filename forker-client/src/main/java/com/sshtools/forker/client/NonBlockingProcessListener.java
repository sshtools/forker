package com.sshtools.forker.client;

import java.nio.ByteBuffer;

/**
 * Listener for events products by {@link NonBlockingProcess}.
 */
public interface NonBlockingProcessListener extends ForkerProcessListener {
	/**
	 * Invoked when an error occurs on one of the polling threads while
	 * handling a read or a write
	 * 
	 * @param exception exception
	 * @param process process 
	 * @param exiting exiting
	 */
	void onError(Exception exception, NonBlockingProcess process, boolean exiting);
	
	/**
	 * This method is invoked when your user code starts the process using {@link ForkerBuilder#start()}. 
	 *
	 * @param process process
	 */
	void onStart(NonBlockingProcess process);
	
	/**
	 * This method is invoked at some point after the process has actually started. 
	 *
	 * @param process process
	 */
	void onStarted(NonBlockingProcess process);
	
	/**
	 * This method is invoked when the process exits. This method is also
	 * invoked immediately in the case of a failure to launch the child process.
	 * <p>
	 * There are two special values, besides ordinary process exit codes, that
	 * may be passed to this method. A value of {@link Integer#MIN_VALUE}
	 * indicates some kind of launch failure. A value of
	 * {@link Integer#MAX_VALUE} indicates an unknown or expected failure mode.
	 *
	 * @param exitCode the exit code of the process, or a special value
	 *            indicating unexpected failures
	 * @param process process
	 */
	void onExit(int exitCode, NonBlockingProcess process);

	/**
	 * This method is invoked when there is stderr data to process or an the
	 * end-of-file (EOF) condition has been reached. In the case of EOF, the
	 * {@code closed} parameter will be {@code true}; this is your signal that
	 * EOF has been reached.
	 * <p>
	 * You do not own the {@link ByteBuffer} provided to you. You should not
	 * retain a reference to this buffer.
	 * <p>
	 * Upon returning from this method, if any bytes are left in the buffer
	 * (i.e., {@code buffer.hasRemaining()} returns {@code true}), then the
	 * buffer will be {@link ByteBuffer#compact() compacted} after returning.
	 * Any unused data will be kept at the start of the buffer and passed back
	 * to you as part of the next invocation of this method (which might be when
	 * EOF is reached and {@code closed} is {@code true}).
	 * <p>
	 * Users wishing to merge stderr into stdout should simply delegate this
	 * callback to {@link #onStdout(NonBlockingProcess, ByteBuffer, boolean)} when invoked, like so:
	 * 
	 * <pre>
	 *    public void onStderr(ByteBuffer buffer, closed) {
	 *       if (!closed) {
	 *          onStdout(buffer, closed);
	 *       }
	 *    }
	 * </pre>
	 * <p>
	 * Notice that an EOF check is performed. If you merge streams in this way,
	 * and you do not check for EOF here, then your
	 * {@link #onStdout(NonBlockingProcess, ByteBuffer, boolean)} method will be called twice for an
	 * EOF condition; once when the stdout stream closes, and once when the
	 * stderr stream closes. If you check for EOF as above, your
	 * {@link #onStdout(NonBlockingProcess, ByteBuffer, boolean)} method would only be called once
	 * (for the close of stdout).
	 * <p>
	 * Exceptions thrown out from your method will be ignored, but your method
	 * should handle all exceptions itself.
	 * @param process process
	 * @param buffer a {@link ByteBuffer} containing received stderr data
	 * @param closed {@code true} if EOF has been reached
	 */
	void onStderr(NonBlockingProcess process, ByteBuffer buffer, boolean closed);

	/**
	 * This method is invoked after you have expressed a desire to write to
	 * stdin by first calling {@link ForkerProcess#wantWrite()}. When this method is
	 * invoked, your code should write data to be sent to the stdin of the child
	 * process into the provided {@link ByteBuffer}. After writing data into the
	 * {@code buffer} your code <em>must</em> {@link ByteBuffer#flip() flip} the
	 * buffer before returning.
	 * <p>
	 * If not all of the data needed to be written will fit in the provided
	 * {@code buffer}, this method can return {@code true} to indicate a desire
	 * to write more data. If there is no more data to be written at the time
	 * this method is invoked, then {@code false} should be returned from this
	 * method. It is always possible to call {@link ForkerProcess#wantWrite()} later
	 * if data becomes available to be written.
	 * @param process process
	 * @param buffer a {@link ByteBuffer} into which your stdin-bound data
	 *            should be written
	 *
	 * @return true if you have more data to write immediately, false otherwise
	 */
	boolean onStdinReady(NonBlockingProcess process, ByteBuffer buffer);

	/**
	 * This method is invoked when there is stdout data to process or an the
	 * end-of-file (EOF) condition has been reached. In the case of EOF, the
	 * {@code closed} parameter will be {@code true}; this is your signal that
	 * EOF has been reached.
	 * <p>
	 * You do not own the {@link ByteBuffer} provided to you. You should not
	 * retain a reference to this buffer.
	 * <p>
	 * Upon returning from this method, if any bytes are left in the buffer
	 * (i.e., {@code buffer.hasRemaining()} returns {@code true}), then the
	 * buffer will be {@link ByteBuffer#compact() compacted} after returning.
	 * Any unused data will be kept at the start of the buffer and passed back
	 * to you as part of the next invocation of this method (which might be when
	 * EOF is reached and {@code closed} is {@code true}).
	 * <p>
	 * Exceptions thrown out from your method will be ignored, but your method
	 * should handle all exceptions itself.
	 * @param process process
	 * @param buffer a {@link ByteBuffer} containing received stdout data
	 * @param closed {@code true} if EOF has been reached
	 */
	void onStdout(NonBlockingProcess process, ByteBuffer buffer, boolean closed);
}
