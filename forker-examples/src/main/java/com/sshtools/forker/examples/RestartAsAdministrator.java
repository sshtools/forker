package com.sshtools.forker.examples;

import static com.sshtools.forker.client.OSCommand.run;

import com.sshtools.forker.client.OSCommand;
import com.sshtools.forker.common.OS;

/**
 * Restart this application as an administrator.
 */
public class RestartAsAdministrator {
	public static void main(String[] args) throws Exception {
		OSCommand.restartAsAdministrator(RestartAsAdministrator.class, args);
		if (OS.isUnix()) {
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
