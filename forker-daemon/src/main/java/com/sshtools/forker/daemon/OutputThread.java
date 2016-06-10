package com.sshtools.forker.daemon;

import java.io.DataOutputStream;
import java.io.InputStream;

import com.sshtools.forker.common.Command;
import com.sshtools.forker.common.States;

/**
 * Thread that copies stderr data from processes to forker client streams.
 */
public class OutputThread extends Thread {
	private final DataOutputStream dout;
	private final Command cmd;
	private final InputStream err;

	/**
	 * Constructor
	 * 
	 * @param dout
	 *            forker client data output stream
	 * @param cmd
	 *            command
	 * @param err
	 *            stream providing error output
	 */
	public OutputThread(DataOutputStream dout, Command cmd, InputStream err) {
		this.dout = dout;
		this.cmd = cmd;
		this.err = err;
	}

	public void run() {
		Forker.readStreamToOutput(dout, err, cmd.isRedirectError() ? States.OUT : States.ERR);
	}
}