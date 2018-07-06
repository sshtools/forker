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