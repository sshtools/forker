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

import java.nio.ByteBuffer;

import org.apache.commons.lang3.SystemUtils;

import com.sshtools.forker.client.DefaultNonBlockingProcessListener;
import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.client.impl.nonblocking.NonBlockingProcess;
import com.sshtools.forker.common.IO;

/**
 * Simple non-blocking I/O example that reads the output of a command.
 */
public class NonBlocking {
	public static void main(String[] args) throws Exception {
		ForkerBuilder builder = new ForkerBuilder().io(IO.NON_BLOCKING).redirectErrorStream(true);
		if (SystemUtils.IS_OS_UNIX) {
			// The unix example tries to list the root directory
			builder.command("ls", "-al", "/");
		} else {
			builder.command("DIR", "C:\\");
		}
		NonBlockingProcess process = new ForkerBuilder("ls","-al", "/").io(IO.NON_BLOCKING).redirectErrorStream(true).start(new DefaultNonBlockingProcessListener() {
			@Override
			public void onStdout(NonBlockingProcess process, ByteBuffer buffer, boolean closed) {
				if (!closed) {
					byte[] bytes = new byte[buffer.remaining()];
					/* Consume bytes from buffer (so position is updated) */
					buffer.get(bytes);
					System.out.println(new String(bytes));
				}
			}
		});
		/*
		 * Not strictly required, this is just to hold up the example thread
		 * until the command is finished, your use case may or may not need to
		 * wait for the command to finish.
		 */
		System.out.println("Done: " + process.waitFor());
	}
}
