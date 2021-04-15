package com.sshtools.forker.pipes;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sshtools.forker.common.CSystem;
import com.sshtools.forker.common.CSystem.SockAddr;
import com.sshtools.forker.pipes.PipeFactory.Flag;
import com.sun.jna.LastErrorException;
import com.sun.jna.Platform;

/**
 * Unix domain server socket
 */
public class UnixDomainServerSocket extends ServerSocket {
	private final AtomicBoolean lock = new AtomicBoolean();
	private final int fd;
	private final SockAddr address;
	private boolean closed;

	UnixDomainServerSocket(String path, Flag... flags) throws IOException {
		if (Platform.isWindows() || Platform.isWindowsCE()) {
			throw new IOException("Unix domain sockets are not supported on Windows");
		}
		File socketFile = new File(path);
		socketFile.delete();
		address = new SockAddr(path);
		socketFile.deleteOnExit();
		lock.set(false);
		try {
			fd = CSystem.INSTANCE.socket(CSystem.AF_UNIX, CSystem.SOCK_STREAM, CSystem.PROTOCOL);
		} catch (LastErrorException lee) {
			throw new IOException("native socket() failed : " + Unix.formatError(lee));
		}
	}

	/**
	 * Bind.
	 *
	 * @param endpoint the endpoint
	 * @param backlog  the backlog
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Override
	public void bind(SocketAddress endpoint, int backlog) throws IOException {
		try {
			CSystem.INSTANCE.bind(fd, address, address.size());
		} catch (LastErrorException lee) {
			throw new IOException("native socket() failed : " + Unix.formatError(lee));
		}
		try {
			CSystem.INSTANCE.listen(fd, backlog);
		} catch (LastErrorException lee) {
			throw new IOException("native socket() failed : " + Unix.formatError(lee));
		}
	}

	/**
	 * Accept.
	 *
	 * @return the socket
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Override
	public Socket accept() throws IOException {
		int tfd;
		try {
			tfd = CSystem.INSTANCE.accept(fd, null, 0);
		} catch (LastErrorException lee) {
			throw new IOException("native socket() failed : " + Unix.formatError(lee));
		}

		return new UnixDomainSocket(tfd);
	}

	/**
	 * Checks if is closed.
	 *
	 * @return true, if is closed
	 */
	@Override
	public boolean isClosed() {
		return closed;
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
			} finally {
				closed = true;
			}
		}
	}
}