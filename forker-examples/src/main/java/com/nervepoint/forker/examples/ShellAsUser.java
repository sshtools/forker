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
		shell.effectiveUser(new EffectiveUserFactory.POSIXUIDEffectiveUser(Integer.parseInt(OSCommand.runCommandAndCaptureOutput("id", "-u").iterator().next())));
		
		final Process p = shell.start();
		
		new Thread() {
			public void run() {
				try {
					IOUtils.copy(System.in, p.getOutputStream());
				} catch (IOException e) {
				} finally {
					// Close the process input stream when stdin closes, this
					// will end the process
					try {
						p.getOutputStream().close();
					} catch (IOException e) {
					}
				}
			}
		}.start();
		IOUtils.copy(p.getInputStream(), System.out);
		int ret = p.waitFor();
		System.err.println("Exited with code: " + ret);
		System.exit(ret);
	}
}
