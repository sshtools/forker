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
 * Represents the priority of a process. Note, the OS may support more less
 * priorities, but currently a least common denominator approach has been taken
 * with this feature. This may change in the future.
 *
 */
public enum Priority {
	/**
	 * Low priority
	 */
	LOW,
	/**
	 * Normal priority, i.e. as decided by OS when priority is not explicitly
	 * set
	 */
	NORMAL,
	/**
	 * High priority
	 */
	HIGH,
	/**
	 * Realtime (when supported)
	 */
	REALTIME
}
