package com.sshtools.forker.daemon;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.sshtools.forker.common.States;

public class ControlHandler implements Handler {

	public ControlHandler() {
	}

	@Override
	public int getType() {
		return 1;
	}

	@Override
	public void handle(Forker forker, DataInputStream din, DataOutputStream dos) throws IOException {
		dos.writeInt(States.OK);
		dos.flush();

		/*
		 * Connection Type 1 is a control connection from the client and is used
		 * when this is an 'isolated' forker for a single JVM only. The client
		 * keeps this connection open until it dies, so a soon as it does, we
		 * shutdown this JVM too
		 */

		// Wait forever
		try {
			while (true)
				din.readByte();
		} finally {
			forker.exit();
		}
	}

	@Override
	public void stop() {
	}

}
