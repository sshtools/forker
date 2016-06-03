package com.sshtools.forker.daemon;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface Handler {

	int getType();

	void handle(Forker forker, DataInputStream din, DataOutputStream dos) throws IOException;

	void stop();
}
