package com.sshtools.forker.pty;

import java.io.IOException;

import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.client.ForkerProcess;
import com.sshtools.forker.client.ForkerProcessFactory;
import com.sshtools.forker.client.ForkerProcessListener;
import com.sshtools.forker.client.NonBlockingProcessListener;
import com.sshtools.forker.common.IO;
import com.sshtools.forker.common.OS;
import com.sun.jna.Platform;

/**
 * Creates a {@link PTYProcess}.
 */
public class PTYProcessFactory implements ForkerProcessFactory {

	@Override
	public ForkerProcess createProcess(ForkerBuilder builder, ForkerProcessListener listener) throws IOException {
		if (OS.isUnix() && !Platform.isMac() && builder.io() == PTYProcess.PTY) {
			if(listener instanceof NonBlockingProcessListener) {
				throw new IllegalArgumentException(String.format("%s is not supported by %s, is your I/O mode set correctly (see %s.io(%s))", listener.getClass(), getClass(), ForkerBuilder.class, IO.class));
			}
			return new PTYProcess(builder);
		}
		return null;
	}

}
