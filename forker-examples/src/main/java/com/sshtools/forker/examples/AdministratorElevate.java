package com.sshtools.forker.examples;

import com.sshtools.forker.client.EffectiveUser;
import com.sshtools.forker.client.EffectiveUserFactory;
import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.common.IO;
import com.sshtools.forker.common.Util;
import com.sun.jna.Platform;

/**
 * Demonstrates elevating to administrator using {@link ForkerBuilder}.
 */
public class AdministratorElevate {
	public static void main(String[] args) throws Exception {
		
		/* Get the user object for admnistrator */
		EffectiveUser administrator = EffectiveUserFactory.getDefault().administrator();
		
		/* Create the builder */
		ForkerBuilder builder = new ForkerBuilder().effectiveUser(administrator).io(IO.IO);
		if (Platform.isLinux()) {
			/* The linux example tries to list the shadow password file */
			builder.redirectErrorStream(true).io(IO.IO).command("cat", "/etc/shadow");
		} else if (Platform.isWindows()) {
			/*
			 * Windows try to create a filename protected by WRP
			 * builder.command("dir", ">", "c:\\forker-test.exe");
			 * builder.command("C:\\windows\\system32\\cmd.exe", "/c", "dir
			 * \\");
			 */
			builder.command("C:\\windows\\system32\\xcopy.exe");
			builder.io(IO.SINK);
		} else {
			throw new UnsupportedOperationException();
		}
		
		Process p = builder.start();
		Util.copy(p.getInputStream(), System.out);
		System.out.println(" (" + p.waitFor() + ")");
	}
}
