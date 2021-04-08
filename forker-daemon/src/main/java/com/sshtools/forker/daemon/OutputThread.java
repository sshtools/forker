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
package com.sshtools.forker.daemon;

import java.io.DataOutputStream;
import java.io.InputStream;

import com.sshtools.forker.common.Command;
import com.sshtools.forker.common.States;

/**
 * Thread that copies stderr data from processes to forker client streams.
 */
public class OutputThread extends Thread {
	private final DataOutputStream dout;
	private final Command cmd;
	private final InputStream err;

	/**
	 * Constructor
	 * 
	 * @param dout
	 *            forker client data output stream
	 * @param cmd
	 *            command
	 * @param err
	 *            stream providing error output
	 */
	public OutputThread(DataOutputStream dout, Command cmd, InputStream err) {
		this.dout = dout;
		this.cmd = cmd;
		this.err = err;
	}

	public void run() {
		Forker.readStreamToOutput(dout, err, cmd.isRedirectError() ? States.OUT : States.ERR);
	}
}