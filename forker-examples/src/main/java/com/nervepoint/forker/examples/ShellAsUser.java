package com.nervepoint.forker.examples;

import java.io.IOException;

import org.apache.commons.io.IOUtils;

import com.sshtools.forker.client.EffectiveUserFactory;
import com.sshtools.forker.client.Forker;
import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.client.ShellBuilder;
import com.sshtools.forker.common.IO;

public class ShellAsUser {

	public static void main(String[] args) throws Exception {
		//Forker.loadDaemon(true);
		Forker.connectUnauthenticatedDaemon(1234);
		ForkerBuilder shell = new ShellBuilder().loginShell(true).io(IO.PTY).redirectErrorStream(true);
		shell.effectiveUser(new EffectiveUserFactory.POSIXEffectiveUser(1001));
		
		final Process p = shell.start();
		Thread t = new Thread() {
			public void run() {
				try {
					IOUtils.copy(System.in, p.getOutputStream());
				} catch (IOException e) {
				}
			}
		};
		t.setDaemon(true);
		t.start();
		
		IOUtils.copy(p.getInputStream(), System.out);
		int ret = p.waitFor();
		System.err.println("Exited with code: " +ret);
		System.exit(ret);
	}
}
