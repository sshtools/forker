package com.nervepoint.forker.examples;

import java.io.IOException;

import org.apache.commons.io.IOUtils;

import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.client.ShellBuilder;
import com.sshtools.forker.common.IO;

public class Shell {

	public static void main(String[] args) throws Exception {
		ForkerBuilder shell = new ShellBuilder().loginShell(true).io(IO.PTY).redirectErrorStream(true);
		final Process p = shell.start();
		new Thread() {
			public void run() {
				try {
					IOUtils.copy(p.getInputStream(), System.out);
				} catch (IOException e) {
				}
			}
		}.start();
		IOUtils.copy(System.in, p.getOutputStream());
		int ret = p.waitFor();
		System.err.println("Exited with code: " +ret);
		System.exit(ret);
	}
}
