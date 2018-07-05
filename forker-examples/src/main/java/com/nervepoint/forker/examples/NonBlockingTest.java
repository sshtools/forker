package com.nervepoint.forker.examples;

import org.apache.commons.lang.SystemUtils;

import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.client.impl.DefaultNonBlockingProcessListener;
import com.sshtools.forker.common.IO;

public class NonBlockingTest {
	public static void main(String[] args) throws Exception {
		ForkerBuilder builder = new ForkerBuilder().io(IO.NON_BLOCKING).redirectErrorStream(true);
		if (SystemUtils.IS_OS_LINUX) {
			// The linux example tries to list the root directory
			builder.command("ls", "-al", "/");
		} else {
			throw new UnsupportedOperationException();
		}
		new DefaultNonBlockingProcessListener(builder.start()) {
		};
	}
}
