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
package com.sshtools.forker.client;

import com.sshtools.forker.common.Command;

/**
 * In order to be able to raise privileges to administrator, or to run a process
 * as another user, an instance of an "Effective User" must be created.
 * Implementations are responsible for setting up the process to be elevated.
 * Forker provides a set a default implementations that should be suitable for
 * most needs in {@link EffectiveUserFactory.DefaultEffectiveUserFactory}.
 * 
 * @see EffectiveUserFactory
 *
 */
public interface EffectiveUser {
	/**
	 * De-configure the command and/or process such that it will no longer be
	 * run as an administrator or different, but will run as the current user.
	 * 
	 * @param builder
	 *            builder
	 * @param process
	 *            process
	 * @param command
	 *            command
	 */
	void descend(ForkerBuilder builder, Process process, Command command);

	/**
	 * Alter the command and/or process such that it will be launched using the
	 * user this object represents.
	 * 
	 * @param builder
	 *            builder
	 * @param process
	 *            process
	 * @param command
	 *            command
	 */
	void elevate(ForkerBuilder builder, Process process, Command command);
}