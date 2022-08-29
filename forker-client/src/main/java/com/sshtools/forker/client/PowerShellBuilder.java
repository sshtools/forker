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
package com.sshtools.forker.client;

import java.io.IOException;
import java.util.List;

import com.sshtools.forker.common.IO;
import com.sun.jna.Platform;

/**
 * Specialised version of {@link ShellBuilder} that builds a process appropriate
 * for running PowerShell commands.
 */
public class PowerShellBuilder extends ShellBuilder {
	{
		if (Platform.isWindows())
			shell("powershell.exe");
		else
			shell("pwsh");

		io(IO.IO);
		redirectErrorStream(true);
	}

	/**
	 * Constructor.
	 * 
	 * @param configuration configuration
	 * @param command command
	 */
	public PowerShellBuilder(ForkerConfiguration configuration, List<String> command) {
		super(configuration, command);
	}

	/**
	 * Constructor.
	 * 
	 * @param configuration configuration
	 * @param command command
	 */
	public PowerShellBuilder(ForkerConfiguration configuration, String... command) {
		super(configuration, command);
	}

	/**
	 * Constructor.
	 * 
	 * @param command command
	 */
	public PowerShellBuilder(List<String> command) {
		super(command);
	}

	/**
	 * Constructor.
	 * 
	 * @param command command
	 */
	public PowerShellBuilder(String... command) {
		super(command);
	}

	@Override
	public <P extends ForkerProcess> P start(ForkerProcessListener listener) throws IOException {
		List<String> a = command();
		if(a.size() > 0) {
			a.add(0, "-Command");
		}
		return super.start(listener);
	}
}
