package com.sshtools.forker.client.impl;

import java.io.IOException;

import org.apache.commons.lang3.SystemUtils;

import com.sshtools.forker.client.AbstractForkerProcess;
import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.client.ForkerProcessFactory;
import com.sshtools.forker.common.IO;

/**
 * Creates a {@link SystemProcess}.
 *
 */
public class SystemProcessFactory implements ForkerProcessFactory {

	@Override
	public AbstractForkerProcess createProcess(ForkerBuilder builder) throws IOException {
		if (SystemUtils.IS_OS_UNIX && builder.io() == IO.SINK)
			/*
			 * We don't need any input or output, so can just start using
			 * 'system' call which just blocks
			 */
			return new SystemProcess(builder);
		return null;
	}

}
