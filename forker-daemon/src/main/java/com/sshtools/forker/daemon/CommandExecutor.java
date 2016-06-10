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
