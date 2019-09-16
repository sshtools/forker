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

import org.apache.commons.io.IOUtils;

import com.sshtools.forker.client.DefaultNonBlockingProcessListener;
import com.sshtools.forker.client.ShellBuilder;
import com.sshtools.forker.client.impl.nonblocking.NonBlockingProcess;
import com.sshtools.forker.common.IO;
import com.sshtools.forker.common.OS;

/**
 * This example shows how to create an interactive shell. 
 *
 */
public class NonBlockingShell {

	public static void main(String[] args) throws Exception {
		/*
		 * This example reads from stdin (i.e. the console), so stdin needs to be unbuffered with
		 * no local echoing at this end of the pipe, the following function
		 * attempts to do this. 
		 */
		OS.unbufferedStdin();
		
		/* ShellBuilder is a specialisation of ForkerBuilder */
		ShellBuilder shell = new ShellBuilder();
		shell.io(IO.NON_BLOCKING);
		shell.redirectErrorStream(true);
		
		/* Demonstrate we are actually in a different shell by setting PS1 */
		shell.environment().put("MYENV", "An environment variable");
		
		final NonBlockingProcess p = shell.start(new DefaultNonBlockingProcessListener() {
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

		/* While using non-blocking, it is still convenient in this case to use
		 * the OutputStream provided by the process as we are just joining
		 * it to stdin of this process
		 */
		IOUtils.copy(System.in, p.getOutputStream());
		
		/* When this processes stdin closes, close the shells stdin too and 
		 * wait for it to finish
		 */
		p.getOutputStream().close();
		int ret = p.waitFor();
		System.err.println("Exited with code: " + ret);
		System.exit(ret);
	}
}
