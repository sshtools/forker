package com.sshtools.forker.client.impl;

import java.io.IOException;

import com.sshtools.forker.client.AbstractForkerProcess;
import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.client.ForkerProcessFactory;

/**
 * Creates {@link LocalProcess} as a fallback. Usually this factory comes last
 * in the list.
 */
public class LocalProcessFactory implements ForkerProcessFactory {

	@Override
	public AbstractForkerProcess createProcess(ForkerBuilder builder) throws IOException {
		// Finally always fallback to a standard local process
		LocalProcess localProcess = new LocalProcess(builder);
		return localProcess;
	}

}
