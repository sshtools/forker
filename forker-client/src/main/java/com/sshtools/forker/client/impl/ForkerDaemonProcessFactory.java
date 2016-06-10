package com.sshtools.forker.client.impl;

import java.io.IOException;
import java.net.ConnectException;

import com.sshtools.forker.client.AbstractForkerProcess;
import com.sshtools.forker.client.EffectiveUser;
import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.client.ForkerProcessFactory;
import com.sshtools.forker.common.IO;

/**
 * Creates {@link ForkerDaemonProcess}.
 */
public class ForkerDaemonProcessFactory implements ForkerProcessFactory {

	@Override
	public AbstractForkerProcess createProcess(ForkerBuilder builder) throws IOException {
		if (builder.io() != IO.DEFAULT) {
			try {
				EffectiveUser effectiveUser = builder.effectiveUser();
				ForkerDaemonProcess forkerProcess = new ForkerDaemonProcess(builder.getCommand());
				if (effectiveUser != null)
					effectiveUser.elevate(builder, forkerProcess, builder.getCommand());
				try {
					forkerProcess.start();
					return forkerProcess;
				} finally {
					if (effectiveUser != null)
						effectiveUser.descend(builder, forkerProcess, builder.getCommand());
				}
			} catch (ConnectException ce) {
				// No forker, we will have to resort to using standard
				// ProcessBuilder
			}

			if (builder.io() == IO.DAEMON) {
				throw new IOException("Mode explicitly request daemon, but daemon process failed.");
			}
		}
		return null;
	}

}
