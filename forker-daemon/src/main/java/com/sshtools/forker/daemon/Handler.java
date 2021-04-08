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
package com.sshtools.forker.daemon;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.sshtools.forker.daemon.Forker.Client;

/**
 * Handlers are responsible for handling requests from forker clients. The
 * handler implementation signals the client type that it is for using
 * {@link #getType()}, which is matched against the type code the client
 * supplies ({@link Client#getType()}).
 *
 */
public interface Handler {

	/**
	 * The clien type this handler is for.
	 * 
	 * @return type
	 */
	int getType();

	/**
	 * Handle the request
	 * 
	 * @param forker
	 *            forker
	 * @param din
	 *            data input stream
	 * @param dos
	 *            data output stream
	 * @throws IOException
	 *             on any error
	 */
	void handle(Forker forker, DataInputStream din, DataOutputStream dos) throws IOException;

	/**
	 * Stop the handler. Called when the daemon shuts down.
	 */
	void stop();
}
