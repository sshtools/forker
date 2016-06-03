package com.sshtools.forker.daemon;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import com.sshtools.forker.common.Command;
import com.sshtools.forker.common.States;

public class CommandHandler implements Handler {

	private List<CommandExecutor> executors = new ArrayList<>();

	public CommandHandler() {
		for (CommandExecutor e : ServiceLoader.load(CommandExecutor.class))
			executors.add(e);
	}

	@Override
	public int getType() {
		return 0;
	}

	@Override
	public void handle(Forker forker, DataInputStream din, DataOutputStream dout) throws IOException {
		dout.writeInt(States.OK);
		dout.flush();
		Command cmd = new Command(din);
		for (CommandExecutor e : executors) {
			if (e.willHandle(forker, cmd)) {
				e.handle(forker, din, dout, cmd);
				break;
			}
		}
	}

}
