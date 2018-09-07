/**
 * Copyright © 2015 - 2018 SSHTOOLS Limited (support@sshtools.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nervepoint.forker.examples;

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
