package com.sshtools.forker.examples;

import com.sshtools.forker.client.EffectiveUser;
import com.sshtools.forker.client.EffectiveUserFactory;
import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.common.IO;
import com.sshtools.forker.common.Util;
import com.sun.jna.Platform;

/**
 * Demonstrates running a command as another user.
 */
public class RunAsUser {

	public static void main(String[] args) throws Exception {

		/* Either supply a username on the command line when you run this class or 
		 * change this username 'testuser2' to one that exists on your system.
		 */
		String username = args.length == 0 ? "testuser2" : args[0];
		
		/* Get the user object for this user */
		EffectiveUser user = EffectiveUserFactory.getDefault().getUserForUsername(username);
		
		/* Create the builder */
		ForkerBuilder builder = new ForkerBuilder().effectiveUser(user).
				io(IO.IO).redirectErrorStream(true);
		
		if(Platform.isLinux()) {
			// The linux example tries to list the users home directory
			builder.command("id");
		}
		else {
			throw new UnsupportedOperationException();
		}
		
		Process p = builder.start();
		Util.copy(p.getInputStream(), System.out);
		System.out.println(" (" + p.waitFor() + ")");
	}
}
