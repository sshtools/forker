package com.sshtools.forker.daemon;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.sshtools.forker.daemon.Forker.Client;

/**
 * Handlers are responsible for handling requests from forker clients. The
 * handler implementation signals the client type that it is for using
 * {@link #getType()}, which is matched against the type code the client
 * supplies ({@link Client#getType()}).
 *
 */
public interface Handler {

	/**
	 * The clien type this handler is for.
	 * 
	 * @return type
	 */
	int getType();

	/**
	 * Handle the request
	 * 
	 * @param forker
	 *            forker
	 * @param din
	 *            data input stream
	 * @param dos
	 *            data output stream
	 * @throws IOException
	 *             on any error
	 */
	void handle(Forker forker, DataInputStream din, DataOutputStream dos) throws IOException;

	/**
	 * Stop the handler. Called when the daemon shuts down.
	 */
	void stop();
}
