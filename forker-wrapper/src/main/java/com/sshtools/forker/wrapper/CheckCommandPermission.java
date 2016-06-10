package com.sshtools.forker.wrapper;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.sshtools.forker.common.Command;
import com.sshtools.forker.daemon.CommandExecutor;
import com.sshtools.forker.daemon.ExecuteCheckResult;
import com.sshtools.forker.daemon.Forker;

/**
 * {@link CommandExecutor} that checks whether the command that Forker Daemon is
 * being requested to executor is allowed or not, based on a list of allowed
 * patterns and a list of rejected patterns. When both lists are empty, all
 * commands are allowed.
 */
public class CheckCommandPermission implements CommandExecutor {

	private List<String> allow = Collections.emptyList();
	private List<String> reject = Collections.emptyList();

	@Override
	public ExecuteCheckResult willHandle(Forker forker, Command command) {
		if (((allow.isEmpty() || (!allow.isEmpty()) && matches(allow, command)) && !matches(reject, command)))
			return ExecuteCheckResult.DONT_CARE;

		return ExecuteCheckResult.NO;
	}

	@Override
	public void handle(Forker forker, DataInputStream din, DataOutputStream dos, Command command) throws IOException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Get the list of allowed command patterns.
	 * 
	 * @return allowed command patterns
	 */
	public List<String> getAllow() {
		return allow;
	}

	/**
	 * Set the list of allowed command patterns.
	 * 
	 * @param allow
	 *            allowed command patterns
	 */
	public void setAllow(List<String> allow) {
		this.allow = allow;
	}

	/**
	 * Get the list of rejected command patterns.
	 * 
	 * @return rejected command patterns
	 */
	public List<String> getReject() {
		return reject;
	}

	/**
	 * Set the list of rejected command patterns.
	 * 
	 * @param reject
	 *            rejected command patterns
	 */
	public void setReject(List<String> reject) {
		this.reject = reject;
	}

	boolean matches(List<String> list, Command command) {
		String arg = command.getArguments().get(0);
		for (String l : list) {
			if (arg.matches(l))
				return true;
		}
		return false;
	}
}
