package com.sshtools.forker.client.impl;

import java.io.IOException;

import org.apache.commons.lang3.SystemUtils;

import com.sshtools.forker.client.AbstractForkerProcess;
import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.client.ForkerProcessFactory;
import com.sshtools.forker.common.IO;

/**
 * Creates {@link POpenProcess}.
 */
public class POpenProcessFactory implements ForkerProcessFactory {

	@Override
	public AbstractForkerProcess createProcess(ForkerBuilder builder) throws IOException {
		if (SystemUtils.IS_OS_UNIX && (builder.io() == IO.INPUT || builder.io() == IO.OUTPUT))
			// We need either input, or output, but not both, so use popen
			return new POpenProcess(builder);
		return null;
	}

}
