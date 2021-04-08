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
package com.sshtools.forker.wrapper;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;

import com.sshtools.forker.common.States;
import com.sshtools.forker.daemon.Forker;
import com.sshtools.forker.daemon.Handler;

/**
 * A {@link Handler} implementation that is installed in the <i>Forker Daemon</i>
 * to process ping (and in the future, other) requests from the wrapped
 * application.
 */
public class WrapperHandler implements Handler {

	private long lastPing;

	public WrapperHandler() {
	}

	@Override
	public int getType() {
		return 2;
	}

	/**
	 * Get when the wrapped application last pinged the wrapper.
	 * 
	 * @return last ping
	 */
	public long getLastPing() {
		return lastPing;
	}

	@Override
	public void handle(Forker forker, DataInputStream din, DataOutputStream dos) throws IOException {

		/**
		 * Connection type 2 is held open by the client waiting for reply (that
		 * we never send). So when this runtime dies, the client can detect this
		 * (and also close itself down). This is also used for JVM timeout
		 * detection
		 */

		lastPing = System.currentTimeMillis();
		try {
			while (true) {
				dos.writeInt(States.OK);
				dos.flush();
				din.readByte();
				lastPing = System.currentTimeMillis();
				Thread.sleep(1);
			}
		} catch (InterruptedException ie) {
			throw new EOFException();
		} catch (SocketException se) {
			throw new EOFException();
		}
	}

	@Override
	public void stop() {
		lastPing = 0;
	}

}
