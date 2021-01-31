package com.sshtools.forker.examples;

import com.sshtools.forker.client.PowerShellBuilder;
import com.sshtools.forker.common.Util;

/**
 * Demonstrates running a PowerShell command using {@link PowerShellBuilder}.
 */
public class PowerShellCommand {
	public static void main(String[] args) throws Exception {
		/* Create the builder */
		Process p = new PowerShellBuilder("$PSVersionTable").start();
		Util.copy(p.getInputStream(), System.out);
		System.out.println(" (" + p.waitFor() + ")");
	}
}
