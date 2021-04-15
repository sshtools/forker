package com.sshtools.forker.common;

import java.io.File;
import java.io.IOException;

import com.sshtools.forker.common.CSystem.Termios;
import com.sun.jna.Memory;
import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;

/**
 * Utilities for accessing some OS specific features.
 *
 */
public class OS {

	private static Termios oldTermios = null;
	private static Thread unbufferedResetHook;
	private static Integer oldMode = null;

	/**
	 * The type of desktop in use
	 *
	 */
	public enum Desktop {
		/**
		 * Gnome
		 */
		GNOME,
		/**
		 * KDE
		 */
		KDE,
		/**
		 * Cinnamon
		 */
		CINNAMON,
		/**
		 * XFCE
		 */
		XFCE,
		/**
		 * GNOME3
		 */
		GNOME3,
		/**
		 * Windows
		 */
		WINDOWS,
		/**
		 * Mac OS X
		 */
		MAC_OSX,
		/**
		 * Mac
		 */
		MAC,
		/**
		 * Other undetected
		 */
		OTHER,
		/**
		 * Unity
		 */
		UNITY,
		/**
		 * LXDE
		 */
		LXDE,
		/**
		 * Console
		 */
		CONSOLE,
		/**
		 * None
		 */
		NONE
	}

	/**
	 * Get if this environment is running on a desktop (and has access to the
	 * display)
	 * 
	 * @return running on desktop
	 */
	public static boolean isRunningOnDesktop() {
		return !getDesktopEnvironment().equals(Desktop.CONSOLE);
	}

