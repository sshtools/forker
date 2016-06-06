package com.sshtools.forker.client;

import java.io.Console;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.SystemUtils;

import com.sshtools.forker.client.impl.ForkerProcess;
import com.sshtools.forker.client.ui.AskPass;
import com.sshtools.forker.client.ui.AskPassConsole;
import com.sshtools.forker.client.ui.WinRunAs;
import com.sshtools.forker.common.CSystem;
import com.sshtools.forker.common.Command;
import com.sshtools.forker.common.IO;
import com.sshtools.forker.common.OS;
import com.sshtools.forker.common.OS.Desktop;
import com.sshtools.forker.common.Util;

public abstract class EffectiveUserFactory {

	private final static Object lock = new Object();
	private static EffectiveUserFactory instance;

	/**
	 * 
	 */
	protected EffectiveUserFactory(boolean registerAsDefault) {
		if (registerAsDefault) {
			if (instance == null)
				instance = this;
			else
				throw new IllegalStateException("Default already registered.");
		}
	}

	/**
	 * Get the default
	 * 
	 * @return
	 */
	public final static EffectiveUserFactory getDefault() {
		synchronized (lock) {
			if (instance == null) {
				new DefaultEffectiveUserFactory();
			}
		}
		return instance;
	}

	/**
	 * Get an effective user that may be used to execute a process with
	 * privileges of the provided username.
	 * 
	 * @param username
	 * @return effective user for username
	 */
	public abstract EffectiveUser getUserForUsername(String username);

	/**
	 * Get an effective user that may be used to execute a process with
	 * administrator privileges.
	 * 
	 * @return administrative effective user
	 */
	public abstract EffectiveUser administrator();

	/**
	 * Get an 'app name'. This is not strictly required to be anything sensible,
	 * but, if graphic elevation tools are invoked (gksu etc), then this is the
	 * friendly name that is displayed to this user.
	 * 
	 * @return app name
	 */
	public abstract String getAppName();

	/**
	 * Default implementation of {@link EffectiveUserFactory} that tries to
	 * detect the host operating environment (OS and desktop) and provide @{link
	 * {@link EffectiveUser} instances for forker to use for the actual
	 * elevation of privileges.
	 * 
	 * This implementation also tries to automatically detect the 'app name',
	 * that is used in the OS's privilege elevation dialog (such as gksu if
	 * available) by examining the stack and looking for the main() method. If
	 * your application bootstraps your application in a different way, or you
	 * simply want to display something other than the main class name, then set
	 * the system property <b>forker.app.name</b> or provide your own factory
	 * implementation that overrides {@link #getAppName()}.
	 */
	public static class DefaultEffectiveUserFactory extends EffectiveUserFactory {

		private String appName;

		public DefaultEffectiveUserFactory() {
			super(true);

			appName = System.getProperty("forker.app.name", null);
			if (appName == null) {
				String thisAppName = null;
				String threadAppName = null;
				for (Map.Entry<Thread, StackTraceElement[]> en : Thread.getAllStackTraces().entrySet()) {
					if (en.getValue().length > 0
							&& en.getValue()[en.getValue().length - 1].getMethodName().equals("main"))
						if (Thread.currentThread() == en.getKey())
							thisAppName = en.getValue()[en.getValue().length - 1].getClassName();
						else
							threadAppName = en.getValue()[en.getValue().length - 1].getClassName();
				}
				if (thisAppName == null)
					thisAppName = threadAppName;
				appName = thisAppName;
			}
			if (appName == null) {
				appName = "Java Application";
			}
		}

