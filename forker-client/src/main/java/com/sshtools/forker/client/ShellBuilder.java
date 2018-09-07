/**
 * Copyright © 2015 - 2018 SSHTOOLS Limited (support@sshtools.com)
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
package com.sshtools.forker.client;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.SystemUtils;

/**
 * Extension to {@link ForkerBuilder} that with launch a shell appropriate for
 * the operating system (optionally running any command specified).
 *
 * If you want an interactive shell, you probably want to set
 * {@link #loginShell(boolean)} and {@link #io(com.sshtools.forker.common.IO)}
 * to <code>IO.PTY</code>
 */
public class ShellBuilder extends ForkerBuilder {

	private boolean loginShell;
	private File rcfile;

	/**
	 * Construct a new builder given a list of command arguments. The first
	 * element is the location of the script, anything remaining is passed to this
	 * script as arguments.  If no arguments are provided, the shell itself
	 * will be executed.
	 * 
	 * @param configuration configuration
	 * @param command command and command arguments
	 */
	public ShellBuilder(ForkerConfiguration configuration, List<String> command) {
		super(configuration, command);
	}


	/**
	 * Construct a new builder given an array (or varargs) of command arguments. The first
	 * element is the location of the script, anything remaining is passed to this
	 * script as arguments. If no arguments are provided, the shell itself
	 * will be executed.
	 * 
	 * @param configuration configuration
	 * @param command command and command arguments
	 */
	public ShellBuilder(ForkerConfiguration configuration, String... command) {
		super(configuration, command);
	}

	/**
	 * Construct a new builder given a list of command arguments. The first
	 * element is the command to execute, anything remaining is passed to this
	 * command as arguments. If no arguments are provided, the shell itself
	 * will be executed.
	 * 
	 * @param command command and command arguments
	 */
	public ShellBuilder(List<String> command) {
		super(command);
	}

	/**
	 * Construct a new builder given an array of command arguments. The first
	 * element is the command to execute, anything remaining is passed to this
	 * command as arguments. If no arguments are provided, the shell itself
	 * will be executed.
	 * 
	 * @param command command and command arguments
	 */
	public ShellBuilder(String... command) {
		super(command);
	}

	/**
	 * Get if the shell should be a login shell. This will affect the
	 * environment and other attributes.
	 * 
	 * @return login shell
	 */
	public boolean loginShell() {
		return loginShell;
	}

	/**
	 * Indicate the shell should be a login shell. This will affect the
	 * environment and other attributes.
	 * 
	 * @param loginShell
	 *            login shell
	 * @return builder for chaining
	 */
	public ShellBuilder loginShell(boolean loginShell) {
		this.loginShell = loginShell;
		return this;
	}

	/**
	 * Get the 'rcfile' to run on every shell.
	 * 
	 * @return rcfile
	 */
	public File rcfile() {
		return rcfile;
	}

	/**
	 * Specify an 'rcfile' to run on every shell.
	 * 
	 * @param rcfile rc file
	 * @return builder for chaining
	 */
	public ShellBuilder rcfile(File rcfile) {
		this.rcfile = rcfile;
		return this;
	}

	public ForkerProcess start() throws IOException {
		if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_UNIX || SystemUtils.IS_OS_MAC_OSX) {
			// This can make us unbuffered and give a much more useful
			// terminal

			// The shell, should be in /bin but just in case
			String shLocation = findCommand("bash", "/usr/bin/bash", "/bin/bash");
			if (shLocation == null) {
				// Sh
				shLocation = findCommand("sh", "/usr/bin/sh", "/bin/sh");
				if (shLocation == null) {
					// No shell, just execute as is
					if (command().isEmpty()) {
						throw new IOException("Nothing to execute.");
					}
				} else {
					if (loginShell) {
						command().add(0, "--login");
					}
					command().add(0, shLocation);
					if (rcfile != null) {
						environment().put("ENV", rcfile.getAbsolutePath());
					}
				}
			} else {
				// rcfile only works with non-login shell :(
				if (rcfile != null) {
					command().add(0, rcfile.getAbsolutePath());
					command().add(0, "--rcfile");
				}
				if (loginShell) {
					command().add(0, "--login");
				}

				// Bash
				command().add(0, shLocation);
			}

		} else if (SystemUtils.IS_OS_WINDOWS) {
			/*
			 * Currently must be handled by forker daemon, this is a special
			 * signal to just start a WinPTY shell. NOTE: If you change this,
			 * also change Forker.java so it handles this correct (in the case
			 * of IO.PTY)
			 */
			command().add(0, "CMD.exe");
			command().add(0, "/c");
			command().add(0, "start");
		}
		return super.start();
	}

	private String findCommand(String command, String... places) throws IOException {
		Collection<String> stdbuf = OSCommand.runCommandAndCaptureOutput("which", command);
		if (stdbuf.isEmpty()) {
			for (String place : places) {
				File f = new File(place);
				if (f.exists()) {
					return f.getAbsolutePath();
				}
			}
		}
		return stdbuf.iterator().next();
	}
}
