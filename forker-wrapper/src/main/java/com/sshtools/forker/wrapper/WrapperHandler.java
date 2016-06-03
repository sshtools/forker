package com.sshtools.forker.wrapper;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;

import com.sshtools.forker.common.States;
import com.sshtools.forker.daemon.Forker;
import com.sshtools.forker.daemon.Handler;

public class WrapperHandler implements Handler {

	private long lastPing;

	public WrapperHandler() {
	}

	@Override
	public int getType() {
		return 2;
	}

	public long getLastPing() {
		return lastPing;
	}

	@Override
	public void handle(Forker forker, DataInputStream din, DataOutputStream dos) throws IOException {

		/**
		 * Connection type 2 is held open by the client waiting for reply (that
		 * we never send). So when this runtime dies, the client can detect this
		 * (and also close itself down). This is also used for JVM timeout
		 * detection
		 */

		lastPing = System.currentTimeMillis();
		try {
			while (true) {
				dos.writeInt(States.OK);
				dos.flush();
				din.readByte();
				lastPing = System.currentTimeMillis();
				Thread.sleep(1);
			}
		} catch (InterruptedException ie) {
			throw new EOFException();
		} catch (SocketException se) {
			throw new EOFException();
		}
	}

	@Override
	public void stop() {
		lastPing = 0;		
	}

}
