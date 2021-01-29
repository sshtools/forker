package com.sshtools.forker.examples;

import static com.sshtools.forker.client.OSCommand.elevate;
import static com.sshtools.forker.client.OSCommand.restrict;
import static com.sshtools.forker.client.OSCommand.run;

import org.apache.commons.lang3.SystemUtils;

import com.sshtools.forker.client.OSCommand;

/**
 * Show uses {@link OSCommand}, that uses {@link ThreadLocal} state to configure
 * and run simple commands.
 */
public class SimpleElevate {
	public static void main(String[] args) throws Exception {
		elevate();
		try {
			if (SystemUtils.IS_OS_UNIX)
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
