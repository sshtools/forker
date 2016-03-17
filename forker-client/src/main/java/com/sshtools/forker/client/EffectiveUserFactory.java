package com.sshtools.forker.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.SystemUtils;

import com.sshtools.forker.client.OS.Desktop;
import com.sshtools.forker.client.impl.ForkerProcess;
import com.sshtools.forker.client.ui.AskPass;
import com.sshtools.forker.common.CSystem;
import com.sshtools.forker.common.Command;
import com.sshtools.forker.common.Util;

public abstract class EffectiveUserFactory {

	private final static Object lock = new Object();
	private static EffectiveUserFactory instance;
	
	/**
	 * 
	 */
	protected EffectiveUserFactory(boolean registerAsDefault) {
		if(registerAsDefault) {
			if(instance == null)
				instance = this;
			else
				throw new IllegalStateException("Default already registered.");
		}
	}

	/**
	 * Get the default 
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
		}

		@Override
		public EffectiveUser administrator() {
			if (SystemUtils.IS_OS_LINUX) {
				Desktop dt = OS.getDesktopEnvironment();
				if (Arrays.asList(Desktop.CINNAMON, Desktop.GNOME, Desktop.GNOME3).contains(dt)) {
					// Try gksudo first
					if (OS.hasCommand("gksudo") || OS.hasCommand("gksu")) {
						return new GKAdministrator();
					}
					else  if(OS.hasCommand("sudo")) {
						return new SudoAskPassAdministrator();
					}
				} else if (dt == Desktop.CONSOLE) {
					if (OS.hasCommand("sudo") || OS.hasCommand("su")) {
						return new SUAdministrator();
					}
				}
			}
			else if (SystemUtils.IS_OS_MAC_OSX) {
				if(OS.hasCommand("sudo")) {
					return new SudoAskPassAdministrator();
				}
			}
			throw new UnsupportedOperationException(System.getProperty("os.name")
					+ " is currently unsupported. Will not be able to get administrative user.");
		}

		@Override
		public EffectiveUser getUserForUsername(String username) {
			if (SystemUtils.IS_OS_LINUX) {
				Desktop dt = OS.getDesktopEnvironment();
				if (Arrays.asList(Desktop.CINNAMON, Desktop.GNOME, Desktop.GNOME3).contains(dt)) {
					// Try gksudo first
					if (OS.hasCommand("gksudo") || OS.hasCommand("gksu")) {
						return new GKUser(username);
					}
				} else if (dt == Desktop.CONSOLE) {
					return new SUUser(username);
				}
			}
			throw new UnsupportedOperationException(System.getProperty("os.name")
					+ " is currently unsupported. Will not be able to get UID for username.");
		}

		@Override
		public String getAppName() {
			return appName;
		}

	}

	public static class SUAdministrator implements EffectiveUser {

		@Override
		public void descend() {
		}

		@Override
		public void elevate(ForkerBuilder builder, Process process, Command command) {
			if (OS.hasCommand("sudo")) {
				/*
				 * This is the only thing we can do to determine if to use sudo
				 * or not. /etc/shadow could not always be read to determine if
				 * root has a password which might be a hint. Neither could
				 * /etc/sudoers
				 */
				builder.command().add(0, "sudo");
			} else {
				List<String> cmd = builder.command();
				StringBuilder bui = getQuotedCommandString(cmd);
				cmd.clear();
				cmd.add("su");
				cmd.add("-c");
				cmd.add(bui.toString());
			}
		}

	}
	
	public static class SudoAskPassAdministrator implements EffectiveUser {
		
		static File tempScript;
		static {
			// Create a temporary script to use to launch AskPass
			try {
				tempScript = File.createTempFile("sapa", ".sh");
				tempScript.deleteOnExit();

				String javaExe = System.getProperty("java.home") + File.separator + "bin"
						+ File.separator + "java";
				if (SystemUtils.IS_OS_WINDOWS)
					javaExe += ".exe";
				
				String cp = null;
				String fullCp = System.getProperty("java.class.path", "");
				for (String p : fullCp.split(File.pathSeparator)) {
					if(p.contains("forker-client")) {
						cp = p;
					}
				}
				if(cp == null) {
					// Couldn't find just forker-common for some reason, just add everything
					cp = fullCp;
				}
				
				OutputStream out = new FileOutputStream(tempScript);
				PrintWriter pw = new PrintWriter(out);
				try {
					pw.println("#!/bin/bash");
					pw.println("\"" + javaExe + "\" -classpath \"" + cp + "\" " + AskPass.class.getName());
					pw.flush();
				}
				finally {
					out.close();
				}
				
				tempScript.setExecutable(true);
				
			}
			catch(Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void descend() {
		}

		@Override
		public void elevate(ForkerBuilder builder, Process process, Command command) {
			builder.command().add(0, "sudo");
			if(SystemUtils.IS_OS_MAC_OSX) {
				builder.command().add(1, "-A");
			}
			builder.environment().put("SUDO_ASKPASS", tempScript.getAbsolutePath());
		}

	}

	/**
	 * Effective user implementation that raises privileges using sudo (or
	 * 'su'). Generally you would only use this in console applications.
	 */
	public static class SUUser implements EffectiveUser {

		private String username;

		public SUUser(String username) {
			this.username = username;
		}

		@Override
		public void descend() {
		}

		@Override
		public void elevate(ForkerBuilder builder, Process process, Command command) {
			if (OS.hasCommand("sudo")) {
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
				StringBuilder bui = getQuotedCommandString(cmd);
				cmd.clear();
				cmd.add("su");
				cmd.add("-c");
				cmd.add(bui.toString());
				cmd.add(username);
			}
		}

	}

	public static class GKUser implements EffectiveUser {

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

			StringBuilder bui = getQuotedCommandString(cmd);

			cmd.clear();
			if (OS.hasCommand("gksudo"))
				cmd.add("gksudo");
			else if (OS.hasCommand("gksu"))
				cmd.add("gksudo");
			cmd.add("--user");
			cmd.add(username);
			cmd.add(bui.toString());
		}

	}

	public static class GKAdministrator implements EffectiveUser {

		@Override
		public void descend() {
		}

		@Override
		public void elevate(ForkerBuilder builder, Process process, Command command) {
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
			if (OS.hasCommand("gksudo")) {
				cmd.add("gksudo");
				cmd.add("--description");
				cmd.add(getDefault().getAppName());
			} else if (OS.hasCommand("gksu")) {
				cmd.add("gksu");
				cmd.add("--description");
				cmd.add(getDefault().getAppName());
			}
			cmd.add(bui.toString());
		}
	}

	public static class POSIXEffectiveUser implements EffectiveUser {

		private int uid;
		private int was = Integer.MIN_VALUE;
		private boolean setRemote;

		public POSIXEffectiveUser(int uid) {
			this.uid = uid;
		}

		public int getUID() {
			return uid;
		}

		@Override
		public void elevate(ForkerBuilder builder, Process process, Command command) {
			if (process instanceof ForkerProcess) {
				command.setRunAs(String.valueOf(getUID()));
				setRemote = true;
			} else {
				if (was != Integer.MIN_VALUE)
					throw new IllegalStateException();
				was = CSystem.INSTANCE.geteuid();
				doSeteuid(uid);
			}
		}

		@Override
		public synchronized void descend() {
			if (setRemote) {
				setRemote = false;
			} else {
				if (was == Integer.MIN_VALUE)
					throw new IllegalStateException();
				doSeteuid(was);
				was = Integer.MIN_VALUE;
			}
		}

		private void doSeteuid(int euid) {
			if (CSystem.INSTANCE.seteuid(euid) == -1) {
				// TODO get errono
				throw new RuntimeException("Failed to set EUID.");
			}
			;
		}

	}

	private static StringBuilder getQuotedCommandString(List<String> cmd) {
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
		return bui;
	}
}
