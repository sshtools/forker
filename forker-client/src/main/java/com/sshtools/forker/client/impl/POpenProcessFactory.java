package com.sshtools.forker.client.impl;

import java.io.IOException;

import org.apache.commons.lang3.SystemUtils;

import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.client.ForkerProcess;
import com.sshtools.forker.client.ForkerProcessFactory;
import com.sshtools.forker.client.ForkerProcessListener;
import com.sshtools.forker.client.NonBlockingProcessListener;
import com.sshtools.forker.common.IO;

/**
 * Creates {@link POpenProcess}.
 */
public class POpenProcessFactory implements ForkerProcessFactory {

	@Override
	public ForkerProcess createProcess(ForkerBuilder builder, ForkerProcessListener listener) throws IOException {
		if (SystemUtils.IS_OS_UNIX && (builder.io() == IO.INPUT || builder.io() == IO.OUTPUT)) {
			// We need either input, or output, but not both, so use popen
			if(listener instanceof NonBlockingProcessListener) {
				throw new IllegalArgumentException(String.format("%s is not supported by %s, is your I/O mode set correctly (see %s.io(%s))", listener.getClass(), getClass(), ForkerBuilder.class, IO.class));
			}
			return new POpenProcess(builder);
		}
		return null;
	}

}