	/**
	 * If this is a console application, you can unbuffer stdin to get immediate
	 * input from the user. This may be reset using {@link #resetStdin()}.
	 * 
	 * @see #resetStdin()
	 */
	public static void resetStdin() {
		if (isUnix()) {
			if (unbufferedResetHook != null) {
				try {
					Runtime.getRuntime().removeShutdownHook(unbufferedResetHook);
				} catch (Exception e) {
				}
				CSystem.INSTANCE.tcsetattr(CSystem.STDIN_FILENO, CSystem.TCSANOW, oldTermios);
				oldTermios = null;
				unbufferedResetHook = null;
			}
		} else if (Platform.isWindows()) {
			if (oldMode != null) {
				HANDLE hStdin = Kernel32.INSTANCE.GetStdHandle(Kernel32.STD_INPUT_HANDLE);
				Kernel32.INSTANCE.SetConsoleMode(hStdin, oldMode);
			}
		} else {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Reset stdin back to a buffered state.
	 * 
	 * @see #unbufferedStdin()
	 */
	public static void unbufferedStdin() {
		// http://forums.codeguru.com/showthread.php?466009-Reading-from-stdin-%28without-echo%29
		if (isUnix()) {
			if (oldTermios != null)
				// Already configuration
				return;
			oldTermios = new Termios();
			CSystem.INSTANCE.tcgetattr(CSystem.STDIN_FILENO, oldTermios);
			Termios termios = new Termios(oldTermios);
			termios.c_iflag = 0;
			termios.c_lflag &= ~(CSystem.ECHO);
			termios.c_lflag &= ~(CSystem.ECHOE);
			termios.c_lflag &= ~(CSystem.ISIG);
			termios.c_oflag &= ~(CSystem.OPOST);
			CSystem.INSTANCE.tcsetattr(CSystem.STDIN_FILENO, CSystem.TCSANOW, termios);
			Runtime.getRuntime().addShutdownHook(unbufferedResetHook = new Thread() {
				@Override
				public void run() {
					resetStdin();
				}
			});
		} else if (Platform.isWindows()) {
			HANDLE hStdin = Kernel32.INSTANCE.GetStdHandle(Kernel32.STD_INPUT_HANDLE);
			IntByReference mode = new IntByReference();
			Kernel32.INSTANCE.GetConsoleMode(hStdin, mode);
			oldMode = mode.getValue();
			Kernel32.INSTANCE.SetConsoleMode(hStdin, mode.getValue() & (~Kernel32.ENABLE_ECHO_INPUT));
		} else {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Get the current process ID.
	 * 
	 * @return process ID
	 */
	public static int getPID() {
		if (Platform.isWindows()) {
			return Kernel32.INSTANCE.GetCurrentProcessId();
		}
		if (isUnix()) {
			return CSystem.INSTANCE.getpid();
		}
		throw new UnsupportedOperationException();
	}

	/**
	 * Get the username that would normally be used for administrator.
	 * 
	 * @return administrator username
	 */
	public static String getAdministratorUsername() {
		if (Platform.isWindows()) {
			return System.getProperty("forker.administratorUsername",
					System.getProperty("vm.rootUser", "Administrator"));
		}
		if (isUnix()) {
			return System.getProperty("forker.administratorUsername", System.getProperty("vm.rootUser", "root"));
		}
		throw new UnsupportedOperationException();
	}

	/**
	 * Get if currently running as an administrator.
	 * 
	 * @return administrator
	 */
	public static boolean isAdministrator() {
		if (Platform.isWindows()) {
			try {
				String programFiles = System.getenv("ProgramFiles");
				if (programFiles == null) {
					programFiles = "C:\\Program Files";
				}
				File temp = new File(programFiles, "foo.txt");
				temp.deleteOnExit();
				if (temp.createNewFile()) {
					temp.delete();
					return true;
				} else {
					return false;
				}
			} catch (IOException e) {
				return false;
			}
		}
		if (isUnix()) {
			return System.getProperty("forker.administratorUsername", System.getProperty("vm.rootUser", "root"))
					.equals(System.getProperty("user.name"));
		}
		throw new UnsupportedOperationException();
	}

	/**
	 * Get the current desktop environment in use.
	 * 
	 * @return desktop
	 */
	public static Desktop getDesktopEnvironment() {
		// TODO more to do - see the following links for lots of info

		// http://unix.stackexchange.com/questions/116539/how-to-detect-the-desktop-environment-in-a-bash-script
		// http://askubuntu.com/questions/72549/how-to-determine-which-window-manager-is-running/227669#227669

		String desktopSession = System.getenv("XDG_CURRENT_DESKTOP");
		String gdmSession = System.getenv("GDMSESSION");
		if (Platform.isWindows()) {
			return Desktop.WINDOWS;
		}
		if (Platform.isMac()) {
			return Desktop.MAC_OSX;
		}
		if (Platform.isLinux() && Util.isBlank(System.getenv("DISPLAY"))) {
			return Desktop.CONSOLE;
		}

		if ("X-Cinnamon".equalsIgnoreCase(desktopSession)) {
			return Desktop.CINNAMON;
		}
		if ("LXDE".equalsIgnoreCase(desktopSession)) {
			return Desktop.LXDE;
		}
		if ("XFCE".equalsIgnoreCase(desktopSession)) {
			return Desktop.XFCE;
		}
		if ("KDE".equalsIgnoreCase(desktopSession)
				|| (Util.isBlank(desktopSession) && "kde-plasma".equals(gdmSession))) {
			return Desktop.KDE;
		}
		if ("UNITY".equalsIgnoreCase(desktopSession)) {
			return Desktop.UNITY;
		}
		if ("GNOME".equalsIgnoreCase(desktopSession)) {
			if ("gnome-shell".equals(gdmSession)) {
				return Desktop./**
								 * GNOME3
								 */
						GNOME3;
			}
			return Desktop.GNOME;
		}
		return Desktop.OTHER;
	}

	/**
	 * Get the path of the command that can be used to launch a new Java
	 * runtime.
	 * 
	 * @return Java path
	 */
	public static String getJavaPath() {
		String javaExe = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
		if (Platform.isWindows()) {
			if (!javaExe.toLowerCase().endsWith("w")) {
				javaExe += "w";
			}
			javaExe += ".exe";
		}
		return javaExe;
	}

	/**
	 * Set the process name. DOES NOT WORK.
	 * 
	 * @param procname
	 *            process name
	 * @return success
	 */
	public static boolean setProcname(String procname) {
		if (isUnix() && !Platform.isMac()) {
			if(procname.length() > 15)
				procname = procname.substring(0, 15);
			Memory name = new Memory(procname.length() + 1);
			name.setString(0, procname);
			return CSystem.INSTANCE.prctl(CSystem.PR_SET_NAME, name, new IntByReference(0).getPointer(),
					new IntByReference(0).getPointer(), new IntByReference(0).getPointer()) == 0;
		}
		return false;

	}

	/**
	 * Gef if using a Unix like OS.
	 * 
	 * @return unix
	 */
	public static boolean isUnix() {
		return Platform.isMac() || Platform.isAIX() || Platform.isLinux() || Platform.isFreeBSD() || Platform.isNetBSD() || Platform.isOpenBSD();
	}

	public static boolean isJava8() {
		return System.getProperty("java.specification.version", "").startsWith("1.8");
	}
}
