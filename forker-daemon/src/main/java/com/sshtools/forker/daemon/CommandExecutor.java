package com.sshtools.forker.daemon;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.sshtools.forker.common.Command;

public interface CommandExecutor {

	ExecuteCheckResult willHandle(Forker forker, Command command);

	void handle(Forker forker, DataInputStream din, DataOutputStream dos, Command command) throws IOException;
}
