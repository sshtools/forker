package com.nervepoint.forker.examples;

import java.io.IOException;

import org.apache.commons.io.IOUtils;

import com.sshtools.forker.client.EffectiveUserFactory;
import com.sshtools.forker.client.Forker;
import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.client.OSCommand;
import com.sshtools.forker.client.ShellBuilder;
import com.sshtools.forker.common.IO;

public class ShellAsUser {

	public static void main(String[] args) throws Exception {
		/* Run the daemon itself as an administrator. This should popup a password request
		 * (if supported on platform) */
		Forker.loadDaemon(true);
		
		ForkerBuilder shell = new ShellBuilder().loginShell(true).io(IO.PTY).redirectErrorStream(true);
		
		/* Run the shell as the user that launches this class. Any valid UID could be 
		 * used, but we just get the current UID (using 'id -u' command) and ask for the shell we
		 * spawn to use the that user.
		 */
		shell.effectiveUser(new EffectiveUserFactory.POSIXEffectiveUser(Integer.parseInt(OSCommand.runCommandAndCaptureOutput("id", "-u").iterator().next())));
		
		final Process p = shell.start();
		
		/* Read input from stdin and pass to the forked shell until this applications exits or stdin is closed */
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

		/* Read output from the shell and pass it to stdout until the shell exits */
		IOUtils.copy(p.getInputStream(), System.out);
		int ret = p.waitFor();
		System.err.println("Exited with code: " +ret);
		System.exit(ret);
	}
}
