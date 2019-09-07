package com.sshtools.forker.client.impl;

import java.io.IOException;

import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.client.ForkerProcess;
import com.sshtools.forker.client.ForkerProcessFactory;
import com.sshtools.forker.client.ForkerProcessListener;
import com.sshtools.forker.client.NonBlockingProcessListener;
import com.sshtools.forker.common.IO;

/**
 * Create a {@link LocalProcess} if requested to explicity do so.
 *
 */
public class DefaultProcessFactory implements ForkerProcessFactory {

	@Override
	public ForkerProcess createProcess(ForkerBuilder builder, ForkerProcessListener listener) throws IOException {
		if (builder.io() == IO.DEFAULT) {
			if(listener instanceof NonBlockingProcessListener) {
				throw new IllegalArgumentException(String.format("%s is not supported by %s, is your I/O mode set correctly (see %s.io(%s))", listener.getClass(), getClass(), ForkerBuilder.class, IO.class));
			}
			return new LocalProcess(builder);
		}
		return null;
	}

}
