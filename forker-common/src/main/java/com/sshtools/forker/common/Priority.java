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
