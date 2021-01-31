package com.sshtools.forker.examples;

import java.io.Console;
import java.io.IOException;

import com.sshtools.forker.client.EffectiveUserFactory.DefaultEffectiveUserFactory;
import com.sshtools.forker.client.Forker;
import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.client.ShellBuilder;
import com.sshtools.forker.common.OS;
import com.sshtools.forker.common.Util;
import com.sshtools.forker.pty.PTYExecutor;

/**
 * Launch an interactive login shell as a particular user using a Pseudo
 * Terminal, or PTY. PTY supports is currently only available via <i>Forker
 * Daemon</i>, which in this example is launched as an administrator allowing
 * any user to be used for the shell.
 */
public class ShellAsUser {

	public static void main(String[] args) throws Exception {
		/*
		 * This example reads from stdni, so stdid needs to be unbuffered with
		 * no local echoing at this end of the pipe, the following function
		 * attempts to do this
		 */
		OS.unbufferedStdin();

		/*
		 * Run the daemon itself as an administrator. This should popup a
		 * password request (if supported on platform)
		 */
		Forker.loadDaemon(true);

		ForkerBuilder shell = new ShellBuilder().loginShell(true).io(PTYExecutor.PTY).redirectErrorStream(true);

		/*
		 * Run the shell as the user that launches this class. Any valid UID
		 * could be used, but we just get the current UID (using 'id -u'
		 * command) and ask for the shell we spawn to use the that user.
		 */
		Console console = System.console();
		if (console == null && args.length == 0)
			throw new IOException("No console available and no username supplied as command line argument.");
		shell.effectiveUser(DefaultEffectiveUserFactory.getDefault()
				.getUserForUsername(args.length == 0 ? console.readLine("Username:") : args[0]));

		// Start process
		final Process p = shell.start();

		/*
		 * Connect both the input and the output streams, start the process and
		 * wait for it to finish
		 */
		new Thread() {
			public void run() {
				try {
					Util.copy(System.in, p.getOutputStream());
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
		Util.copy(p.getInputStream(), System.out);
		int ret = p.waitFor();
		System.err.println("Exited with code: " + ret);
		System.exit(ret);
	}
}
