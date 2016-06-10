package com.nervepoint.forker.examples;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;

import com.sshtools.forker.client.EffectiveUserFactory;
import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.common.IO;

public class RunAsUser {

	public static void main(String[] args) throws Exception {

		// Change this to another user on your system
		String username = "testuser2";
		
		ForkerBuilder builder = new ForkerBuilder().effectiveUser(EffectiveUserFactory.getDefault().getUserForUsername(username)).
				io(IO.IO).redirectErrorStream(true);
		
		if(SystemUtils.IS_OS_LINUX) {
			// The linux example tries to list the users home directory
			builder.command("id");
		}
		else {
			throw new UnsupportedOperationException();
		}
		
		Process p = builder.start();
		IOUtils.copy(p.getInputStream(), System.out);
		System.out.println(" (" + p.waitFor() + ")");
	}
}
