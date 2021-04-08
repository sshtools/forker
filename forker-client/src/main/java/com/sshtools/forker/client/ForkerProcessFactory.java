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

import java.io.IOException;

/**
 * Interface to be implemented by pluggable elements that handle actual process
 * creation. Each factory will be called in turn until one creates a process.
 * New factories are registered as a standard Java server (META-INF/services). *
 */
public interface ForkerProcessFactory {

	/**
	 * Create a new process. If this factory is not appropriate for the builder
	 * configuration it should return <code>null</code>. If an error occurs it
	 * should throw an exception.
	 * 
	 * @param builder builder
	 * @param listener listener
	 * @return process
	 * @throws IOException on any error
	 */
	ForkerProcess createProcess(ForkerBuilder builder, ForkerProcessListener listener) throws IOException;
}
