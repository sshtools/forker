package com.sshtools.forker.client.impl;

import java.io.IOException;

import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.client.ForkerProcess;
import com.sshtools.forker.client.ForkerProcessFactory;
import com.sshtools.forker.client.ForkerProcessListener;
import com.sshtools.forker.client.NonBlockingProcessListener;
import com.sshtools.forker.common.IO;
import com.sshtools.forker.common.OS;

/**
 * Creates a {@link SystemProcess}.
 *
 */
public class SystemProcessFactory implements ForkerProcessFactory {

	@Override
	public ForkerProcess createProcess(ForkerBuilder builder, ForkerProcessListener listener) throws IOException {
		if (OS.isUnix() && builder.io() == IO.SINK) {

			if(listener instanceof NonBlockingProcessListener) {
				throw new IllegalArgumentException(String.format("%s is not supported by %s, is your I/O mode set correctly (see %s.io(%s))", listener.getClass(), getClass(), ForkerBuilder.class, IO.class));
			}
			/*
			 * We don't need any input or output, so can just start using
			 * 'system' call which just blocks
			 */
			return new SystemProcess(builder);
		}
		return null;
	}

}
