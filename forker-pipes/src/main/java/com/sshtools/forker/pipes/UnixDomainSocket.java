package com.sshtools.forker.pipes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sshtools.forker.common.CSystem;
import com.sshtools.forker.common.CSystem.SockAddr;
import com.sshtools.forker.pipes.PipeFactory.Flag;
import com.sun.jna.LastErrorException;
import com.sun.jna.Native;

/**
 * The Class UnixDomainSocket.
 */
public class UnixDomainSocket extends Socket {

	class UnixSocketInputStream extends InputStream {

		@Override
		public int read() throws IOException {
			byte[] b = new byte[1];
			int r = read(b);
			if (r > 0)
				return b[0] & 0xff;
			return -1;
		}

		@Override
		public int read(byte[] b) throws IOException {
			return read(b, 0, b.length);
		}

		@Override
		public int read(byte[] bytesEntry, int off, int len) throws IOException {
			try {
				if (off > 0) {
					int bytes = 0;
					int remainingLength = len;
					int size;
					byte[] data = new byte[(len < BUFFER_SIZE) ? len : BUFFER_SIZE];
					do {
						size = CSystem.INSTANCE.recv(fd, data,
								(remainingLength < BUFFER_SIZE) ? remainingLength : 10240, 0);
						if (size > 0) {
							System.arraycopy(data, 0, bytesEntry, off, size);
							bytes += size;
							off += size;
							remainingLength -= size;
						}
					} while ((remainingLength > 0) && (size > 0));
					return bytes == 0 ? -1 : bytes;
				} else {
					int recv = CSystem.INSTANCE.recv(fd, bytesEntry, len, 0);
					return recv == 0 ? -1 : recv;
				}
			} catch (LastErrorException lee) {
				throw new IOException(String.format("Failed to read. %s", Unix.formatError(lee)));
			}
		}
	}

	class UnixSocketOutputStream extends OutputStream {

		@Override
		public void write(byte[] bytes) throws IOException {
			write(bytes, 0, bytes.length);
		}

		@Override
		public void write(byte[] bytesEntry, int off, int len) throws IOException {
			try {
				int bytes;
				if (off > 0) {
					int size;
					int r = len;
					byte[] data = new byte[(len < BUFFER_SIZE) ? len : BUFFER_SIZE];
					do {
						size = (r < BUFFER_SIZE) ? r : BUFFER_SIZE;
						System.arraycopy(bytesEntry, off, data, 0, size);
						bytes = CSystem.INSTANCE.send(fd, data, size, 0);
						if (bytes > 0) {
							off += bytes;
							r -= bytes;
						}
					} while ((r > 0) && (bytes > 0));
				} else {
					bytes = CSystem.INSTANCE.send(fd, bytesEntry, len, 0);
				}

				if (bytes != len) {
					throw new IOException(String.format("Failed to write %d bytes", len));
				}
			} catch (LastErrorException lee) {
				throw new IOException(String.format("Failed to write. %s", Unix.formatError(lee)));
			}
		}

		@Override
		public void write(int value) throws IOException {
			write(new byte[] { (byte) value });
		}
	}

	static final int BUFFER_SIZE = 10240;

	private final SockAddr address;
	private final int fd;
	private final AtomicBoolean lock = new AtomicBoolean();
	private InputStream in;
	private boolean connected;

	private OutputStream out;

	UnixDomainSocket(int fd) throws IOException {
		this.fd = fd;
		address = null;
		lock.set(false);
		connected = true;
		in = new UnixSocketInputStream();
		out = new UnixSocketOutputStream();
	}

	UnixDomainSocket(String path, Flag... flags) throws IOException {
		address = new SockAddr(path);
		lock.set(false);
		try {
			fd = CSystem.INSTANCE.socket(CSystem.AF_UNIX, CSystem.SOCK_STREAM, CSystem.PROTOCOL);
		} catch (LastErrorException lee) {
			throw new IOException(String.format("Failed to open socket. %s", Unix.formatError(lee)));
		}
	}

	/**
	 * Close.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Override
	public void close() throws IOException {
		if (!lock.getAndSet(true)) {
			try {
				CSystem.INSTANCE.close(fd);
			} catch (LastErrorException lee) {
				throw new IOException("native close() failed : " + Unix.formatError(lee));
			}
			connected = false;
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
	 * @param timeout  the timeout
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Override
	public void connect(SocketAddress endpoint, int timeout) throws IOException {
		try {
			int ret = CSystem.INSTANCE.connect(fd, address, address.size());
			if (ret != 0) {
				throw new IOException(CSystem.INSTANCE.strerror(Native.getLastError()));
			}
			connected = true;
		} catch (LastErrorException lee) {
			throw new IOException(String.format("Failed to connect socket. %s", Unix.formatError(lee)));
		}
		in = new UnixSocketInputStream();
		out = new UnixSocketOutputStream();
	}

	/**
	 * Gets the input stream.
	 *
	 * @return the input stream
	 */
	@Override
	public InputStream getInputStream() {
		return in;
	}

	/**
	 * Gets the output stream.
	 *
	 * @return the output stream
	 */
	@Override
	public OutputStream getOutputStream() {
		return out;
	}

	/**
	 * Checks if is connected.
	 *
	 * @return true, if is connected
	 */
	@Override
	public boolean isConnected() {
		return connected;
	}

	/**
	 * Sets the keep alive.
	 *
	 * @param b the new keep alive
	 */
	@Override
	public void setKeepAlive(boolean b) {
	}

	/**
	 * Sets the receive buffer size.
	 *
	 * @param size the new receive buffer size
	 */
	@Override
	public void setReceiveBufferSize(int size) {
	}

	/**
	 * Sets the send buffer size.
	 *
	 * @param size the new send buffer size
	 */
	@Override
	public void setSendBufferSize(int size) {
	}

	/**
	 * Sets the so linger.
	 *
	 * @param b the b
	 * @param i the i
	 */
	@Override
	public void setSoLinger(boolean b, int i) {
	}

	/**
	 * Sets the so timeout.
	 *
	 * @param timeout the new so timeout
	 */
	@Override
	public void setSoTimeout(int timeout) {
	}

	/**
	 * Sets the tcp no delay.
	 *
	 * @param b the new tcp no delay
	 */
	@Override
	public void setTcpNoDelay(boolean b) {
	}

	/**
	 * Shutdown input.
	 */
	@Override
	public void shutdownInput() {
	}

	/**
	 * Shutdown output.
	 */
	@Override
	public void shutdownOutput() {
	}
}