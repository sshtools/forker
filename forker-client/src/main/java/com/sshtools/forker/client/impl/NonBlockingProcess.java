package com.sshtools.forker.client.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import com.sshtools.forker.client.AbstractOSProcess;
import com.sshtools.forker.client.ForkerBuilder;

/**
 * Non-blocking implementation of {@link AbstractOSProcess}. Uses JNA. 
 */
public class NonBlockingProcess extends AbstractOSProcess {
	
	private List<DefaultNonBlockingProcessListener> listeners = new LinkedList<DefaultNonBlockingProcessListener>();

	/**
	 * Constructor
	 * 
	 * @param builder
	 *            builder
	 * @throws IOException on any error
	 */
	public NonBlockingProcess(final ForkerBuilder builder) throws IOException {
		throw new UnsupportedOperationException("Non-blocking mode is not yet implemented.");
	}

	@Override
	public OutputStream getOutputStream() {
		throw new UnsupportedOperationException("This process is a non-blocking one. Please use NonBlockProcess.write() instead.");
	}

	@Override
	public InputStream getInputStream() {
		throw new UnsupportedOperationException("This process is a non-blocking one. Please use NonBlockProcess.listen() instead.");
	}

	@Override
	public InputStream getErrorStream() {
		throw new UnsupportedOperationException("This process is a non-blocking one. Please use NonBlockProcess.listen() instead.");
	}

	@Override
	public int waitFor() throws InterruptedException {
		// TODO
		throw new UnsupportedOperationException();
	}

	@Override
	public int exitValue() {
		// TODO
		throw new UnsupportedOperationException();
	}

	@Override
	public void destroy() {
		// TODO
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Add a {@link DefaultNonBlockingProcessListener} whose methods are called when various non-blocking events
	 * occur.
	 * 
	 * @param listener listener toi add
	 */
	public void addListener(DefaultNonBlockingProcessListener listener) {
		listeners.add(listener);
	}
	

}