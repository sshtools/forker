package com.nervepoint.forker.examples;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;

import com.sshtools.forker.client.EffectiveUserFactory;
import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.common.IO;

public class AdministratorElevate {

	public static void main(String[] args) throws Exception {

		ForkerBuilder builder = new ForkerBuilder().effectiveUser(
				EffectiveUserFactory.getDefault().administrator()).io(IO.IO).redirectErrorStream(true);

		if (SystemUtils.IS_OS_LINUX) {
			// The linux example tries to list the shadow password file
			builder.command("cat", "/etc/shadow");
		} else {
			throw new UnsupportedOperationException();
		}

		Process p = builder.start();
		String string = IOUtils.toString(p.getInputStream());
		System.out.println(string);
	}
}
