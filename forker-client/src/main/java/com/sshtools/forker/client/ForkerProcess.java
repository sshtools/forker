package com.sshtools.forker.client;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import com.sshtools.forker.client.impl.nonblocking.NonBlockingProcess;
import com.sshtools.forker.common.IO;

/**
 * All processes created by {@link ForkerBuilder} will extend this.
 */
public abstract class ForkerProcess extends Process {
	
	/**
	 * Close the STDIN pipe to the process. This is similar to using
	 * {@link OutputStream#close()} on the stream returned from
	 * {@link #getOutputStream()}, but with the optional to forcibly close even
	 * if there is data waiting. When not forcing, the stream will be closed
	 * when pending writes are done. Note, this only applies to non-blocking I/O
	 * modes.
	 *
	 * @param force whether to wait for pending data to be written before
	 *            closing (if supported)
	 * @throws IOException
	 */
	public void closeStdin(boolean force) throws IOException {
		getOutputStream().close();
	}

	/**
	 * Returns <code>true</code> if there is data waiting to written to STDIN.
	 * This is only meaningful in non-blocking I/O modes, all other modes
	 * will just return <code>false</code>.
	 *
	 * @return if there any any pending write
	 */
	public boolean hasPendingWrites() {
		return false;
	}
	
	/**
	 * Only application when the I/O mode is {@link IO#NON_BLOCKING}, this 
	 * indicates a desire to write to STDIN. When space is available 
	 * the {@link NonBlockingProcessListener#onStdinReady(NonBlockingProcess, ByteBuffer)} will
	 * be called for you to perform the actual write.
	 * 
	 * An {@link IllegalStateException} will be thrown if the stream is
	 * already closed, and an {@link UnsupportedOperationException} if
	 * this is not a non-blocking I/O process.
	 */
	public void wantWrite() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Perform a direct write to STDIN of the process using a
	 * {@link ByteBuffer}. To use byte arrays with the traditional
	 * {@link OutputStream} instead, using {@link #getOutputStream()} and write
	 * to that. Note, if this is a non-blocking implementation, {@link IOException}
	 * will not be thrown on any error.
	 *
	 * @param buffer the {@link ByteBuffer} to write to the STDIN stream of the
	 *            process
	 * @throws IOException on error
	 */
	public void writeStdin(ByteBuffer buffer) throws IOException {
		byte[] bytes = new byte[buffer.remaining()];
		buffer.get(bytes);
		getOutputStream().write(bytes);
		getOutputStream().flush();
	}
}
