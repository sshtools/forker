package com.sshtools.forker.client;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.SystemUtils;

import com.sshtools.forker.client.OS.Desktop;
import com.sshtools.forker.client.impl.CSystem;

public abstract class EffectiveUserFactory {

	private final static Object lock = new Object();
	private static EffectiveUserFactory instance;

	public final static EffectiveUserFactory getDefault() {
		synchronized (lock) {
			if (instance == null) {
				instance = new DefaultEffectiveUserFactory();
			}
		}
		return instance;
	}

	public abstract EffectiveUser getUserForUsername(String username);

	public abstract EffectiveUser administrator();

	public static class DefaultEffectiveUserFactory extends
			EffectiveUserFactory {

		@Override
		public EffectiveUser administrator() {
			if (SystemUtils.IS_OS_LINUX) {
				Desktop dt = OS.getDesktopEnvironment();
				if (Arrays.asList(Desktop.CINNAMON, Desktop.GNOME,
						Desktop.GNOME3).contains(dt)) {
					// Try gksudo first
					if (OS.hasCommand("gksudo") || OS.hasCommand("gksu")) {
						return new GKAdministrator();
					}
				} else if (dt == Desktop.CONSOLE) {
					if(OS.hasCommand("sudo") || OS.hasCommand("su")) {
						return new SUAdministrator();
					}
				}
			}
			throw new UnsupportedOperationException(
					System.getProperty("os.name")
							+ " is currently unsupported. Will not be able to get administrative user.");
		}

		@Override
		public EffectiveUser getUserForUsername(String username) {
			if (SystemUtils.IS_OS_LINUX) {
				Desktop dt = OS.getDesktopEnvironment();
				if (Arrays.asList(Desktop.CINNAMON, Desktop.GNOME,
						Desktop.GNOME3).contains(dt)) {
					// Try gksudo first
					if (OS.hasCommand("gksudo") || OS.hasCommand("gksu")) {
						return new GKUser(username);
					}
				} else if (dt == Desktop.CONSOLE) {
					return new SUUser(username);
				}
			}
			throw new UnsupportedOperationException(
					System.getProperty("os.name")
							+ " is currently unsupported. Will not be able to get UID for username.");
		}

	}

	public class SUAdministrator implements EffectiveUser {

		@Override
		public void descend() {
		}

		@Override
		public void elevate(ForkerBuilder builder, Process process) {
			if (OS.hasCommand("sudo")) {
				/* This is the only thing we can do to determine if to use sudo or not. /etc/shadow could
				 * not always be read to determine if root has a password which might be a hint. Neither could
				 * /etc/sudoers
				 */
				builder.command().add(0, "sudo");
			}
			else {
				List<String> cmd = builder.command();
				StringBuilder bui = getQuotedCommandString(cmd);
				cmd.clear();
				cmd.add("su");
				cmd.add("-c");
				cmd.add(bui.toString());
			}
		}

	}

	public class SUUser implements EffectiveUser {
		
		private String username;

		public SUUser(String username) {
			this.username = username;
		}

		@Override
		public void descend() {
		}

		@Override
		public void elevate(ForkerBuilder builder, Process process) {
			if (OS.hasCommand("sudo")) {
				/* This is the only thing we can do to determine if to use sudo or not. /etc/shadow could
				 * not always be read to determine if root has a password which might be a hint. Neither could
				 * /etc/sudoers
				 */
				builder.command().add(0, "sudo");
				builder.command().add(1, "-u");
				builder.command().add(2, username);
			}
			else {
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

	public class GKUser implements EffectiveUser {

		private String username;

		public GKUser(String username) {
			this.username = username;
		}
		
		@Override
		public void descend() {
		}

		@Override
		public void elevate(ForkerBuilder builder, Process process) {
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

	public class GKAdministrator implements EffectiveUser {

		@Override
		public void descend() {
		}

		@Override
		public void elevate(ForkerBuilder builder, Process process) {
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
			if (OS.hasCommand("gksudo"))
				cmd.add("gksudo");
			else if (OS.hasCommand("gksu"))
				cmd.add("gksudo");
			cmd.add(bui.toString());
		}
	}

	public class POSIXEffectiveUser implements EffectiveUser {

		private int uid;
		private int was = Integer.MIN_VALUE;

		public POSIXEffectiveUser(int uid) {
			this.uid = uid;
		}

		public int getUID() {
			return uid;
		}

		@Override
		public void elevate(ForkerBuilder builder, Process process) {
			if (was != Integer.MIN_VALUE)
				throw new IllegalStateException();
			was = CSystem.INSTANCE.geteuid();
			doSeteuid(uid);
		}

		@Override
		public synchronized void descend() {
			if (was == Integer.MIN_VALUE)
				throw new IllegalStateException();
			doSeteuid(was);
			was = Integer.MIN_VALUE;
		}

		private void doSeteuid(int euid) {
			if (CSystem.INSTANCE.seteuid(was) == -1) {
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
