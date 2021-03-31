package com.sshtools.forker.examples;

import static com.sshtools.forker.client.OSCommand.admin;
import static com.sshtools.forker.client.OSCommand.elevate;
import static com.sshtools.forker.client.OSCommand.restrict;
import static com.sshtools.forker.client.OSCommand.run;

/**
 * Demonstrates using elevated commands
 *
 */
public class OSCommandElevate {

	public static void main(String[] args) throws Exception {
		/*
		 * First run an admin command, each time such a command is run
		 * the password will be required. The 'admin' method is a shortcut for elevating
		 * permissions then using 'run'
		 */

		// The shorthand version
		admin("ifconfig");

		// The longhand version
		elevate();
		try {
			run("ifconfig");
		} finally {
			restrict();
		}

	}

}
