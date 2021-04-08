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

import com.sshtools.forker.common.Command;
import com.sshtools.forker.common.IO;

/**
 * {@link CommandHandler} delegates the actual handling of the request to a
 * Command Executor. These usually examine the {@link IO} mode before deciding
 * if to handle the command or not.
 *
 */
public interface CommandExecutor {

	/**
	 * Determine if this handle will handle the command. If the result is
	 * {@link ExecuteCheckResult#YES}, the executor is indicating it WILL handle
	 * the request. A result of {@link ExecuteCheckResult#NO} indicates NOTHING
	 * should handle the request (used by permission checks). A result of
	 * {@link ExecuteCheckResult#DONT_CARE} indicates other executors are free
	 * to handle the request.
	 * 
	 * @param forker
	 *            forker daemon
	 * @param command
	 *            command
	 * @return result
	 */
	ExecuteCheckResult willHandle(Forker forker, Command command);

	/**
	 * Handle the command execution request.
	 * 
	 * @param forker
	 *            forker
	 * @param din
	 *            data input
	 * @param dos
	 *            data output
	 * @param command
	 *            command
	 * @throws IOException
	 *             on any error
	 */
	void handle(Forker forker, DataInputStream din, DataOutputStream dos, Command command) throws IOException;
}
