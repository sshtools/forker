package com.sshtools.forker.pipes;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A factory for creating pipes.
 */
public interface PipeFactory {

	/**
	 * Flags
	 */
	public class Flag {

		/**
		 * Use a virtual file (if supported). E.g. on unix this would be an 'abstract
		 * path'. If the underlying OS does not support it an
		 * {@link UnsupportedOperationException} will be thrown, unless
		 * {@link Flag#CONCRETE} is specified and that is supported.
		 */
		public final static Flag ABSTRACT = new Flag();

		/**
		 * Use a concrete file (if supported). E.g. on unix this would be an 'pathname'.
		 * If the underlying OS does not support it an
		 * {@link UnsupportedOperationException} will be thrown, unless
		 * {@link Flag#ABSTRACT} is specified and that is supported.
		 */
		public final static Flag CONCRETE = new Flag();

		/**
		 * Access a remote pipe (if supported). The name must be in the format
		 * <code>host:name</code> if this flag is set. If the underlying OS does not
		 * support it an {@link UnsupportedOperationException} will be thrown, unless
		 * {@link Flag#ABSTRACT} is specified and that is supported.
		 */
		public final static Flag REMOTE = new Flag();
	}

	/**
	 * Creates a new pipe.
	 *
	 * @param name  the name
	 * @param flags flags
	 * @return the socket
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	Socket createPipe(String name, Flag... flags) throws IOException;

	/**
	 * Creates a new pipe server.
	 *
	 * @param name  the name
	 * @param flags flags
	 * @return the server socket
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	ServerSocket createPipeServer(String name, Flag... flags) throws IOException;
}
