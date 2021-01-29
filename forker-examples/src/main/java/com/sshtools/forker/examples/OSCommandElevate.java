package com.sshtools.forker.examples;

import static com.sshtools.forker.client.OSCommand.admin;
import static com.sshtools.forker.client.OSCommand.elevate;
import static com.sshtools.forker.client.OSCommand.restrict;
import static com.sshtools.forker.client.OSCommand.run;

import com.sshtools.forker.client.Forker;

/**
 * Demonstrates using elevated commands
 *
 */
public class OSCommandElevate {

	public static void main(String[] args) throws Exception {
		/*
		 * First run an admin command with no daemon, each time such a command
		 * is run the password will be required. The 'admin' method is a shortcut for elevating permissions then using
		 * 'run'
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

		/**
		 * Now run some admin commands with an elevated daemon running. Once the
		 * daemon is running, further admin commands will not require a password
		 */
		Forker.loadDaemon(true);

		admin("ifconfig");

		// // 2nd time should not ask for password
		admin("id");
		//
		// // 3rd should run as current user (via root)
		run("id");

	}

}
