package com.nervepoint.forker.examples;

import org.apache.commons.lang.SystemUtils;

import com.sshtools.forker.client.OSCommand;
import static com.sshtools.forker.client.OSCommand.*;

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
