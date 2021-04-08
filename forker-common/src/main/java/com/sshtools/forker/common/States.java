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
package com.sshtools.forker.common;

/**
 * Various constants used by the Forker Daemon protocol.
 *
 */
public class States {

	/**
	 * Remote command or operation executed OK
	 */
	public final static int OK = 0;
	/**
	 * Remote command or operation failed to execute OK
	 */
	public final static int FAILED = 1;
	/**
	 * Apply operation to input stream
	 */
	public final static int IN = 2;
	/**
	 * Apply operation to output stream
	 */
	public final static int ERR = 3;
	/**
	 * End of stream
	 */
	public final static int END = 4;
	/**
	 * Apply operation to output stream
	 */
	public final static int OUT = 5;
	/**
	 * Kill process
	 */
	public final static int KILL = 6;
	/**
	 * Close output stream
	 */
	public final static int CLOSE_OUT = 7;
	/**
	 * Close error stream
	 */
	public final static int CLOSE_ERR = 8;
	/**
	 * Close input stream
	 */
	public final static int CLOSE_IN = 9;
	/**
	 * Flush output stream
	 */
	public final static int FLUSH_OUT = 10;
	/**
	 * Window size changed (either direction)
	 */
	public final static int WINDOW_SIZE = 11;
}
