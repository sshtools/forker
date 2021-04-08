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
package com.sshtools.forker.client;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

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
	private String shell;

	/**
	 * Construct a new builder given a list of command arguments. The first
	 * element is the location of the script, anything remaining is passed to
	 * this script as arguments. If no arguments are provided, the shell itself
	 * will be executed.
	 * 
	 * @param configuration configuration
	 * @param command command and command arguments
	 */
	public ShellBuilder(ForkerConfiguration configuration, List<String> command) {
		super(configuration, command);
	}

	/**
	 * Construct a new builder given an array (or varargs) of command arguments.
	 * The first element is the location of the script, anything remaining is
	 * passed to this script as arguments. If no arguments are provided, the
	 * shell itself will be executed.
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
	 * command as arguments. If no arguments are provided, the shell itself will
	 * be executed.
	 * 
	 * @param command command and command arguments
	 */
	public ShellBuilder(List<String> command) {
		super(command);
	}

	/**
	 * Construct a new builder given an array of command arguments. The first
	 * element is the command to execute, anything remaining is passed to this
	 * command as arguments. If no arguments are provided, the shell itself will
	 * be executed.
	 * 
	 * @param command command and command arguments
	 */
	public ShellBuilder(String... command) {
		super(command);
	}

	/**
	 * Get the preferred shell name. This may either be <code>null</code>,
	 * indicating there is no preferred shell, just use the auto-detected one.
	 * It may be a simple name without any extension or any additional path
	 * information such as 'bash' or 'pwsh'. Finally it may be a full qualified
	 * path to the actual shell.
	 * 
	 * @return the shell name or path
	 */
	public String shell() {
		return shell;
	}

	/**
	 * Set the preferred shell name. This may either be <code>null</code>,
	 * indicating there is no preferred shell, just use the auto-detected one.
	 * It may be a simple name without any extension or any additional path
	 * information such as 'bash' or 'pwsh'. Finally it may be a full qualified
	 * path to the actual shell.
	 * 
	 * @param shell the shell name or path
	 * @return building for chaining
	 */
	public ShellBuilder shell(String shell) {
		this.shell = shell;
		return this;
	}

	/**
	 * Get if the shell should be a login shell. This will affect the
	 * environment and other attributes. Note, will only automatically
	 * add the argument required for this if the {@link #shell()} is
	 * <code>null</code>, i.e. automatic.
	 * 
	 * @return login shell
	 */
	public boolean loginShell() {
		return loginShell;
	}

	/**
	 * Indicate the shell should be a login shell. This will affect the
	 * environment and other attributes. Note, will only automatically
	 * add the argument required for this if the {@link #shell()} is
	 * <code>null</code>, i.e. automatic.
	 * 
	 * @param loginShell login shell
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

	public <P extends ForkerProcess> P start(ForkerProcessListener listener) throws IOException {
		String shLocation = null;
		
		if (StringUtils.isNotBlank(shell)) {
			if (shell.contains("/") || shell.contains("\\")) {
				/* Absolute path to shell */
				if (new File(shell).exists())
					shLocation = shell;
				else
					throw new IOException(String.format("Shell %s not found.", shell));
			} else {
				/* Shell command name */
				shLocation = findCommand(shell);
			}
		}
		
		if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_UNIX || SystemUtils.IS_OS_MAC_OSX) {
			// The shell, should be in /bin but just in case
			
			if (shLocation == null) {
				shLocation = findCommand("bash");
				if(shLocation != null) {
					// rcfile only works with non-login shell :(
					if (rcfile != null) {
						command().add(0, rcfile.getAbsolutePath());
						command().add(0, "--rcfile");
					}
					if (loginShell) {
						command().add(0, "--login");
					}
				}
			}
			if (shLocation == null) {
				// Sh
				shLocation = findCommand("sh");
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
				// Everything else
				command().add(0, shLocation);
			}
		} else if (SystemUtils.IS_OS_WINDOWS) {
			if(shLocation == null) {
				command().add(0, "CMD.exe");
				command().add(0, "/c");
				command().add(0, "start");
			}
			else
				command().add(0, shLocation);
		}
		return super.start(listener);
	}

	private String findCommand(String command) throws IOException {
		if(SystemUtils.IS_OS_UNIX) {
			Collection<String> stdbuf = OSCommand.runCommandAndCaptureOutput("which", command);
			if (!stdbuf.isEmpty()) {
				return stdbuf.iterator().next();
			}
		}
		
		/* What path extensions might be used? */
		List<String> exts = new LinkedList<>();
		if(SystemUtils.IS_OS_WINDOWS) {
			/* Do we have a PATHEXT? */
			String pathExt = System.getenv("PATHEXT");
			if(StringUtils.isBlank(pathExt)) {
				/* Assume a default set */
				pathExt = ".COM;.EXT;.BAT;.CMD;.VBS;.VBE;.JS;.JSE;.WSF;.WSH;.PSC1";
			}
			exts.addAll(Arrays.asList(pathExt.split(File.pathSeparator)));
		}
		
		/* Do we have a PATH? */
		String path = System.getenv("PATH");

		/* No path, add some defaults */
		// TODO probably need more OS's 
		if(StringUtils.isBlank(path)) {
			if(SystemUtils.IS_OS_UNIX)
				path = "/bin:/usr/bin";
			else if(SystemUtils.IS_OS_WINDOWS)
				path = "C:\\Windows\\System32";
		}
		
		if(StringUtils.isNotBlank(path)) {
			for(String p : path.split(File.pathSeparator)) {
				/* With a discovered extension? */
				for(String pe : exts) {
					File f = new File(p, command + pe);
					if(f.exists())
						return f.getAbsolutePath();
				}
				
				/* As is? */
				File f = new File(p, command);
				if(f.exists())
					return f.getAbsolutePath();
			}
		}
		
		/* Still not got it, just return exactly as was supplied */
		return command;
	}
}
