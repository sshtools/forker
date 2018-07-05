package com.sshtools.forker.client;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

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
	 * Specify an 'rcfile' to run on every shell.
	 * 
	 * @param rcfile rc file
	 * @return builder for chaining
	 */
	public ShellBuilder rcfile(File rcfile) {
		this.rcfile = rcfile;
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
	 * Get if the shell should be a login shell. This will affect the
	 * environment and other attributes.
	 * 
	 * @return login shell
	 */
	public boolean loginShell() {
		return loginShell;
	}

	public Process start() throws IOException {
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
