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

import java.io.IOException;
import java.io.InputStream;
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
	 * {@link #getOutputStream()}, but with the optional to forcibly close even if
	 * there is data waiting. When not forcing, the stream will be closed when
	 * pending writes are done. Note, this only applies to non-blocking I/O modes.
	 *
	 * @param force
	 *            whether to wait for pending data to be written before closing (if
	 *            supported)
	 * @throws IOException on error
	 */
	public void closeStdin(boolean force) throws IOException {
		getOutputStream().close();
	}

	/**
	 * Returns <code>true</code> if there is data waiting to written to STDIN. This
	 * is only meaningful in non-blocking I/O modes, all other modes will just
	 * return <code>false</code>.
	 *
	 * @return if there any any pending write
	 */
	public boolean hasPendingWrites() {
		return false;
	}

	/**
	 * Only application when the I/O mode is {@link IO#NON_BLOCKING}, this indicates
	 * a desire to write to STDIN. When space is available the
	 * {@link NonBlockingProcessListener#onStdinReady(NonBlockingProcess, ByteBuffer)}
	 * will be called for you to perform the actual write.
	 * 
	 * An {@link IllegalStateException} will be thrown if the stream is already
	 * closed, and an {@link UnsupportedOperationException} if this is not a
	 * non-blocking I/O process.
	 */
	public void wantWrite() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Perform a direct write to STDIN of the process using a {@link ByteBuffer}. To
	 * use byte arrays with the traditional {@link OutputStream} instead, using
	 * {@link #getOutputStream()} and write to that. Note, if this is a non-blocking
	 * implementation, {@link IOException} will not be thrown on any error.
	 *
	 * @param buffer
	 *            the {@link ByteBuffer} to write to the STDIN stream of the process
	 * @throws IOException
	 *             on error
	 */
	public void writeStdin(ByteBuffer buffer) throws IOException {
		byte[] bytes = new byte[buffer.remaining()];
		buffer.get(bytes);
		getOutputStream().write(bytes);
		getOutputStream().flush();
	}

	/**
	 * Utility to read from an input stream (i.e. in a blocking fashion) and write
	 * the data to a processes stdin in a non-block fashinng (where supported).
	 * 
	 * @param in
	 *            input
	 * @param process
	 *            process to write to tock
	 * @throws IOException
	 *             on error
	 */
	public static void readToStdin(InputStream in, ForkerProcess process) throws IOException {
		readToStdin(in, process, 65536);
	}

	/**
	 * Utility to read from an input stream (i.e. in a blocking fashion) and write
	 * the data to a processes stdin in a non-block fashinng (where supported).
	 * 
	 * @param in
	 *            input
	 * @param process
	 *            process to write to to
	 * @param bufferSize
	 *            buffer size
	 * @throws IOException
	 *             on error
	 */
	public static void readToStdin(InputStream in, ForkerProcess process, int bufferSize) throws IOException {
		int r;
		byte[] buf = new byte[bufferSize];
		final OutputStream outputStream = process.getOutputStream();
		while ((r = in.read(buf)) != -1) {
			outputStream.write(buf, 0, r);
			outputStream.flush();
		}

	}

	/**
	 * Utility to write the contents of a byte buffer to an {@link OutputStream}.
	 * 
	 * @param out
	 *            output stream
	 * @param buffer
	 *            buffer
	 * @throws IOException
	 *             on error 
	 */
	public static void writeToOutputStream(OutputStream out, ByteBuffer buffer) throws IOException {
		byte[] bytes = new byte[buffer.remaining()];
		buffer.get(bytes);
		out.write(bytes);
		out.flush();

	}
}
