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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.sshtools.forker.common.States;

/**
 * Handler implementation that deals with the 'control connection'. This is used
 * to ensure the daemon shuts down when the client application exits.
 *
 */
public class ControlHandler implements Handler {

	/**
	 * Constructor
	 */
	public ControlHandler() {
	}

	@Override
	public int getType() {
		return 1;
	}

	@Override
	public void handle(Forker forker, DataInputStream din, DataOutputStream dos) throws IOException {
		dos.writeInt(States.OK);
		dos.flush();

		/*
		 * Connection Type 1 is a control connection from the client and is used
		 * when this is an 'isolated' forker for a single JVM only. The client
		 * keeps this connection open until it dies, so a soon as it does, we
		 * shutdown this JVM too
		 */

		// Wait forever
		try {
			while (true)
				din.readByte();
		} finally {
			forker.exit();
		}
	}

	@Override
	public void stop() {
	}

}
