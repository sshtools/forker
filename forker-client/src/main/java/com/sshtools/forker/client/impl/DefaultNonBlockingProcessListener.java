package com.sshtools.forker.client.impl;

import com.sshtools.forker.client.NonBlockingProcessListener;

/**
 * Default implementation of a {@link NonBlockingProcessListener}.
 */
public class DefaultNonBlockingProcessListener implements NonBlockingProcessListener {
	/**
	 * Constructor.
	 * 
	 * @param process must be a {@link NonBlockingProcess}
	 * @throws IllegalArgumentException if not a {@link NonBlockingProcess}.
	 */
	public DefaultNonBlockingProcessListener(Process process) {
		if (!(process instanceof NonBlockingProcess)) {
			throw new IllegalArgumentException(
					String.format("Provided process is not a %s. Did you create it with an I/O mode of Io.NON_BLOCKING",
							NonBlockingProcess.class));
		}
		NonBlockingProcess np = (NonBlockingProcess) process;
		np.addListener(this);
	}
}
