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
package com.sshtools.forker.client.impl;

import java.io.IOException;
import java.net.ConnectException;

import com.sshtools.forker.client.EffectiveUser;
import com.sshtools.forker.client.Forker;
import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.client.ForkerProcess;
import com.sshtools.forker.client.ForkerProcessFactory;
import com.sshtools.forker.client.ForkerProcessListener;
import com.sshtools.forker.client.impl.ForkerDaemonProcess.Listener;
import com.sshtools.forker.common.IO;
import com.sshtools.forker.common.OS;

/**
 * Creates {@link ForkerDaemonProcess}.
 */
public class ForkerDaemonProcessFactory implements ForkerProcessFactory {

	@Override
	public ForkerProcess createProcess(ForkerBuilder builder, ForkerProcessListener listener) throws IOException {
		if (builder.io() != IO.DEFAULT && !Boolean.getBoolean("forker.disableDaemon")) {
			

			
			
			try {
				EffectiveUser effectiveUser = builder.effectiveUser();
				
				ForkerDaemonProcess forkerProcess = new ForkerDaemonProcess(builder.getCommand());
				if(listener instanceof Listener) {
					forkerProcess.addListener((Listener)listener);
				}
				if (effectiveUser != null) {
					if (!Forker.isDaemonRunning() && !OS.isAdministrator())
						return null;
					effectiveUser.elevate(builder, forkerProcess, builder.getCommand());
				}
				try {
					forkerProcess.start();
					return forkerProcess;
				} finally {
					if (effectiveUser != null)
						effectiveUser.descend(builder, forkerProcess, builder.getCommand());
				}
			} catch (ConnectException ce) {
				// No forker, we will have to resort to using standard
				// ProcessBuilder
			}

			if (builder.io() == IO.DAEMON) {
				throw new IOException("Mode explicitly request daemon, but daemon process failed.");
			}
		}
		return null;
	}

}
