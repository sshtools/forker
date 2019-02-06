package com.nervepoint.forker.examples;

import org.apache.commons.io.IOUtils;

import com.sshtools.forker.client.PowerShellBuilder;

/**
 * Demonstrates running a PowerShell command using {@link PowerShellBuilder}.
 */
public class PowerShellCommand {
	public static void main(String[] args) throws Exception {
		/* Create the builder */
		Process p = new PowerShellBuilder("$PSVersionTable").start();
		IOUtils.copy(p.getInputStream(), System.out);
		System.out.println(" (" + p.waitFor() + ")");
	}
}
