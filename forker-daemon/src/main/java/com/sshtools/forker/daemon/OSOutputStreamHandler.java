package com.sshtools.forker.daemon;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.sshtools.forker.common.States;

public class OSOutputStreamHandler implements Handler {

	public OSOutputStreamHandler() {
	}

	@Override
	public int getType() {
		return 4;
	}

	@Override
	public void handle(Forker forker, DataInputStream din, DataOutputStream dos) throws IOException {
		String filename = din.readUTF();
		boolean append = din.readBoolean();
		FileOutputStream fout = new FileOutputStream(new File(filename), append);
		try {
			dos.writeInt(States.OK);
			dos.flush();
			while (true) {
				int bs = din.readInt();
				if (bs == 0)
					break;
				byte[] buf = new byte[bs];
				din.readFully(buf);
				fout.write(buf);
			}
			fout.flush();
			dos.writeInt(States.OK);
			dos.flush();			
		} finally {
			fout.close();
		}
	}

	@Override
	public void stop() {
	}

}
