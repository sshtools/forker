package com.nervepoint.forker.examples;

import com.sshtools.forker.client.Forker;
import com.sshtools.forker.client.OSCommand;

public class OSCommandElevate {

	public static void main(String[] args) throws Exception {
		Forker.loadDaemon(true);
		OSCommand.admin("ifconfig");

		// // 2nd time should not ask for password
		OSCommand.admin("id");
		//
		// // 3rd should run as current user (via root)
		OSCommand.run("id");

	}

}
