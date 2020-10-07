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
import org.apache.commons.lang3.SystemUtils;

import com.sshtools.forker.client.EffectiveUser;
import com.sshtools.forker.client.EffectiveUserFactory;
import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.common.IO;

/**
 * Demonstrates elevating to administrator using {@link ForkerBuilder}.
 */
public class AdministratorElevate {
	public static void main(String[] args) throws Exception {
		
		/* Get the user object for admnistrator */
		EffectiveUser administrator = EffectiveUserFactory.getDefault().administrator();
		
		/* Create the builder */
		ForkerBuilder builder = new ForkerBuilder().effectiveUser(administrator).io(IO.IO);
		if (SystemUtils.IS_OS_LINUX) {
			/* The linux example tries to list the shadow password file */
			builder.redirectErrorStream(true).io(IO.IO).command("cat", "/etc/shadow");
		} else if (SystemUtils.IS_OS_WINDOWS) {
			/*
			 * Windows try to create a filename protected by WRP
			 * builder.command("dir", ">", "c:\\forker-test.exe");
			 * builder.command("C:\\windows\\system32\\cmd.exe", "/c", "dir
			 * \\");
			 */
			builder.command("C:\\windows\\system32\\xcopy.exe");
			builder.io(IO.SINK);
		} else {
			throw new UnsupportedOperationException();
		}
		
		Process p = builder.start();
		IOUtils.copy(p.getInputStream(), System.out);
		System.out.println(" (" + p.waitFor() + ")");
	}
}
