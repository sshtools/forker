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
package com.sshtools.forker.daemon;

import java.io.DataInputStream;
import java.io.OutputStream;

import com.sshtools.forker.common.States;

/**
 * Read commands from a forker client and act on them, such as sending stdin to
 * the process, altering wiindow size and more.
 *
 */
public abstract class InputThread extends Thread {
	private final DataInputStream din;
	private final OutputStream out;

	/**
	 * Constructor.
	 * 
	 * @param out
	 *            process output stream
	 * @param din
	 *            client data input stream
	 */
	public InputThread(OutputStream out, DataInputStream din) {
		this.out = out;
		this.din = din;
	}

	/**
	 * Kill the process
	 */
	public abstract void kill();

	/**
	 * Set the window size
	 * 
	 * @param width
	 *            width
	 * @param height
	 *            height
	 */
	public abstract void setWindowSize(int width, int height);

	public void run() {
		try {
			boolean run = true;
			while (run) {
				int cmd = din.readInt();
				if (cmd == States.OUT) {
					int len = din.readInt();
					byte[] buf = new byte[len];
					din.readFully(buf);
					out.write(buf);
				} else if (cmd == States.KILL) {
					kill();
				} else if (cmd == States.CLOSE_OUT) {
					out.close();
				} else if (cmd == States.FLUSH_OUT) {
					out.flush();
				} else if (cmd == States.WINDOW_SIZE) {
					setWindowSize(din.readInt(), din.readInt());
				} else if (cmd == States.END) {
					run = false;
				} else {
					throw new IllegalStateException("Unknown state code from client '" + cmd + "'");
				}
			}
		} catch (Exception ioe) {
		}
	}
}