		@Override
		public EffectiveUser administrator() {
			String fixedPassword = getFixedPassword();
			if (SystemUtils.IS_OS_LINUX) {
				if (fixedPassword != null) {
					return new SudoFixedPasswordAdministrator(fixedPassword.toCharArray());
				} else {
					Desktop dt = OS.getDesktopEnvironment();
					if (Arrays.asList(Desktop.CINNAMON, Desktop.GNOME, Desktop.GNOME3).contains(dt)) {
						// Try gksudo first

						if (OSCommand.hasCommand("sudo")
								&& (OSCommand.hasCommand("gksudo") || OSCommand.hasCommand("gksu"))) {
							return new SudoGksudoAdministrator();
						} else if (OSCommand.hasCommand("sudo")) {
							return new SudoAskPassGuiAdministrator();
						} else if (OSCommand.hasCommand("gksudo") || OSCommand.hasCommand("gksu")) {
							/*
							 * Last resort, used Gksud/Gksu. These are a last
							 * resort because they do not support stdin
							 */
							return new GKAdministrator();
						}
					} else if (dt == Desktop.CONSOLE) {

						Console console = System.console();
						if (OSCommand.hasCommand("sudo") && console == null)
							return new SudoAskPassAdministrator();
						else {
							if (OSCommand.hasCommand("sudo") || OSCommand.hasCommand("su")) {
								return new SUAdministrator();
							}
						}
					}
				}
			} else if (SystemUtils.IS_OS_MAC_OSX) {
				if (fixedPassword != null) {
					return new SudoFixedPasswordAdministrator(fixedPassword.toCharArray());
				} else if (OSCommand.hasCommand("sudo")) {
					return new SudoAskPassGuiAdministrator();
				}
			} else if (SystemUtils.IS_OS_WINDOWS) {
				// http://mark.koli.ch/uac-prompt-from-java-createprocess-error740-the-requested-operation-requires-elevation
				if (fixedPassword != null) {
					return new RunAs(OS.getAdministratorUsername(), fixedPassword.toCharArray());
				} else {
					return new RunAs(OS.getAdministratorUsername());
				}
			}
			throw new UnsupportedOperationException(System.getProperty("os.name")
					+ " is currently unsupported. Will not be able to get administrative user. "
					+ "To hard code an adminstrator password, set the system property forker.administrator.password. "
					+ "This is unsafe, as the password will exist in a file for the life of the process. Do NOT use "
					+ "this in a production environment.");
		}

		@Override
		public EffectiveUser getUserForUsername(String username) {
			if (SystemUtils.IS_OS_LINUX) {
				Desktop dt = OS.getDesktopEnvironment();
				if (Arrays.asList(Desktop.CINNAMON, Desktop.GNOME, Desktop.GNOME3).contains(dt)) {
					// Try gksudo first
					if (OSCommand.hasCommand("gksudo") || OSCommand.hasCommand("gksu")) {
						return new GKUser(username);
					}
				} else {
					return new SUUser(username);
				}
			} else if (SystemUtils.IS_OS_MAC_OSX) {
				return new SUUser(username);
			} else if (SystemUtils.IS_OS_WINDOWS) {
				return new RunAs(username);
			}
			throw new UnsupportedOperationException(System.getProperty("os.name")
					+ " is currently unsupported. Will not be able to get UID for username.");
		}

		@Override
		public String getAppName() {
			return appName;
		}

		private String getFixedPassword() {
			String p = System.getProperty("forker.administrator.password");
			// For backwards compatibility
			return p == null ? System.getProperty("vm.sudo") : p;
		}

	}

	public static abstract class AbstractProcessBuilderEffectiveUser implements EffectiveUser {

	}

	public static class SUAdministrator extends AbstractProcessBuilderEffectiveUser implements EffectiveUser {

		@Override
		public void descend() {
		}

		@Override
		public void elevate(ForkerBuilder builder, Process process, Command command) {
			if (OSCommand.hasCommand("sudo")) {
				/*
				 * This is the only thing we can do to determine if to use sudo
				 * or not. /etc/shadow could not always be read to determine if
				 * root has a password which might be a hint. Neither could
				 * /etc/sudoers
				 */
				builder.command().add(0, "sudo");
			} else {
				if (System.console() == null)
					throw new IllegalStateException("This program requires elevated privileges, "
							+ "but sudo is not available, and the fallback 'su' is not capable of "
							+ "running without a controlling terminal.");
				List<String> cmd = builder.command();
				StringBuilder bui = Util.getQuotedCommandString(cmd);
				cmd.clear();
				cmd.add("su");
				cmd.add("-c");
				cmd.add(bui.toString());
			}
		}

	}

