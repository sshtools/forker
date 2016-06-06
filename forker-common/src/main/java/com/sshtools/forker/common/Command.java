package com.sshtools.forker.common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.SystemUtils;

public class Command {
	private List<String> arguments = new ArrayList<String>();
	private boolean redirectError;
	private File directory;
	private Map<String, String> environment;
	private String runAs = "";
	private IO io;
	private Priority priority = null;
	private List<Integer> affinity = new ArrayList<>();

	public Command() {
		environment = new ProcessBuilder("dummy").environment();
	}

	public Command(DataInputStream din) throws IOException {
		int argc = din.readInt();
		for (int i = 0; i < argc; i++) {
			arguments.add(din.readUTF());
		}
		directory = new File(din.readUTF());
		redirectError = din.readBoolean();
		boolean hasEnv = din.readBoolean();
		if (hasEnv) {
			environment = new HashMap<String, String>();
			int envc = din.readInt();
			for (int i = 0; i < envc; i++) {
				environment.put(din.readUTF(), din.readUTF());
			}
		} else {
			// Otherwise pass on our own environment.
			// TODO i think better handling of environment in general is needed.
			// Something for 1.3
			environment = new ProcessBuilder("dummy").environment();
		}
		String r = din.readUTF();
		runAs = r.equals("") ? null : r;
		io = IO.valueOf(din.readUTF());
		String priStr = din.readUTF();
		priority = priStr.equals("") ? null : Priority.valueOf(priStr);
	}

	public List<Integer> getAffinity() {
		return affinity;
	}

	public boolean isRedirectError() {
		return redirectError;
	}

	public void setRedirectError(boolean redirectError) {
		this.redirectError = redirectError;
	}

	public File getDirectory() {
		return directory;
	}

	public void setDirectory(File directory) {
		this.directory = directory;
	}

	public Map<String, String> getEnvironment() {
		return environment;
	}

	public void setEnvironment(Map<String, String> environment) {
		this.environment = environment;
	}

	public Priority getPriority() {
		return priority;
	}

	public void setPriority(Priority priority) {
		this.priority = priority;
	}

	public IO getIO() {
		return io;
	}

	public void setIO(IO io) {
		this.io = io;
	}

	public String getRunAs() {
		return runAs;
	}

	public void setRunAs(String runAs) {
		this.runAs = runAs;
	}

	public List<String> getArguments() {
		return arguments;
	}

	public void write(DataOutputStream dout) throws IOException {
		dout.writeInt(arguments.size());
		for (String s : arguments) {
			dout.writeUTF(s);
		}
		dout.writeUTF(directory == null ? System.getProperty("user.dir") : directory.getAbsolutePath());
		dout.writeBoolean(redirectError);
		dout.writeBoolean(environment != null);
		if (environment != null) {
			dout.writeInt(environment.size());
			for (Map.Entry<String, String> en : environment.entrySet()) {
				dout.writeUTF(en.getKey());
				dout.writeUTF(en.getValue());
			}
		}
		dout.writeUTF(runAs == null ? "" : runAs);
		dout.writeUTF(io.name());
		dout.writeUTF(priority == null ? "" : priority.name());
	}

	@Override
	public String toString() {
		return "Command [arguments=" + arguments + ", redirectError=" + redirectError + ", directory=" + directory
				+ ", environment=" + environment + ", runAs=" + runAs + "]";
	}

	public List<String> getAllArguments() {
		List<String> a = new ArrayList<>(arguments);
		if (priority != null) {
			if (SystemUtils.IS_OS_UNIX) {
				a.add(0, "nice");
				a.add(1, "-n");
				switch (priority) {
				case REALTIME:
					a.add(2, "-20");
					break;
				case HIGH:
					a.add(2, "-10");
					break;
				case NORMAL:
					a.add(2, "10");
					break;
				case LOW:
					a.add(2, "19");
					break;
				default:
					break;
				}
			} else if (SystemUtils.IS_OS_WINDOWS) {
				if (!a.get(0).equals("START")) {
					a.add(0, "START");
				}
				a.add(1, "/WAIT");
				a.add(2, "/B");
				switch (priority) {
				case REALTIME:
					a.add(3, "/REALTIME");
					break;
				case HIGH:
					a.add(3, "/HIGH");
					break;
				case NORMAL:
					a.add(3, "/NORMAL");
					break;
				case LOW:
					a.add(3, "/LOW");
					break;
				default:
					break;
				}
			} else
				throw new UnsupportedOperationException();
		}

		if (!affinity.isEmpty()) {
			long mask = 0;
			for (Integer cpu : affinity) {
				mask = mask | 1 << (cpu - 1);
			}
			if (SystemUtils.IS_OS_UNIX) {
				a.add(0, "taskset");
				a.add(1, String.format("0x%x", mask));
			} else if (SystemUtils.IS_OS_WINDOWS && !(SystemUtils.IS_OS_WINDOWS_XP) && !SystemUtils.IS_OS_WINDOWS_95
					&& !SystemUtils.IS_OS_WINDOWS_98 && !SystemUtils.IS_OS_WINDOWS_ME && !SystemUtils.IS_OS_WINDOWS_NT
					&& !SystemUtils.IS_OS_WINDOWS_VISTA) {
				// Windows 7 and above
				if (!a.get(0).equals("START")) {
					a.add(0, "START");
				}
				a.add(1, "/AFFINITY");
				a.add(2, String.format("0x%x", mask));
			} else
				throw new UnsupportedOperationException();
		}
		return a;
	}
}
