/**
 * Copyright © 2015 - 2021 SSHTOOLS Limited (support@sshtools.com)
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

import static com.sshtools.forker.client.OSCommand.elevate;
import static com.sshtools.forker.client.OSCommand.restrict;
import static com.sshtools.forker.client.OSCommand.run;

import org.apache.commons.lang3.SystemUtils;

import com.sshtools.forker.client.OSCommand;

/**
 * Show uses {@link OSCommand}, that uses {@link ThreadLocal} state to configure
 * and run simple commands.
 */
public class SimpleElevate {
	public static void main(String[] args) throws Exception {
		elevate();
		try {
			if (SystemUtils.IS_OS_UNIX)
				run("cat", "/etc/passwd");
			else {
				String pf = System.getenv("PROGRAMFILES");
				if (pf == null)
					pf = "C:\\Program Files";
				run("MKDIR", pf + "\\SimpleElevate.TMP");
				run("RD", pf + "\\SimpleElevate.TMP");
			}
		} finally {
			restrict();
		}
	}
}