	public static class SudoFixedPasswordAdministrator extends AbstractProcessBuilderEffectiveUser
			implements EffectiveUser {

		File tempScript;

		public SudoFixedPasswordAdministrator(char[] password) {
			// Create a temporary script to use to launch AskPass
			try {
				tempScript = File.createTempFile("sfpa", ".sh");
				tempScript.deleteOnExit();
				OutputStream out = new FileOutputStream(tempScript);
				PrintWriter pw = new PrintWriter(out);
				try {
					pw.println("#!/bin/bash");
					pw.println("echo '" + new String(password).replace("'", "\\'") + "'");
					pw.flush();
				} finally {
					out.close();
				}

				tempScript.setExecutable(true);

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void descend() {
		}

		@Override
		public void elevate(ForkerBuilder builder, Process process, Command command) {
			builder.command().add(0, "sudo");
			builder.command().add(1, "-A");
			builder.environment().put("SUDO_ASKPASS", tempScript.getAbsolutePath());
		}

	}

	public static class SudoAskPassAdministrator extends AbstractProcessBuilderEffectiveUser implements EffectiveUser {

		static File tempScript;
		static {
			// Create a temporary script to use to launch AskPass
			try {
				tempScript = File.createTempFile("sapa", ".sh");
				tempScript.deleteOnExit();

				String javaExe = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
				if (SystemUtils.IS_OS_WINDOWS)
					javaExe += ".exe";

				String cp = null;
				String fullCp = System.getProperty("java.class.path", "");
				for (String p : fullCp.split(File.pathSeparator)) {
					if (p.contains("forker-client")) {
						cp = p;
					}
				}
				if (cp == null) {
					// Couldn't find just forker-common for some reason, just
					// add everything
					cp = fullCp;
				}

				OutputStream out = new FileOutputStream(tempScript);
				PrintWriter pw = new PrintWriter(out);
				try {
					pw.println("#!/bin/bash");
					pw.println("\"" + javaExe + "\" -classpath \"" + cp + "\" " + AskPassConsole.class.getName());
					pw.flush();
				} finally {
					out.close();
				}

				tempScript.setExecutable(true);

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void descend() {
		}

		@Override
		public void elevate(ForkerBuilder builder, Process process, Command command) {
			builder.command().add(0, "sudo");
			builder.command().add(1, "-A");
			builder.command().add(2, "-E");
			builder.environment().put("SUDO_ASKPASS", tempScript.getAbsolutePath());
		}

	}

	public static class SudoAskPassGuiAdministrator extends AbstractProcessBuilderEffectiveUser
			implements EffectiveUser {

		static File tempScript;
		static {
			// Create a temporary script to use to launch AskPass
			try {
				tempScript = File.createTempFile("sapa", ".sh");
				tempScript.deleteOnExit();

				String javaExe = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
				if (SystemUtils.IS_OS_WINDOWS)
					javaExe += ".exe";

				String cp = null;
				String fullCp = System.getProperty("java.class.path", "");
				for (String p : fullCp.split(File.pathSeparator)) {
					if (p.contains("forker-client")) {
						cp = p;
					}
				}
				if (cp == null) {
					// Couldn't find just forker-common for some reason, just
					// add everything
					cp = fullCp;
				}

				OutputStream out = new FileOutputStream(tempScript);
				PrintWriter pw = new PrintWriter(out);
				try {
					pw.println("#!/bin/bash");
					pw.println("\"" + javaExe + "\" -classpath \"" + cp + "\" " + AskPass.class.getName());
					pw.flush();
				} finally {
					out.close();
				}

				tempScript.setExecutable(true);

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void descend() {
		}

		@Override
		public void elevate(ForkerBuilder builder, Process process, Command command) {
			builder.command().add(0, "sudo");
			builder.command().add(1, "-A");
			builder.command().add(2, "-E");
			builder.environment().put("SUDO_ASKPASS", tempScript.getAbsolutePath());
		}

	}

	public static class SudoGksudoAdministrator extends AbstractProcessBuilderEffectiveUser implements EffectiveUser {

		static File tempScript;
		static {
			// Create a temporary script to use to launch AskPass
			try {
				tempScript = File.createTempFile("sapa", ".sh");
				tempScript.deleteOnExit();

				OutputStream out = new FileOutputStream(tempScript);
				PrintWriter pw = new PrintWriter(out);
				try {
					pw.println("#!/bin/bash");
					pw.println("gksudo --description=\"" + getDefault().getAppName() + "\" --print-pass");
					pw.flush();
				} finally {
					out.close();
				}

				tempScript.setExecutable(true);

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void descend() {
		}

		@Override
		public void elevate(ForkerBuilder builder, Process process, Command command) {
			builder.command().add(0, "sudo");
			builder.command().add(1, "-A");
			builder.command().add(2, "-E");
			builder.environment().put("SUDO_ASKPASS", tempScript.getAbsolutePath());
		}

	}

	/**
	 * Effective user implementation that raises privileges using sudo (or
	 * 'su'). Generally you would only use this in console applications.
	 */
	public static class SUUser extends AbstractProcessBuilderEffectiveUser implements EffectiveUser {

		private String username;

		public SUUser(String username) {
			this.username = username;
		}

		@Override
		public void descend() {
		}

		@Override
		public void elevate(ForkerBuilder builder, Process process, Command command) {
			if (OSCommand.hasCommand("sudo")) {
				/*
				 * This is the only thing we can do to determine if to use sudo
				 * or not. /etc/shadow could not always be read to determine if
				 * root has a password which might be a hint. Neither could
				 * /etc/sudoers
				 */
				builder.command().add(0, "sudo");
				builder.command().add(1, "-u");
				builder.command().add(2, username);
			} else {
				List<String> cmd = builder.command();
				StringBuilder bui = Util.getQuotedCommandString(cmd);
				cmd.clear();
				cmd.add("su");
				cmd.add("-c");
				cmd.add(bui.toString());
				cmd.add(username);
			}
		}

	}

	public static class GKUser extends AbstractProcessBuilderEffectiveUser implements EffectiveUser {

		private String username;

		public GKUser(String username) {
			this.username = username;
		}

		@Override
		public void descend() {
		}

		@Override
		public void elevate(ForkerBuilder builder, Process process, Command command) {
			List<String> cmd = builder.command();

			StringBuilder bui = Util.getQuotedCommandString(cmd);

			cmd.clear();
			if (OSCommand.hasCommand("gksudo"))
				cmd.add("gksudo");
			else if (OSCommand.hasCommand("gksu"))
				cmd.add("gksudo");
			cmd.add("--user");
			cmd.add(username);
			cmd.add(bui.toString());
		}

	}

	public static class RunAs extends AbstractProcessBuilderEffectiveUser implements EffectiveUser {

		private String username;
		private boolean setRemote;
		private String was;
		private Command command;
		private char[] password;

		public RunAs() {
			this(null);
		}

		public RunAs(String username) {
			this(username, null);
		}

		public RunAs(String username, char[] password) {
			this.username = username;
			this.password = password;
		}

		@Override
		public void descend() {
			if (setRemote) {
				setRemote = false;
				command.setRunAs(was);
			}
		}

		@Override
		public void elevate(ForkerBuilder builder, Process process, Command command) {
			if (process instanceof ForkerProcess) {
				if (setRemote)
					descend();
				was = command.getRunAs();
				this.command = command;
				command.setRunAs(username);
				setRemote = true;
			} else {
				List<String> cmd = builder.command();
				List<String> args = new ArrayList<>(cmd);
				cmd.clear();
				cmd.add(OS.getJavaPath());
				cmd.add("-classpath");
				cmd.add(Forker.getForkerClasspath());
				cmd.add(WinRunAs.class.getName());
				if (username.equals(OS.getAdministratorUsername()) && command.getIO() == IO.SINK) {
					cmd.add("--uac");
				} else {
					cmd.add("--username");
					cmd.add(username);
					if (password != null) {
						command.getEnvironment().put("W32RUNAS_PASSWORD", new String(password));
					}
				}
				cmd.add("--");
				cmd.addAll(args);
				System.out.println(cmd);
			}
		}
	}

	public static class GKAdministrator extends AbstractProcessBuilderEffectiveUser implements EffectiveUser {

		@Override
		public void descend() {
		}

		@Override
		public void elevate(ForkerBuilder builder, Process process, Command command) {
			if (command.getIO() == IO.IO || command.getIO() == IO.INPUT) {
				throw new RuntimeException(String.format("%s does not support stdin, so may not be used.", getClass()));
			}

			List<String> cmd = builder.command();

			// Take existing command and turn it into one escaped command
			StringBuilder bui = new StringBuilder();
			for (int i = 0; i < cmd.size(); i++) {
				if (bui.length() > 0) {
					bui.append(' ');
				}
				if (i > 0)
					bui.append("'");
				bui.append(Util.escapeSingleQuotes(cmd.get(i)));
				if (i > 0)
					bui.append("'");
			}

			cmd.clear();
			if (OSCommand.hasCommand("gksudo")) {
				cmd.add("gksudo");
				cmd.add("--sudo-mode");
				cmd.add("--preserve-env");
				cmd.add("--description");
				cmd.add(getDefault().getAppName());
			} else if (OSCommand.hasCommand("gksu")) {
				cmd.add("gksu");
				cmd.add("--su-mode");
				cmd.add("--preserve-env");
				cmd.add("--description");
				cmd.add(getDefault().getAppName());
			}
			cmd.add(bui.toString());
		}
	}

	static abstract class AbstractPOSIXEffectiveUser<T> extends AbstractProcessBuilderEffectiveUser {

		private T value;
		private int was = Integer.MIN_VALUE;
		private boolean setRemote;

		AbstractPOSIXEffectiveUser(T value) {
			this.value = value;
		}

		@Override
		public void elevate(ForkerBuilder builder, Process process, Command command) {
			if (process instanceof ForkerProcess) {
				command.setRunAs(String.valueOf(value));
				setRemote = true;
			} else {
				if (was != Integer.MIN_VALUE)
					throw new IllegalStateException();
				was = CSystem.INSTANCE.geteuid();
				doSet(value);
			}
		}

		public T getValue() {
			return value;
		}

		abstract void doSet(T value);

		@Override
		public synchronized void descend() {
			if (setRemote) {
				setRemote = false;
			} else {
				if (was == Integer.MIN_VALUE)
					throw new IllegalStateException();
				seteuid(was);
				was = Integer.MIN_VALUE;
			}
		}

		protected void seteuid(int euid) {
			if (CSystem.INSTANCE.seteuid(euid) == -1) {
				// TODO get errono
				throw new RuntimeException("Failed to set EUID.");
			}
			;
		}

	}

	public static class POSIXUIDEffectiveUser extends AbstractPOSIXEffectiveUser<Integer> {

		public POSIXUIDEffectiveUser(int uid) {
			super(uid);
		}

		@Override
		void doSet(Integer value) {
			seteuid(value);
		}

	}

	public static class POSIXUsernameEffectiveUser extends AbstractPOSIXEffectiveUser<String> {

		public POSIXUsernameEffectiveUser(String username) {
			super(username);
		}

		@Override
		void doSet(String value) {
			try {
				seteuid(Integer.parseInt(Util.getIDForUsername(value)));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

	}
}
