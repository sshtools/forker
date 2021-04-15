package com.sshtools.forker.examples;

import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.common.OS;
import com.sshtools.forker.common.Util;
import com.sshtools.forker.wrapper.ForkerWrapper;
import com.sshtools.forker.wrapper.KeyValuePair;
import com.sshtools.forker.wrapper.WrapperIO;
import com.sshtools.forker.wrapper.WrapperProcessFactory;
import com.sun.jna.Platform;

/**
 * Another way of embedded {@link ForkerWrapper}, this time by using the
 * {@link ForkerBuilder} with an I/O mode of {@link WrapperIO}.
 */
public class WrappedProcessTest {

	public static void main(String[] args) throws Exception {
		// Standard builder creation
		ForkerBuilder fb = new ForkerBuilder();

		// Choose a command to run based on OS
		if (OS.isUnix())
			fb.parse("ls /etc");
		else if (Platform.isWindows())
			fb.parse("DIR C:\\");
		else
			throw new UnsupportedOperationException("Add a command for your OS to test.");

		// Get a handle on the process factory, allowing configuration of it
		WrapperProcessFactory processFactory = fb.configuration().processFactory(WrapperProcessFactory.class);

		processFactory.addOption(new KeyValuePair("native", "true"));
		processFactory.addOption(new KeyValuePair("level", "FINEST"));
		processFactory.addOption(new KeyValuePair("restart-on", "0"));
		processFactory.addOption(new KeyValuePair("restart-wait", "10"));

		// Launches a new JVM
		processFactory.setSeparateProcess(true);

		// Tell forker builder we want a wrapped process
		fb.io(WrapperIO.WRAPPER);

		// Boilerplate stuff
		fb.redirectErrorStream(true);
		Process p = fb.start();
		Util.copy(p.getInputStream(), System.out);
		System.out.println(" (" + p.waitFor() + ")");

	}

}
