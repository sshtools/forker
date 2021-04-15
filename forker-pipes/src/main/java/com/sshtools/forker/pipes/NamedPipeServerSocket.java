package com.sshtools.forker.pipes;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sshtools.forker.common.CSystem;
import com.sshtools.forker.common.CSystem.SockAddr;
import com.sshtools.forker.common.XKernel32;
import com.sshtools.forker.pipes.PipeFactory.Flag;
import com.sun.jna.LastErrorException;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT.HANDLE;

/**
 * Unix domain server socket
 */
public class NamedPipeServerSocket extends ServerSocket {
	private final HANDLE hNamedPipe;
	private boolean closed;

	final int MAX_BUFFER_SIZE = 1024;

	NamedPipeServerSocket(String path, Flag... flags) throws IOException {
		hNamedPipe = assertValidHandle("CreateNamedPipe",
				XKernel32.INSTANCE.CreateNamedPipe(path, WinBase.PIPE_ACCESS_DUPLEX, // dwOpenMode
						WinBase.PIPE_TYPE_MESSAGE | WinBase.PIPE_READMODE_MESSAGE | WinBase.PIPE_WAIT, // dwPipeMode
						1, // nMaxInstances,
						MAX_BUFFER_SIZE, // nOutBufferSize,
						MAX_BUFFER_SIZE, // nInBufferSize,
						(int) TimeUnit.SECONDS.toMillis(30L), // nDefaultTimeOut,
						null // lpSecurityAttributes
				));
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
		// TODO
//		try {
//			CSystem.INSTANCE.bind(fd, address, address.size());
//		} catch (LastErrorException lee) {
//			throw new IOException("native socket() failed : " + Unix.formatError(lee));
//		}
//		try {
//			CSystem.INSTANCE.listen(fd, backlog);
//		} catch (LastErrorException lee) {
//			throw new IOException("native socket() failed : " + Unix.formatError(lee));
//		}
	}

	/**
	 * Accept.
	 *
	 * @return the socket
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Override
	public Socket accept() throws IOException {
//		int tfd;
//		try {
//			tfd = CSystem.INSTANCE.accept(fd, null, 0);
//		} catch (LastErrorException lee) {
//			throw new IOException("native socket() failed : " + Unix.formatError(lee));
//		}
//
//		return new UnixDomainSocket(tfd);
		throw new UnsupportedOperationException("TODO");
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
//		if (!lock.getAndSet(true)) {
//			try {
//				CSystem.INSTANCE.close(fd);
//			} catch (LastErrorException lee) {
//				throw new IOException("native close() failed : " + Unix.formatError(lee));
//			} finally {
//				closed = true;
//			}
//		}
	}

	/**
	 * Makes sure that the handle argument is not {@code null} or
	 * {@link WinBase#INVALID_HANDLE_VALUE}. If invalid handle detected, then it
	 * invokes {@link Kernel32#GetLastError()} in order to display the error code
	 * 
	 * @param message Message to display if bad handle
	 * @param handle  The {@link HANDLE} to test
	 * @return The same as the input handle if good handle - otherwise does not
	 *         return and throws an assertion error
	 */
	public static final HANDLE assertValidHandle(String message, HANDLE handle) {
		if ((handle == null) || WinBase.INVALID_HANDLE_VALUE.equals(handle)) {
			int hr = Kernel32.INSTANCE.GetLastError();
			if (hr == WinError.ERROR_SUCCESS) {
				throw new IllegalStateException(String.format("%s failed with unknown reason code", message));
			} else {
				throw new IllegalStateException(String.format("%s failed. Err %d - 0x%x", message, hr, hr));
			}
		}

		return handle;
	}
}