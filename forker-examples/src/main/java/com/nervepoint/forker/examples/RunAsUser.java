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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;

import com.sshtools.forker.client.EffectiveUser;
import com.sshtools.forker.client.EffectiveUserFactory;
import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.common.IO;

/**
 * Demonstrates running a command as another user.
 */
public class RunAsUser {

	public static void main(String[] args) throws Exception {

		/* Either supply a username on the command line when you run this class or 
		 * change this username 'testuser2' to one that exists on your system.
		 */
		String username = args.length == 0 ? "testuser2" : args[0];
		
		/* Get the user object for this user */
		EffectiveUser user = EffectiveUserFactory.getDefault().getUserForUsername(username);
		
		/* Create the builder */
		ForkerBuilder builder = new ForkerBuilder().effectiveUser(user).
				io(IO.IO).redirectErrorStream(true);
		
		if(SystemUtils.IS_OS_LINUX) {
			// The linux example tries to list the users home directory
			builder.command("id");
		}
		else {
			throw new UnsupportedOperationException();
		}
		
		Process p = builder.start();
		IOUtils.copy(p.getInputStream(), System.out);
		System.out.println(" (" + p.waitFor() + ")");
	}
}
