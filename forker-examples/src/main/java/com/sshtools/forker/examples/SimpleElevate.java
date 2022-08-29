package com.sshtools.forker.examples;

import static com.sshtools.forker.client.OSCommand.elevate;
import static com.sshtools.forker.client.OSCommand.restrict;
import static com.sshtools.forker.client.OSCommand.run;

import com.sshtools.forker.client.OSCommand;
import com.sshtools.forker.common.OS;
import com.sun.jna.Platform;

/**
 * Show uses {@link OSCommand}, that uses {@link ThreadLocal} state to configure
 * and run simple commands.
 */
public class SimpleElevate {
	public static void main(String[] args) throws Exception {
		elevate();
		try {
			if (Platform.isMac())
				run("cat", "/etc/master.passwd");
			else if (OS.isUnix())
				run("cat", "/etc/passwd");
			else {
				String pf = System.getenv("PROGRAMFILES");
				if (pf == null)
					pf = "C:\\Program Files";
				run("MKDIR", pf + "\\SimpleElevate.TMP");
				run("RD", pf + "\\SimpleElevate.TMP");
			}
		} finally {
			restrict();
		}
	}
}
