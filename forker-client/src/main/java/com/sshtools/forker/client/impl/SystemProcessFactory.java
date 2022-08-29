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

import java.io.IOException;

import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.client.ForkerProcess;
import com.sshtools.forker.client.ForkerProcessFactory;
import com.sshtools.forker.client.ForkerProcessListener;
import com.sshtools.forker.client.NonBlockingProcessListener;
import com.sshtools.forker.common.IO;
import com.sshtools.forker.common.OS;

/**
 * Creates a {@link SystemProcess}.
 *
 */
public class SystemProcessFactory implements ForkerProcessFactory {

	@Override
	public ForkerProcess createProcess(ForkerBuilder builder, ForkerProcessListener listener) throws IOException {
		if (OS.isUnix() && builder.io() == IO.SINK) {

			if(listener instanceof NonBlockingProcessListener) {
				throw new IllegalArgumentException(String.format("%s is not supported by %s, is your I/O mode set correctly (see %s.io(%s))", listener.getClass(), getClass(), ForkerBuilder.class, IO.class));
			}
			/*
			 * We don't need any input or output, so can just start using
			 * 'system' call which just blocks
			 */
			return new SystemProcess(builder);
		}
		return null;
	}

}
