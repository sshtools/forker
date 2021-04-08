/**
 * Copyright © 2015 - 2021 SSHTOOLS Limited (support@sshtools.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nervepoint.forker.examples;

import java.io.Console;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

import com.sshtools.forker.client.EffectiveUserFactory.DefaultEffectiveUserFactory;
import com.sshtools.forker.client.Forker;
import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.client.ShellBuilder;
import com.sshtools.forker.common.OS;
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
