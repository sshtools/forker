package com.sshtools.forker.client;

import java.io.IOException;

/**
 * Interface to be implemented by pluggable elements that handle actual process
 * creation. Each factory will be called in turn until one creates a process.
 * New factories are registered as a standard Java server (META-INF/services). *
 */
public interface ForkerProcessFactory {

	/**
	 * Create a new process. If this factory is not appropriate for the builder
	 * configuration it should return <code>null</code>. If an error occurs it
	 * should throw an exception.
	 * 
	 * @param builder builder
	 * @return process
	 * @throws IOException on any error
	 */
	AbstractForkerProcess createProcess(ForkerBuilder builder) throws IOException;
}
