package com.sshtools.forker.client.impl;

import java.io.IOException;

import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.client.ForkerProcess;
import com.sshtools.forker.client.ForkerProcessFactory;
import com.sshtools.forker.client.ForkerProcessListener;

/**
 * Creates {@link LocalProcess} as a fallback. Usually this factory comes last
 * in the list.
 */
public class LocalProcessFactory implements ForkerProcessFactory {

	@Override
	public ForkerProcess createProcess(ForkerBuilder builder, ForkerProcessListener listener) throws IOException {
		// Finally always fallback to a standard local process
		LocalProcess localProcess = new LocalProcess(builder);
		return localProcess;
	}

}
