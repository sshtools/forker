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
