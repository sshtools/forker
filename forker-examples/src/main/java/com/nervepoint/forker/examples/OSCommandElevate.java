package com.nervepoint.forker.examples;

import com.sshtools.forker.client.Forker;
import com.sshtools.forker.client.OSCommand;

public class OSCommandElevate {

	public static void main(String[] args) throws Exception {
		Forker.loadDaemon(true);
		OSCommand.elevate();
		try {
			OSCommand.run("ls");
		} finally {
			OSCommand.restrict();
		}

		// 2nd time should not ask for password
		OSCommand.elevate();
		try {
			OSCommand.run("ls");
		} finally {
			OSCommand.restrict();
		}

	}

}
