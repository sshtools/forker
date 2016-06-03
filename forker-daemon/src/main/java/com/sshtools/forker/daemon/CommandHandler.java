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
	
	@SuppressWarnings("unchecked")
	public <T extends CommandExecutor> T getExecutor(Class<T> handler) {
		for(CommandExecutor h : executors) {
			if(h.getClass().equals(handler))
				return (T)h;
		}
		return null;
	}

	@Override
	public void handle(Forker forker, DataInputStream din, DataOutputStream dout) throws IOException {
		dout.writeInt(States.OK);
		dout.flush();
		Command cmd = new Command(din);
		CommandExecutor toExecute = null;
		for (CommandExecutor e : executors) {
			ExecuteCheckResult res = e.willHandle(forker, cmd);
			if(res == ExecuteCheckResult.NO)
				throw new IOException("Execution refused.");
			else if(res == ExecuteCheckResult.YES) {
				if(toExecute == null)
					toExecute = e;
			}
		}
		if(toExecute == null)
			throw new IOException("Unwilling to execute.");
		
		toExecute.handle(forker, din, dout, cmd);
		
		
	}

	@Override
	public void stop() {
	}

}
