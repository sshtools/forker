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
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

import com.sshtools.forker.common.States;

/**
 * Handler that deals with requests for opening files for reading as the user
 * the daemon is running as.
 */
public class OSInputStreamHandler implements Handler {

	@Override
	public int getType() {
		return 3;
	}

	@Override
	public void handle(Forker forker, DataInputStream din, DataOutputStream dos) throws IOException {

		String filename = din.readUTF();
		File file = new File(filename);
		FileInputStream fin = null;
		try {
			fin = new FileInputStream(file);
		} catch (IOException ioe) {
			dos.writeInt(States.FAILED);
			dos.writeUTF(ioe.getMessage());
			dos.flush();
			return;
		}

		try {
			dos.writeInt(States.OK);
			dos.writeLong(file.length());
			dos.flush();
			IOUtils.copyLarge(fin, dos);
			dos.flush();
		} finally {
			fin.close();
		}
	}

	@Override
	public void stop() {
	}

}
