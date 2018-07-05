package com.nervepoint.forker.examples;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;

import com.sshtools.forker.client.EffectiveUserFactory;
import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.common.IO;

public class AdministratorElevate {

	public static void main(String[] args) throws Exception {

		ForkerBuilder builder = new ForkerBuilder().effectiveUser(
				EffectiveUserFactory.getDefault().administrator()).io(IO.IO);

		if (SystemUtils.IS_OS_LINUX) {
			// The linux example tries to list the shadow password file
			builder.redirectErrorStream(true).io(IO.IO).command("cat", "/etc/shadow");
		} else if (SystemUtils.IS_OS_WINDOWS) {
			// Windows try to create a filename protected by WRP
			//builder.command("dir", ">", "c:\\forker-test.exe");
			//builder.command("C:\\windows\\system32\\cmd.exe", "/c", "dir \\");
			builder.command("C:\\windows\\system32\\xcopy.exe");
			builder.io(IO.SINK);
		} else {
			throw new UnsupportedOperationException();
		}

		Process p = builder.start();
		IOUtils.copy(p.getInputStream(), System.out);
		System.out.println(" (" + p.waitFor() + ")");
	}
}
