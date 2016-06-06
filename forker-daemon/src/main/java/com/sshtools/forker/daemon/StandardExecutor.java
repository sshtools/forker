package com.sshtools.forker.daemon;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.sshtools.forker.common.Command;
import com.sshtools.forker.common.IO;
import com.sshtools.forker.common.OS;
import com.sshtools.forker.common.States;
import com.sshtools.forker.common.Util;

public class StandardExecutor implements CommandExecutor {

	public StandardExecutor() {
	}

	@Override
	public ExecuteCheckResult willHandle(Forker forker, Command command) {
		return command.getIO() != IO.PTY ? ExecuteCheckResult.YES : ExecuteCheckResult.DONT_CARE;
	}

	@Override
	public void handle(Forker forker, DataInputStream din, final DataOutputStream dout, Command cmd)
			throws IOException {
		ProcessBuilder builder = new ProcessBuilder(cmd.getAllArguments());

		// TODO way to control this
		Map<String, String> builderEnv = builder.environment();
		builderEnv.putAll(System.getenv());

		Map<String, String> cmdEnvironment = cmd.getEnvironment();
		if (cmdEnvironment != null) {
			cmdEnvironment = new HashMap<String, String>();
			// We want to preserve the path?
			// TODO merge this instead?
			if (builderEnv.containsKey("PATH"))
				cmdEnvironment.remove("PATH");
			if (builderEnv.containsKey("HOME"))
				cmdEnvironment.remove("HOME");
			builderEnv.putAll(cmdEnvironment);
		}

		builder.directory(cmd.getDirectory());
		if (cmd.isRedirectError()) {
			builder.redirectErrorStream(true);
		}

		try {
			if (StringUtils.isNotBlank(cmd.getRunAs())) {
				int uid = -1;
				String username = null;
				try {
					uid = Integer.parseInt(cmd.getRunAs());
					username = Util.getUsernameForID(cmd.getRunAs());
					if (username == null)
						throw new IOException(String.format("Could not determine username for UID %d", uid));
				} catch (NumberFormatException nfe) {
					username = cmd.getRunAs();
				}
				if (!username.equals(System.getProperty("user.name"))) {
					if (OS.isAdministrator()) {
						List<String> args = new ArrayList<String>(cmd.getArguments());
						cmd.getArguments().clear();
						cmd.getArguments().add("su");
						cmd.getArguments().add(username);
						cmd.getArguments().add("-c");
						cmd.getArguments().add(Util.getQuotedCommandString(args).toString());
					} else {
						throw new IOException("Not an administrator.");
					}
				}
			}

			final Process process = builder.start();
			dout.writeInt(States.OK);
			if (!cmd.isRedirectError()) {
				new Thread() {
					public void run() {
						Forker.readStreamToOutput(dout, process.getErrorStream(), States.ERR);
					}
				}.start();
			}

			// Take any input coming the other way
			final Thread input = new InputThread(process.getOutputStream(), din) {
				@Override
				void kill() {
					process.destroy();
				}

				@Override
				void setWindowSize(int width, int height) {
				}
			};
			input.start();
			Forker.readStreamToOutput(dout, process.getInputStream(), States.IN);
			synchronized (dout) {
				dout.writeInt(States.END);
				dout.writeInt(process.waitFor());
				dout.flush();
			}

			// Wait for stream other end to close
			input.join();
		} catch (Throwable t) {
			t.printStackTrace();
			synchronized (dout) {
				dout.writeInt(States.FAILED);
				dout.writeUTF(t.getMessage());
			}
		}
	}

}
