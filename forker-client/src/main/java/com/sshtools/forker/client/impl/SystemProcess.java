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
package com.sshtools.forker.client.impl;

import java.io.InputStream;
import java.io.OutputStream;

import com.sshtools.forker.client.AbstractOSProcess;
import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.common.CSystem;

/**
 * Uses the C call <b>system(command)</b>. This method doesn't support any I/O
 * streams, and internally may still cause a fork.
 */
public class SystemProcess extends AbstractOSProcess {

	private Thread thread;
	private int exitValue;

	/**
	 * Constructor
	 * 
	 * @param builder
	 *            builder
	 */
	public SystemProcess(final ForkerBuilder builder) {
		thread = new Thread("SystemProcess" + builder.command()) {
			public void run() {
				exitValue = CSystem.INSTANCE.system(buildCommand(builder));
			};
		};
		thread.start();
	}

	@Override
	public OutputStream getOutputStream() {
		throw new UnsupportedOperationException();
	}

	@Override
	public InputStream getInputStream() {
		throw new UnsupportedOperationException();
	}

	@Override
	public InputStream getErrorStream() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int waitFor() throws InterruptedException {
		thread.join();
		return exitValue;
	}

	@Override
	public int exitValue() {
		return exitValue;
	}

	@Override
	public void destroy() {
		throw new UnsupportedOperationException();
	}

}