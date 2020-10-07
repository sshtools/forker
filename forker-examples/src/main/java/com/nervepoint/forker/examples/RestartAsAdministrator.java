package com.nervepoint.forker.examples;

import static com.sshtools.forker.client.OSCommand.run;

import org.apache.commons.lang3.SystemUtils;

import com.sshtools.forker.client.OSCommand;

/**
 * Restart this application as an administrator.
 */
public class RestartAsAdministrator {
	public static void main(String[] args) throws Exception {
		OSCommand.restartAsAdministrator(RestartAsAdministrator.class, args);
		if (SystemUtils.IS_OS_UNIX) {
			run("cat", "/etc/passwd");
			run("id");
		}
		else {
			String pf = System.getenv("PROGRAMFILES");
			if (pf == null)
				pf = "C:\\Program Files";
			run("MKDIR", pf + "\\SimpleElevate.TMP");
			run("RD", pf + "\\SimpleElevate.TMP");
		}
	}
}
