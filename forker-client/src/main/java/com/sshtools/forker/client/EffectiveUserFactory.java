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

import com.sshtools.forker.client.impl.ForkerDaemonProcess;
import com.sshtools.forker.client.ui.AskPass;
import com.sshtools.forker.client.ui.AskPassConsole;
import com.sshtools.forker.client.ui.WinRunAs;
import com.sshtools.forker.common.CSystem;
import com.sshtools.forker.common.Command;
import com.sshtools.forker.common.IO;
import com.sshtools.forker.common.OS;
import com.sshtools.forker.common.OS.Desktop;
import com.sshtools.forker.common.Util;

/**
 * Responsible for creating {@link EffectiveUser} objects for use with
 * {@link ForkerBuilder#effectiveUser(EffectiveUser)} to raise privileges of
 * processes or run them as another user.
 * <p>
 * The class is abstract,allowing client code to provide their own
 * implementations if required, but for most cases
 * {@link DefaultEffectiveUserFactory} will suffice.
 * <p>
 * The two primary methods are, {@link EffectiveUserFactory#administrator()} to
 * obtain an administrator effective user, or
 * {@link EffectiveUserFactory#getUserForUsername(String)} to obtain one for a
 * particular username.
 * <p>
 * Client code can if it wish simply create instances of {@link EffectiveUser}
 * implementations such as {@link SUUser} directly.
 * <h2>Example</h2>
 * 
 * <pre>
 * <code>
 * ForkerBuilder fb = new ForkerBuilder("cat", "/etc/shadow");
 * fb.effectiveUser(EffectiveUserFactory.getDefault().administrator());
 * </code>
 * </pre>
 *
 */
public abstract class EffectiveUserFactory {
	private static EffectiveUserFactory instance;
	private final static Object lock = new Object();

	protected EffectiveUserFactory(boolean registerAsDefault) {
		if (registerAsDefault) {
			if (instance == null)
				instance = this;
			else
				throw new IllegalStateException("Default already registered.");
		}
	}

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
	 * Get an effective user that may be used to execute a process with
	 * privileges of the provided username.
	 * 
	 * @param username username
	 * @return effective user for username
	 */
	public abstract EffectiveUser getUserForUsername(String username);

	/**
	 * Get the default {@link EffectiveUserFactory} that should be appropriate
	 * for most common uses.
	 * 
	 * @return default effective user factory
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
	 * Abstract implementation of an {@link EffectiveUser} that can be used to
	 * create a temporary script that will be used as part of the privilege
	 * escalation or user switching process.
	 *
	 */
	public static abstract class AbstractProcessBuilderEffectiveUser implements EffectiveUser {
		File tempScript;

