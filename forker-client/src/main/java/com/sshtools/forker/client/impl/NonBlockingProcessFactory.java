package com.sshtools.forker.client.impl;

import java.io.IOException;

import org.apache.commons.lang.SystemUtils;

import com.sshtools.forker.client.AbstractForkerProcess;
import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.client.ForkerProcessFactory;
import com.sshtools.forker.common.IO;

/**
 * Creates {@link NonBlockingProcess}.
 */
public class NonBlockingProcessFactory implements ForkerProcessFactory {

	@Override
	public AbstractForkerProcess createProcess(ForkerBuilder builder) throws IOException {
		if (SystemUtils.IS_OS_UNIX && (builder.io() == IO.NON_BLOCKING))
			return new NonBlockingProcess(builder);
		return null;
	}

}
