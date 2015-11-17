package com.sshtools.forker.daemon;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.sshtools.forker.common.States;

public abstract class InputThread extends Thread {
	private final DataInputStream din;
	private final OutputStream out;

	public InputThread(OutputStream out, DataInputStream din) {
		this.out = out;
		this.din = din;
	}

	abstract void kill();

	public void run() {
		try {
			boolean run = true;
			while (run) {
				int cmd = din.readInt();
				if (cmd == States.OUT) {
					int len = din.readInt();
					byte[] buf = new byte[len];
					din.readFully(buf);
					out.write(buf);
				} else if (cmd == States.KILL) {
					kill();
				} else if (cmd == States.CLOSE_OUT) {
					out.close();
					break;
				} else if (cmd == States.FLUSH_OUT) {
					out.flush();
					break;
				} else if (cmd == States.END) {
					run = false;
					break;
				} else {
					throw new IllegalStateException(
							"Unknown state code from client '" + cmd + "'");
				}
			}
		} catch (IOException ioe) {
		}
	}
}