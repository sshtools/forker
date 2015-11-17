package com.sshtools.forker.client;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.security.AccessControlException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;

import com.sshtools.forker.client.impl.ForkerProcess;
import com.sshtools.forker.client.impl.POpenProcess;
import com.sshtools.forker.client.impl.SystemProcess;
import com.sshtools.forker.common.Command;

/**
 * A replacement for {@link ProcessBuilder} that will either use a 'system' call
 * (when no I/O is required), a 'popen' called (when either input or output is
 * required), a call the a 'Forker Daemon' (must be running as a separate
 * service and can handle both input and output), or finally falling back to the
 * native {@link ProcessBuilder}
 * <p>
 * All of this is to work around the fact the Linux fork() might take a copy (at
 * least temporarily) of the entire JVM. It was found that will JVMs that have
 * large memory set, this might cause the OS to run out of memory and start to
 * refuse to run system commands.
 *
 */
public class ForkerBuilder {
	public enum IO {
		INPUT, OUTPUT, IO, SINK
	}

	private Command command = new Command();
	private IO io = IO.IO;
	private boolean background;
	private EffectiveUser effectiveUser;

	public ForkerBuilder(List<String> command) {
		if (command == null)
			throw new NullPointerException();
		this.command.getArguments().addAll(command);
	}

	public ForkerBuilder(String... command) {
		this.command.getArguments().addAll(Arrays.asList(command));
	}

	public ForkerBuilder command(List<String> command) {
		if (command == null)
			throw new NullPointerException();
		this.command.getArguments().clear();
		this.command.getArguments().addAll(command);
		return this;
	}

	public ForkerBuilder command(String... command) {
		this.command.getArguments().clear();
		this.command.getArguments().addAll(Arrays.asList(command));
		return this;
	}

	public EffectiveUser effectiveUser() {
		return effectiveUser;
	}

	public ForkerBuilder effectiveUser(EffectiveUser effictiveUser) {
		this.effectiveUser = effictiveUser;
		return this;
	}

	public boolean background() {
		return background;
	}

	public ForkerBuilder background(boolean background) {
		this.background = background;
		return this;
	}

	public IO io() {
		return io;
	}

	public ForkerBuilder io(IO io) {
		if ((io == IO.SINK || io == IO.OUTPUT) && redirectErrorStream()) {
			throw new IllegalStateException("Cannot set IO mode '" + io
					+ "' because redirectErrorStream() is true.");
		}
		this.io = io;
		return this;
	}

	public List<String> command() {
		return command.getArguments();
	}

	public Map<String, String> environment() {
		return command.getEnvironment();
	}

	ForkerBuilder environment(String[] envp) {
		if (envp != null) {
			this.command.getEnvironment().clear();
			for (String envstring : envp) {
				if (envstring.indexOf((int) '\u0000') != -1)
					envstring = envstring.replaceFirst("\u0000.*", "");
				int eqlsign = envstring.indexOf('=', 0);
				if (eqlsign != -1)
					this.command.getEnvironment().put(
							envstring.substring(0, eqlsign),
							envstring.substring(eqlsign + 1));
			}
		}
		return this;
	}

	public File directory() {
		return command.getDirectory();
	}

	public ForkerBuilder directory(File directory) {
		this.command.setDirectory(directory);
		return this;
	}

	public boolean redirectErrorStream() {
		return command.isRedirectError();
	}

	public ForkerBuilder redirectErrorStream(boolean redirectErrorStream) {
		if (redirectErrorStream && io != IO.IO && io != IO.INPUT) {
			throw new IllegalStateException(
					"Cannot redirect error stream if using IO mode '" + io
							+ "'");
		}
		this.command.setRedirectError(redirectErrorStream);
		return this;
	}

	public Process start() throws IOException {
		// As far as I know none of this is required on Windows
		if (SystemUtils.IS_OS_WINDOWS
				|| "true".equals(System.getProperty("forker.forceJavaFork"))) {
			return startProcessBuilder();
		}

		// Must convert to array first -- a malicious user-supplied
		// list might try to circumvent the security check.
		String[] cmdarray = command.getArguments().toArray(
				new String[command.getArguments().size()]);
		cmdarray = cmdarray.clone();
		for (String arg : cmdarray)
			if (arg == null)
				throw new NullPointerException();
		// Throws IndexOutOfBoundsException if command is empty
		String prog = cmdarray[0];

		SecurityManager security = System.getSecurityManager();
		if (security != null) {
			security.checkExec(prog);
		}

		String dir = command.getDirectory() == null ? null : command
				.getDirectory().toString();

		for (int i = 1; i < cmdarray.length; i++) {
			if (cmdarray[i].indexOf('\u0000') >= 0) {
				throw new IOException("invalid null character in command");
			}
		}

		switch (io) {
		case INPUT:
		case OUTPUT:
			// We need either input, or output, but not both, so use popen
			try {
				return new POpenProcess(this);
			} catch (IllegalArgumentException e) {
				throw handleIllegalArgumentException(prog, dir, e);
			}
		case SINK:
			/*
			 * We don't need any input or output, so can just start using
			 * 'system' call which just blocks
			 */
			try {
				return new SystemProcess(this);
			} catch (IllegalArgumentException e) {
				throw handleIllegalArgumentException(prog, dir, e);
			}
		case IO:
			// We need input and output, first try and connect to the forker
			// daemon
			try {
				return new ForkerProcess(command);
			} catch (ConnectException ce) {
				// No forker, we will have to resort to using standard
				// ProcessBuilder
				return startProcessBuilder();
			} catch (IOException e) {
				throw handleIOException(prog, security, dir, e);
			} catch (IllegalArgumentException e) {
				throw handleIllegalArgumentException(prog, dir, e);
			}
		default:
			throw new UnsupportedOperationException();
		}
	}

	protected IOException handleIllegalArgumentException(String prog,
			String dir, IllegalArgumentException e) {
		String exceptionInfo = ": " + e.getMessage();
		return new IOException("Cannot run program \"" + prog + "\""
				+ (dir == null ? "" : " (in directory \"" + dir + "\")")
				+ exceptionInfo, e);
	}

	protected IOException handleIOException(String prog,
			SecurityManager security, String dir, IOException e)
			throws IOException {
		String exceptionInfo = ": " + e.getMessage();
		Throwable cause = e;
		if (security != null) {
			try {
				security.checkRead(prog);
			} catch (AccessControlException ace) {
				exceptionInfo = "";
				cause = ace;
			}
		}
		return new IOException("Cannot run program \"" + prog + "\""
				+ (dir == null ? "" : " (in directory \"" + dir + "\")")
				+ exceptionInfo, cause);
	}

	protected Process startProcessBuilder() throws IOException {
		// Fallback to standard ProcessBuilder

		if (effectiveUser != null)
			effectiveUser.elevate(this, null);

		ProcessBuilder pb = new ProcessBuilder(command.getArguments());
		if (command.isRedirectError()) {
			pb.redirectErrorStream(true);
		}
		if (command.getDirectory() != null) {
			pb.directory(command.getDirectory());
		}
		if (command.getEnvironment() != null) {
			pb.environment().putAll(command.getEnvironment());
		}
		try {
			return pb.start();
		} finally {
			if (effectiveUser != null)
				effectiveUser.descend();
		}
	}

	public static void main(String[] args) throws Exception {
		ForkerBuilder fb = new ForkerBuilder("ls", "/etc");
		fb.io(IO.INPUT);
		// fb.redirectErrorStream(true);
		Process p = fb.start();
		IOUtils.copy(p.getInputStream(), System.out);
		System.out.println("Exit value: " + p.exitValue());
	}
}
