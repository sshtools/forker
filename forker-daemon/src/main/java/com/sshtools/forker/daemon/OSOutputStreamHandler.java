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
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.sshtools.forker.common.States;

/**
 * Handler that deals with requests for opening files for writing as the user
 * the daemon is running as.
 */
public class OSOutputStreamHandler implements Handler {

	@Override
	public int getType() {
		return 4;
	}

	@Override
	public void handle(Forker forker, DataInputStream din, DataOutputStream dos) throws IOException {
		String filename = din.readUTF();
		boolean append = din.readBoolean();
		FileOutputStream fout = new FileOutputStream(new File(filename), append);
		try {
			dos.writeInt(States.OK);
			dos.flush();
			while (true) {
				int bs = din.readInt();
				if (bs == 0)
					break;
				byte[] buf = new byte[bs];
				din.readFully(buf);
				fout.write(buf);
			}
			fout.flush();
			dos.writeInt(States.OK);
			dos.flush();
		} finally {
			fout.close();
		}
	}

	@Override
	public void stop() {
	}

}
