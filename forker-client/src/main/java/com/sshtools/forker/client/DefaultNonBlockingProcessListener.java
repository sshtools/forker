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

import java.nio.ByteBuffer;

import com.sshtools.forker.client.impl.nonblocking.NonBlockingProcess;

/**
 * Default implementation of a {@link NonBlockingProcessListener}.
 */
public class DefaultNonBlockingProcessListener implements NonBlockingProcessListener {
	@Override
	public void onError(Exception exception, NonBlockingProcess process, boolean existing) {
	}

	@Override
	public void onExit(int exitCode, NonBlockingProcess process) {
	}

	@Override
	public void onStdout(NonBlockingProcess process, ByteBuffer buffer, boolean closed) {
	}

	@Override
	public void onStderr(NonBlockingProcess process, ByteBuffer buffer, boolean closed) {
	}

	@Override
	public boolean onStdinReady(NonBlockingProcess process, ByteBuffer buffer) {
		return false;
	}

	@Override
	public void onStart(NonBlockingProcess process) {
	}

	@Override
	public void onStarted(NonBlockingProcess process) {
	}
}
