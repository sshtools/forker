package com.sshtools.forker.daemon;

/**
 * Used as the result of
 * {@link CommandExecutor#willHandle(Forker, com.sshtools.forker.common.Command)}
 *
 */
public enum ExecuteCheckResult {
	/**
	 * Executor indicates that is WILL execute the request
	 */
	YES,
	/**
	 * Executor indicates that neither this nor any other executor should handle
	 * the request
	 */
	NO,
	/**
	 * Executor indicates that other executors are free to handle the request
	 */
	DONT_CARE
}
