package com.sshtools.forker.daemon;

import java.io.DataOutputStream;
import java.io.InputStream;

import com.sshtools.forker.common.Command;
import com.sshtools.forker.common.States;

public class OutputThread extends Thread {
	private final DataOutputStream dout;
	private final Command cmd;
	private final InputStream err;

	public OutputThread(DataOutputStream dout, Command cmd, InputStream err) {
		this.dout = dout;
		this.cmd = cmd;
		this.err = err;
	}

	public void run() {
		Forker.readStreamToOutput(dout, err,
				cmd.isRedirectError() ? States.OUT : States.ERR);
	}
}