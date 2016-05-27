package com.sshtools.forker.client;

import java.io.File;
import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;

public class OS {

	public enum Desktop {
		GNOME, KDE, CINNAMON, XFCE, GNOME3, WINDOWS, MAC_OSX, MAC, OTHER, UNITY, LXDE, CONSOLE, NONE
	}

	public static boolean isRunningOnDesktop() {
		return !getDesktopEnvironment().equals(Desktop.CONSOLE);
	}

	public static Desktop getDesktopEnvironment() {
		// TODO more to do - see the following links for lots of info

		// http://unix.stackexchange.com/questions/116539/how-to-detect-the-desktop-environment-in-a-bash-script
		// http://askubuntu.com/questions/72549/how-to-determine-which-window-manager-is-running/227669#227669

		String desktopSession = System.getenv("XDG_CURRENT_DESKTOP");
		String gdmSession = System.getenv("GDMSESSION");
		if (SystemUtils.IS_OS_WINDOWS) {
			return Desktop.WINDOWS;
		}
		if (SystemUtils.IS_OS_MAC_OSX) {
			return Desktop.MAC_OSX;
		}
		if (SystemUtils.IS_OS_MAC) {
			return Desktop.MAC;
		}
		if (SystemUtils.IS_OS_LINUX && StringUtils.isBlank(System.getenv("DISPLAY"))) {
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
				|| (StringUtils.isBlank(desktopSession) && gdmSession.equals("kde-plasma"))) {
			return Desktop.KDE;
		}
		if ("UNITY".equalsIgnoreCase(desktopSession)) {
			return Desktop.UNITY;
		}
		if ("GNOME".equalsIgnoreCase(desktopSession)) {
			if ("gnome-shell".equals(gdmSession)) {
				return Desktop.GNOME3;
			}
			return Desktop.GNOME;
		}
		return Desktop.OTHER;
	}

	public static boolean hasCommand(String command) {
		if (SystemUtils.IS_OS_LINUX) {
			try {
				Collection<String> out = OSCommand.runCommandAndCaptureOutput("which", command);
				return !out.isEmpty();
			} catch (Exception e) {
				return false;
			}
		} else {
			String path = System.getenv("PATH");
			if (path != "") {
				boolean found = false;
				for (String p : path.split(File.pathSeparator)) {
					File f = new File(p);
					if (f.isDirectory()) {
						String cmd = command;
						if (SystemUtils.IS_OS_WINDOWS) {
							cmd += ".exe";
						}
						File e = new File(f, cmd);
						if (e.exists()) {
							found = true;
							break;
						}
					}
				}
				return found;
			}
			throw new UnsupportedOperationException(System.getProperty("os.name")
					+ " is not supported. Cannot determine if command " + command + " exists");
		}
	}
}
