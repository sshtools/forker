package com.sshtools.forker.pipes;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import com.sshtools.forker.pipes.PipeFactory.Flag;
import com.sun.jna.platform.win32.Kernel32;

/**
 * The Class NamedPipeSocket.
 */
public class NamedPipeSocket extends Socket {

	final class DelegateOutputStream extends OutputStream {
		@Override
		public void write(byte[] bytes) throws IOException {
			file.write(bytes);
		}

		@Override
		public void write(byte[] bytes, int off, int len) throws IOException {
			file.write(bytes, off, len);
		}

		@Override
		public void write(int value) throws IOException {
			file.write(value);
		}
	}

	final class DelegateInputStream extends InputStream {
		@Override
		public int read() throws IOException {
			return file.read();
		}

		@Override
		public int read(byte[] bytes) throws IOException {
			return file.read(bytes);
		}

		@Override
		public int read(byte[] bytes, int off, int len) throws IOException {
			return file.read(bytes, off, len);
		}
	}

	private RandomAccessFile file;
	private InputStream in;
	private OutputStream out;
	private final String path;

	NamedPipeSocket(String path, Flag... flags) {
		this.path = path;
	}

	/**
	 * Close.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Override
	public void close() throws IOException {
		if (file != null) {
			try {
				file.close();
			}
			finally {
				file = null;
			}
		}
	}

	/**
	 * Connect.
	 *
	 * @param endpoint the endpoint
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Override
	public void connect(SocketAddress endpoint) throws IOException {
		connect(endpoint, 0);
	}

	/**
	 * Connect.
	 *
	 * @param endpoint the endpoint
	 * @param timeout the timeout
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Override
	public void connect(SocketAddress endpoint, int timeout) throws IOException {
		int usedTimeout = timeout == 0 ? 100 : timeout;
		long now = System.currentTimeMillis();
		do {
			try {
				file = new RandomAccessFile(path, "rw");
				break;
			} catch (FileNotFoundException fnfe) {
				try {
					Kernel32.INSTANCE.WaitNamedPipe(path, timeout);
					file = new RandomAccessFile(path, "rw");
				} catch (Throwable cle) {
					if (System.currentTimeMillis() - now > TimeUnit.MILLISECONDS.toNanos(usedTimeout)) {
						if (timeout == 0) {
							throw new FileNotFoundException("Timeout may be needed.");
						}
						throw fnfe;
					}
					try {
						TimeUnit.MILLISECONDS.sleep(5);
					} catch (InterruptedException ie) {
						throw new IOException("Interrupted.", ie);
					}
				}
			}
		} while (true);
		in = new DelegateInputStream();
		out = new DelegateOutputStream();
	}

	@Override
	public InputStream getInputStream() {
		return in;
	}

	@Override
	public OutputStream getOutputStream() {
		return out;
	}

	@Override
	public void setKeepAlive(boolean bool) {
	}

	@Override
	public void setReceiveBufferSize(int size) {
	}

	@Override
	public void setSendBufferSize(int size) {
	}

	@Override
	public void setSoLinger(boolean bool, int value) {
	}

	@Override
	public void setSoTimeout(int timeout) {
	}

	@Override
	public void setTcpNoDelay(boolean bool) {
	}

	@Override
	public void shutdownInput() {
	}

	@Override
	public void shutdownOutput() {
	}
}