		protected void createTempScript(String script) {
			// Create a temporary script to use to launch AskPass
			try {
				tempScript = File.createTempFile("sapa", ".sh");
				tempScript.deleteOnExit();
				OutputStream out = new FileOutputStream(tempScript);
				PrintWriter pw = new PrintWriter(out);
				try {
					pw.println("#!/bin/bash");
					pw.println(script);
					pw.println("ret=$?");
					pw.println("rm -f '" + tempScript.getAbsolutePath() + "'");
					pw.println("exit ${ret}");
					pw.flush();
				} finally {
					out.close();
				}
				tempScript.setExecutable(true);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		protected String javaAskPassScript(Class<?> clazz) {
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
			return "\"" + javaExe + "\" -classpath \"" + cp + "\" " + clazz.getName();
		}
	}

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

		/**
		 * Constructor
		 */
		public DefaultEffectiveUserFactory() {
			super(true);
			appName = System.getProperty("forker.app.name", null);
			if (appName == null) {
				String thisAppName = null;
				String threadAppName = null;
				for (Map.Entry<Thread, StackTraceElement[]> en : Thread.getAllStackTraces().entrySet()) {
					if (en.getValue().length > 0 && en.getValue()[en.getValue().length - 1].getMethodName().equals("main"))
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
			/*
			 * Wrap the effective user that tests at run time whether it's
			 * actually needed. This is required be we do not know up front if
			 * the process will be handled by the daemon
			 */
			final EffectiveUser user = createAdministrator();
			return new EffectiveUser() {
				@Override
				public void descend(ForkerBuilder builder, Process process, Command command) {
					if ((Forker.isDaemonRunning() && !Forker.isDaemonRunningAsAdministrator()
							&& process instanceof ForkerDaemonProcess)
							|| (!OS.isAdministrator() && !(process instanceof ForkerDaemonProcess)))
						user.descend(builder, process, command);
				}

				@Override
				public void elevate(ForkerBuilder builder, Process process, Command command) {
					if ((Forker.isDaemonRunning() && !Forker.isDaemonRunningAsAdministrator()
							&& process instanceof ForkerDaemonProcess)
							|| (!OS.isAdministrator() && !(process instanceof ForkerDaemonProcess)))
						user.elevate(builder, process, command);
				}
			};
		}

		@Override
		public String getAppName() {
			return appName;
		}

		@Override
		public EffectiveUser getUserForUsername(String username) {
			if (SystemUtils.IS_OS_LINUX) {
				// If already administrator, just su or sudo should be
				// sufficient is no password will be required
				if (OS.isAdministrator()) {
					return new SUUser(username);
				} else {
					Desktop dt = OS.getDesktopEnvironment();
					if (Arrays.asList(Desktop.CINNAMON, Desktop.GNOME, Desktop.GNOME3).contains(dt)) {
						// Try gksudo first
						if (OSCommand.hasCommand("sudo") && (OSCommand.hasCommand("gksudo") || OSCommand.hasCommand("gksu"))) {
							return new SudoGksudoUser(username);
						} else if (OSCommand.hasCommand("sudo")) {
							return new SudoAskPassGuiUser();
						} else if (OSCommand.hasCommand("gksudo") || OSCommand.hasCommand("gksu")) {
							/*
							 * Last resort, used Gksud/Gksu. These are a last
							 * resort because they do not support stdin
							 */
							return new GKSuUser(username);
						} else if (OSCommand.hasCommand("pkexec")) {
						}
					} else if (dt == Desktop.CONSOLE) {
						Console console = System.console();
						if (OSCommand.hasCommand("sudo") && console == null)
							return new SudoAskPassUser(username);
						else {
							if (OSCommand.hasCommand("sudo") || OSCommand.hasCommand("su")) {
								return new SUUser(username);
							}
						}
					}
				}
			} else if (SystemUtils.IS_OS_MAC_OSX) {
				return new SUUser(username);
			} else if (SystemUtils.IS_OS_WINDOWS) {
				return new RunAsUser(username);
			}
			throw new UnsupportedOperationException(
					System.getProperty("os.name") + " is currently unsupported. Will not be able to get UID for username.");
		}

		protected EffectiveUser createAdministrator() {
			String fixedPassword = getFixedPassword();
			if (SystemUtils.IS_OS_LINUX) {
				if (fixedPassword != null) {
					return new SudoFixedPasswordUser(fixedPassword.toCharArray());
				} else {
					Desktop dt = OS.getDesktopEnvironment();
					if (Arrays.asList(Desktop.CINNAMON, Desktop.GNOME, Desktop.GNOME3).contains(dt)) {
						// Try gksudo first
						if (OSCommand.hasCommand("sudo") && (OSCommand.hasCommand("gksudo") || OSCommand.hasCommand("gksu"))) {
							return new SudoGksudoUser();
						} else if (OSCommand.hasCommand("sudo")) {
							return new SudoAskPassGuiUser();
						} else if (OSCommand.hasCommand("gksudo") || OSCommand.hasCommand("gksu")) {
							/*
							 * Last resort, used Gksud/Gksu. These are a last
							 * resort because they do not support stdin
							 */
							return new GKSuUser();
						}
					} else if (dt == Desktop.CONSOLE) {
						Console console = System.console();
						if (OSCommand.hasCommand("sudo") && console == null)
							return new SudoAskPassUser();
						else {
							if (OSCommand.hasCommand("sudo") || OSCommand.hasCommand("su")) {
								return new SUAdministrator();
							}
						}
					} else {
						// Unknown desktop
						return new SudoAskPassGuiUser();
					}
				}
			} else if (SystemUtils.IS_OS_MAC_OSX) {
				if (fixedPassword != null) {
					return new SudoFixedPasswordUser(fixedPassword.toCharArray());
				} else if (OSCommand.hasCommand("sudo")) {
					return new SudoAskPassGuiUser();
				}
			} else if (SystemUtils.IS_OS_WINDOWS) {
				// http://mark.koli.ch/uac-prompt-from-java-createprocess-error740-the-requested-operation-requires-elevation
				if (fixedPassword != null) {
					return new RunAsUser(OS.getAdministratorUsername(), fixedPassword.toCharArray());
				} else {
					return new RunAsUser(OS.getAdministratorUsername());
				}
			}
			return new EffectiveUser() {
				@Override
				public void descend(ForkerBuilder builder, Process process, Command command) {
				}

				@Override
				public void elevate(ForkerBuilder builder, Process process, Command command) {
					throw new UnsupportedOperationException(System.getProperty("os.name")
							+ " is currently unsupported. Will not be able to get administrative user. "
							+ "To hard code an adminstrator password, set the system property forker.administrator.password. "
							+ "This is unsafe, as the password will exist in a file for the life of the process. Do NOT use "
							+ "this in a production environment.");
				}
			};
		}

		private String getFixedPassword() {
			String p = System.getProperty("forker.administrator.password");
			// For backwards compatibility
			return p == null ? System.getProperty("vm.sudo") : p;
		}
	}

	/**
	 * An {@link EffectiveUser} implementation that uses a combination
	 * GkSu/Gksudo. Note, this method uses Gksu/Gksudo to do the actual
	 * launching, but this means stdin is not supported. In order to use
	 * Gksu/Gksudo and have stdin, {@link SudoGksudoUser} should be used
	 * instead.
	 *
	 */
	public static class GKSuUser extends AbstractProcessBuilderEffectiveUser implements EffectiveUser {
		private ArrayList<String> original;
		private String username;

		/**
		 * Constructor for administrator (root)
		 */
		public GKSuUser() {
		}

		/**
		 * Constructor for specific username
		 * 
		 * @param username username
		 */
		public GKSuUser(String username) {
			this.username = username;
		}

		@Override
		public void descend(ForkerBuilder builder, Process process, Command command) {
			builder.command().clear();
			builder.command().addAll(original);
		}

		@Override
		public void elevate(ForkerBuilder builder, Process process, Command command) {
			original = new ArrayList<String>(builder.command());
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
				if (username != null) {
					cmd.add("-u");
					cmd.add(username);
				}
				cmd.add("--sudo-mode");
				cmd.add("--preserve-env");
				cmd.add("--description");
				cmd.add(getDefault().getAppName());
			} else if (OSCommand.hasCommand("gksu")) {
				cmd.add("gksu");
				if (username != null) {
					cmd.add("-u");
					cmd.add(username);
				}
				cmd.add("--su-mode");
				cmd.add("--preserve-env");
				cmd.add("--description");
				cmd.add(getDefault().getAppName());
			}
			cmd.add(bui.toString());
		}
	}

	/**
	 * An {@link EffectiveUser} implementation that uses PkExec, which uses is a
	 * very restrictive default method that is now recommended in Freedesktop
	 * environments such as GNOME etc.
	 *
	 * By default it cannot run graphical applications as neither DISPLAY or
	 * XAUTH are passed.
	 * 
	 * Standard streams are supported.
	 */
	public static class PkExecUser extends AbstractProcessBuilderEffectiveUser implements EffectiveUser {
		private ArrayList<String> original;
		private String username;

		/**
		 * Constructor for administrator (root)
		 */
		public PkExecUser() {
		}

		/**
		 * Constructor for specific username
		 * 
		 * @param username username
		 */
		public PkExecUser(String username) {
			this.username = username;
		}

		@Override
		public void descend(ForkerBuilder builder, Process process, Command command) {
			builder.command().clear();
			builder.command().addAll(original);
		}

		@Override
		public void elevate(ForkerBuilder builder, Process process, Command command) {
			original = new ArrayList<String>(builder.command());
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
			cmd.add("pkexec");
			if (username != null) {
				cmd.add("--user");
				cmd.add(username);
			}
			cmd.add(bui.toString());
		}
	}

	/**
	 * Effective user that does nothing. May be used when a request has been
	 * made to run as administrator, but the current process is already
	 * administrator.
	 *
	 */
	public class NullEffectiveUser implements EffectiveUser {
		@Override
		public void descend(ForkerBuilder builder, Process process, Command command) {
		}

		@Override
		public void elevate(ForkerBuilder builder, Process process, Command command) {
		}
	}

	/**
	 * An {@link EffectiveUser} implementation for POSIX systems that take a
	 * numeric UID to run the process as.
	 */
	public static class POSIXUIDEffectiveUser extends AbstractPOSIXEffectiveUser<Integer> {
		/**
		 * Constructor for particular UID.
		 * 
		 * @param uid uiid
		 */
		public POSIXUIDEffectiveUser(int uid) {
			super(uid);
		}

		@Override
		void doSet(Integer value) {
			seteuid(value);
		}
	}

	/**
	 * An {@link EffectiveUser} implementation for POSIX systems that take a
	 * username and turned it into a numeric UID to run the process as.
	 */
	public static class POSIXUsernameEffectiveUser extends AbstractPOSIXEffectiveUser<String> {
		/**
		 * Constructor for specific username
		 * 
		 * @param username username
		 */
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

	/**
	 * An {@link EffectiveUser} implementation that uses {@link WinRunAs}, a
	 * simple privilege escalation helper tool.
	 *
	 */
	public static class RunAsUser extends AbstractProcessBuilderEffectiveUser implements EffectiveUser {
		private ArrayList<String> original;
		private char[] password;
		private boolean setRemote;
		private String username;
		private String was;

		/**
		 * Constructor for Administrator
		 */
		public RunAsUser() {
			this(null);
		}

		/**
		 * Constructor for specific username
		 * 
		 * @param username username
		 */
		public RunAsUser(String username) {
			this(username, null);
		}

		/**
		 * Constructor for specific username and fixed password
		 * 
		 * @param username username
		 * @param password password
		 */
		public RunAsUser(String username, char[] password) {
			this.username = username;
			this.password = password;
		}

		@Override
		public void descend(ForkerBuilder builder, Process process, Command command) {
			if (setRemote) {
				setRemote = false;
				command.setRunAs(was);
			} else {
				builder.command().clear();
				builder.command().addAll(original);
			}
		}

		@Override
		public void elevate(ForkerBuilder builder, Process process, Command command) {
			if (process instanceof ForkerDaemonProcess) {
				if (setRemote)
					descend(builder, process, command);
				was = command.getRunAs();
				command.setRunAs(username);
				setRemote = true;
			} else {
				List<String> cmd = builder.command();
				original = new ArrayList<String>(cmd);
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
				cmd.addAll(original);
				System.out.println(cmd);
			}
		}
	}

	/**
	 * An effective user that uses that 'su' command to raise privileges to
	 * administrator.
	 *
	 */
	public static class SUAdministrator extends AbstractProcessBuilderEffectiveUser implements EffectiveUser {
		private ArrayList<String> original;

		@Override
		public void descend(ForkerBuilder builder, Process process, Command command) {
			builder.command().clear();
			builder.command().addAll(original);
		}

		@Override
		public void elevate(ForkerBuilder builder, Process process, Command command) {
			original = new ArrayList<String>(builder.command());
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

	/**
	 * An {@link EffectiveUser} implementation that uses the 'sudo' command and
	 * the {@link AskPass} application to request a password for a particular
	 * user. The advantage of this over plain sudo based implementations, is
	 * that a friendly GUI is displayed with more descriptive text.
	 */
	public static class SudoAskPassGuiUser extends AbstractProcessBuilderEffectiveUser implements EffectiveUser {
		private String username;

		/**
		 * Constructor for administrator (root)
		 */
		public SudoAskPassGuiUser() {
		}

		/**
		 * Constructor for specific user
		 * 
		 * @param username username
		 */
		public SudoAskPassGuiUser(String username) {
			this();
			this.username = username;
		}

		@Override
		public void descend(ForkerBuilder builder, Process process, Command command) {
			builder.command().remove(0);
			builder.command().remove(0);
			builder.command().remove(0);
			if (username != null) {
				builder.command().remove(0);
				builder.command().remove(0);
			}
			builder.environment().remove("SUDO_ASKPASS");
		}

		@Override
		public void elevate(ForkerBuilder builder, Process process, Command command) {
			createTempScript(javaAskPassScript(AskPass.class));
			builder.command().add(0, "sudo");
			builder.command().add(1, "-A");
			builder.command().add(2, "-E");
			if (username != null) {
				builder.command().add(3, "-u");
				builder.command().add(4, username);
			}
			builder.environment().put("SUDO_ASKPASS", tempScript.getAbsolutePath());
		}
	}

	/**
	 * An {@link EffectiveUser} implementation that uses the 'sudo' command and
	 * the {@link AskPassConsole} application to request a password for a
	 * particular user. The advantage of this over plain sudo based
	 * implementations, is that a more descriptive text may be displayed, and it
	 * works without a tty (although may echo the password).
	 */
	public static class SudoAskPassUser extends AbstractProcessBuilderEffectiveUser implements EffectiveUser {
		private String username;

		/**
		 * Constructor for administrator (root)
		 */
		public SudoAskPassUser() {
		}

		/**
		 * Constructor for specific user
		 * 
		 * @param username username
		 */
		public SudoAskPassUser(String username) {
			this();
			this.username = username;
		}

		@Override
		public void descend(ForkerBuilder builder, Process process, Command command) {
			builder.command().remove(0);
			builder.command().remove(0);
			builder.command().remove(0);
			if (username != null) {
				builder.command().remove(0);
				builder.command().remove(0);
			}
			builder.environment().remove("SUDO_ASKPASS");
		}

		@Override
		public void elevate(ForkerBuilder builder, Process process, Command command) {
			createTempScript(javaAskPassScript(AskPassConsole.class));
			builder.command().add(0, "sudo");
			builder.command().add(1, "-A");
			builder.command().add(2, "-E");
			if (username != null) {
				builder.command().add(3, "-u");
				builder.command().add(4, username);
			}
			builder.environment().put("SUDO_ASKPASS", tempScript.getAbsolutePath());
		}
	}

	/**
	 * An {@link EffectiveUser} implementation that uses the 'sudo' command to
	 * either escalate privileges to administrator or switch to another user
	 * using a <b>fixed</b> password. <b>This is not secure</b> and should only
	 * be used for automated processes where security is not a consideration or
	 * is provided by some other mechanism, or for testing and development.
	 */
	public static class SudoFixedPasswordUser extends AbstractProcessBuilderEffectiveUser implements EffectiveUser {
		private char[] password;
		private String username;

		/**
		 * Constructor for administrator (root) and a fixed password
		 * 
		 * @param password fixed password
		 */
		public SudoFixedPasswordUser(char[] password) {
			this(null, password);
		}

		/**
		 * Constructor for a particular user and a fixed password
		 * 
		 * @param username username
		 * @param password fixed password
		 */
		public SudoFixedPasswordUser(String username, char[] password) {
			this.username = username;
			this.password = password;
		}

		@Override
		public void descend(ForkerBuilder builder, Process process, Command command) {
			builder.command().remove(0);
			builder.command().remove(0);
			if (username != null) {
				builder.command().remove(0);
				builder.command().remove(0);
			}
			builder.environment().remove("SUDO_ASKPASS");
		}

		@Override
		public void elevate(ForkerBuilder builder, Process process, Command command) {
			createTempScript("echo '" + new String(password).replace("'", "\\'") + "'");
			builder.command().add(0, "sudo");
			builder.command().add(1, "-A");
			if (username != null) {
				builder.command().add(2, "-u");
				builder.command().add(3, username);
			}
			builder.environment().put("SUDO_ASKPASS", tempScript.getAbsolutePath());
		}
	}

	/**
	 * An {@link EffectiveUser} implementation that uses a combination of Sudo
	 * and Gksudo. Note, we use both of these tools because Gksudo/Gksu do not
	 * support stdin. However 'sudo' does, and Gksudo has a print-pass option
	 * that can make it act in the way required by the 'SUDO_ASKPASS'.
	 *
	 */
	public static class SudoGksudoUser extends AbstractProcessBuilderEffectiveUser implements EffectiveUser {
		private String username;

		/**
		 * Constructor for administrator (root)
		 */
		public SudoGksudoUser() {
		}

		/**
		 * Constructor for specific user
		 * 
		 * @param username username
		 */
		public SudoGksudoUser(String username) {
			this.username = username;
		}

		@Override
		public void descend(ForkerBuilder builder, Process process, Command command) {
			builder.command().remove(0);
			builder.command().remove(0);
			builder.command().remove(0);
			if (username != null) {
				builder.command().remove(0);
				builder.command().remove(0);
			}
			builder.environment().remove("SUDO_ASKPASS");
		}

		@Override
		public void elevate(ForkerBuilder builder, Process process, Command command) {
			createTempScript(String.format("gksudo --description=\"%s\" --print-pass", getDefault().getAppName()));
			builder.command().add(0, "sudo");
			builder.command().add(1, "-A");
			builder.command().add(2, "-E");
			if (username != null) {
				builder.command().add(3, "-u");
				builder.command().add(4, username);
			}
			builder.environment().put("SUDO_ASKPASS", tempScript.getAbsolutePath());
		}
	}

	/**
	 * Effective user implementation that raises privileges using sudo (or
	 * 'su'). Generally you would only use this in console applications.
	 */
	public static class SUUser extends AbstractProcessBuilderEffectiveUser implements EffectiveUser {
		private List<String> original;
		private String username;

		/**
		 * Constructor for specific user
		 * 
		 * @param username username
		 */
		public SUUser(String username) {
			this.username = username;
		}

		@Override
		public void descend(ForkerBuilder builder, Process process, Command command) {
			builder.command().clear();
			builder.command().addAll(original);
		}

		@Override
		public void elevate(ForkerBuilder builder, Process process, Command command) {
			original = new ArrayList<String>(builder.command());
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
				if (System.console() == null)
					throw new IllegalStateException("This program wants to switch users, "
							+ "but sudo is not available, and the fallback 'su' is not capable of "
							+ "running without a controlling terminal.");
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

	/**
	 * Abstract {@link EffectiveUser} implementation to be used on POSIX systems
	 * that have 'seteuid'.
	 */
	static abstract class AbstractPOSIXEffectiveUser<T> extends AbstractProcessBuilderEffectiveUser {
		private boolean setRemote;
		private T value;
		private int was = Integer.MIN_VALUE;

		AbstractPOSIXEffectiveUser(T value) {
			this.value = value;
		}

		@Override
		public synchronized void descend(ForkerBuilder builder, Process process, Command command) {
			if (setRemote) {
				setRemote = false;
			} else {
				if (was == Integer.MIN_VALUE)
					throw new IllegalStateException();
				seteuid(was);
				was = Integer.MIN_VALUE;
			}
		}

		@Override
		public void elevate(ForkerBuilder builder, Process process, Command command) {
			if (process instanceof ForkerDaemonProcess) {
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

		protected void seteuid(int euid) {
			if (CSystem.INSTANCE.seteuid(euid) == -1) {
				// TODO get errono
				throw new RuntimeException("Failed to set EUID.");
			}
			;
		}

		abstract void doSet(T value);
	}
}
