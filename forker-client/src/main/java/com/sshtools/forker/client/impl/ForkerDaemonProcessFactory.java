package com.sshtools.forker.client.impl;

import java.io.IOException;
import java.net.ConnectException;

import com.sshtools.forker.client.EffectiveUser;
import com.sshtools.forker.client.Forker;
import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.client.ForkerProcess;
import com.sshtools.forker.client.ForkerProcessFactory;
import com.sshtools.forker.client.ForkerProcessListener;
import com.sshtools.forker.client.impl.ForkerDaemonProcess.Listener;
import com.sshtools.forker.common.IO;
import com.sshtools.forker.common.OS;

/**
 * Creates {@link ForkerDaemonProcess}.
 */
public class ForkerDaemonProcessFactory implements ForkerProcessFactory {

	@Override
	public ForkerProcess createProcess(ForkerBuilder builder, ForkerProcessListener listener) throws IOException {
		IO io = builder.io();
		if (io != IO.DEFAULT && !Boolean.getBoolean("forker.disableDaemon")) {
			

			
			
			try {
				EffectiveUser effectiveUser = builder.effectiveUser();
				
				ForkerDaemonProcess forkerProcess = new ForkerDaemonProcess(builder.getCommand());
				if(listener instanceof Listener) {
					forkerProcess.addListener((Listener)listener);
				}
				if (effectiveUser != null) {
					if (!Forker.isDaemonRunning() && !OS.isAdministrator())
						return null;
					effectiveUser.elevate(builder, forkerProcess, builder.getCommand());
				}
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

			if (io == IO.DAEMON) {
				throw new IOException("Mode explicitly request daemon, but daemon process failed.");
			}
		}
		return null;
	}

}
