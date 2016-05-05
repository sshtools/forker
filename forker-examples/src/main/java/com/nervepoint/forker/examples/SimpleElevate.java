package com.nervepoint.forker.examples;

import com.sshtools.forker.client.OSCommand;

public class SimpleElevate {

	public static void main(String[] args) throws Exception {
		// Either use this, set a password using vm.sudo system property, or leave it (in which case the password will be prompted for) 
		// OSCommand.sudo("SOME PASSWORD".toCharArray());
		
		OSCommand.elevate();
		try {
			OSCommand.run("cat", "/etc/passwd");
		}
		finally {
			OSCommand.restrict();
		}
	}
}
