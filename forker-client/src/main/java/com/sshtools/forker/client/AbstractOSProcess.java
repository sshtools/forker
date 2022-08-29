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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import com.sshtools.forker.common.OS;
import com.sshtools.forker.common.Util;
import com.sun.jna.Platform;

/**
 * Abstract implementation for processes that use OS calls.
 */
public abstract class AbstractOSProcess extends ForkerProcess {

	@Override
	public void destroy() {
		throw new UnsupportedOperationException();
	};

	@Override
	public InputStream getErrorStream() {
		throw new UnsupportedOperationException();
	}

	@Override
	public InputStream getInputStream() {
		throw new UnsupportedOperationException();
	}

	@Override
	public OutputStream getOutputStream() {
		throw new UnsupportedOperationException();
	}

	protected String buildCommand(final ForkerBuilder builder) {
		// This will run in a shell, so we need to escape the
		// command line arguments
		StringBuilder bui = new StringBuilder();

		if (Platform.isWindows()) {
			if (builder.background()) {
				bui.append("start /b");

				if (builder.directory() != null) {
					bui.append(" /d\"");
					bui.append(Util.escapeDoubleQuotes(builder.directory().getAbsolutePath()));
					bui.append("\"");
				}

			} else {
				bui.append("cmd /c");
				if (builder.directory() != null) {
					bui.append(" \"cd \"");
					bui.append(Util.escapeDoubleQuotes(builder.directory().getAbsolutePath()));
					bui.append("\" && ");
				}

			}
			for (int i = 0; i < builder.command().size(); i++) {
				if (i == 0 && !builder.background()) {
					// cmd doesnt like first argument at all :(
					bui.append(builder.command().get(i));
				} else {
					bui.append("\"");
					bui.append(Util.escapeDoubleQuotes(builder.command().get(i)));
					bui.append("\"");
				}
			}

			if (!builder.background() && builder.directory() != null) {
				bui.append("\"");
			}
		} else if (OS.isUnix()) {
			for (Map.Entry<String, String> en : builder.environment().entrySet()) {
				if (bui.length() > 0) {
					bui.append(";");
				}
				bui.append("export ");
				bui.append(en.getKey());
				bui.append("='");
				bui.append(Util.escapeSingleQuotes(en.getValue()));
				bui.append("'");
			}
			if (builder.directory() != null) {
				if (bui.length() > 0) {
					bui.append(";");
				}
				bui.append("cd '");
				bui.append(Util.escapeSingleQuotes(builder.directory().getAbsolutePath()));
				bui.append("'");
			}
			if (bui.length() > 0) {
				bui.append(";");
			}
			if (builder.background()) {
				bui.append("nohup ");
			}
			for (String a : builder.getAllArguments()) {
				bui.append(" '");
				bui.append(Util.escapeSingleQuotes(a));
				bui.append("'");
			}
			if (builder.background()) {
				bui.insert(0, "(");
			}
			if (builder.redirectErrorStream()) {
				bui.insert(0, "(");
				bui.append(") 2>&1");
			}
			if (builder.background()) {
				bui.append(")&");
			}
		} else {
			throw new UnsupportedOperationException("OS not supported.");
		}

		final String string = bui.toString();
		return string;
	}


}
