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

/**
 * This class carries all of the detail for the command to launch and may be
 * serialised and sent to the <i>Forker Daemon</i>, which will deserialise and
 * actually run it.
 *
 */
public class Command {
	@SuppressWarnings("serial")
	private List<String> arguments = new ArrayList<String>() {

		@Override
		public boolean add(String e) {
			if(e == null)
				throw new NullPointerException();
			return super.add(e);
		}
		
	};
	private boolean redirectError;
	private File directory;
	private Map<String, String> environment;
	private String runAs = "";
	private IO io = IO.IO;
	private Priority priority = null;
	private List<Integer> affinity = new ArrayList<Integer>();

	/**
	 * Constructor
	 */
	public Command() {
		environment = new ProcessBuilder("dummy").environment();
	}

	/**
	 * Construct a new command given the serialisation stream. The order of
	 * attributes must be as per {@link Command#write(DataOutputStream)}.
	 * 
	 * @param din
	 *            input stream
	 * @throws IOException
	 *             on any error
	 */
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
		io = IO.DefaultIO.valueOf(din.readUTF());
		String priStr = din.readUTF();
		priority = priStr.equals("") ? null : Priority.valueOf(priStr);
	}

	/**
	 * Get the list of processes this command should be bound to. An empty list
	 * means the default OS behavior should be used.
	 * 
	 * @return processor affinity
	 */
	public List<Integer> getAffinity() {
		return affinity;
	}

	/**
	 * Get whether stderr should be redirected to stdout.
	 * 
	 * @return redirect error stream
	 */
	public boolean isRedirectError() {
		return redirectError;
	}

	/**
	 * Set whether stderr should be redirected to stdout.
	 * 
	 * @param redirectError
	 *            redirect error stream
	 */
	public void setRedirectError(boolean redirectError) {
		this.redirectError = redirectError;
	}

	/**
	 * Get the working directory this command should run in.
	 * 
	 * @return directory
	 */
	public File getDirectory() {
		return directory;
	}

	/**
	 * Set the working directory this command should run in.
	 * 
	 * @param directory
	 *            directory
	 */
	public void setDirectory(File directory) {
		this.directory = directory;
	}

	/**
	 * Get the environment variables that will be passed to the command.
	 * 
	 * @return environment variables
	 */
	public Map<String, String> getEnvironment() {
		return environment;
	}

	/**
	 * Set the environment variables that will be passed to the command.
	 * 
	 * @param environment
	 *            environment variables
	 */
	public void setEnvironment(Map<String, String> environment) {
		this.environment = environment;
	}

	/**
	 * Get the priority the command should run under.
	 * 
	 * @return priority
	 */
	public Priority getPriority() {
		return priority;
	}

	/**
	 * Set the priority the command should run under.
	 * 
	 * @param priority
	 *            priority
	 */
	public void setPriority(Priority priority) {
		this.priority = priority;
	}

	/**
	 * Get the I/O mode that should be used.
	 * 
	 * @return I/O mode
	 */
	public IO getIO() {
		return io;
	}

	/**
	 * Set the I/O mode that should be used.
	 * 
	 * @param io
	 *            I/O mode
	 */
	public void setIO(IO io) {
		this.io = io;
	}

	/**
	 * Get the user the command should run as. If this is <code>null</code>, the
	 * user will be run under the same user as the either the current runtime or
	 * the daemon.
	 * 
	 * @return user to run as
	 */
	public String getRunAs() {
		return runAs;
	}

	/**
	 * Set the user the command should run as. If this is <code>null</code>, the
	 * user will be run under the same user as the either the current runtime or
	 * the daemon.
	 * 
	 * @param runAs
	 *            user to run as
	 */
	public void setRunAs(String runAs) {
		this.runAs = runAs;
	}

	/**
	 * Get the list of command arguments. The list may be modified directly.
	 * 
	 * @return arguments
	 */
	public List<String> getArguments() {
		return arguments;
	}

	/**
	 * Serialise this command to a stream. This data may be used to construct another
	 * {@link Command} (see the constructors).
	 * 
	 * @param dout output stream to write command data to
	 * @throws IOException on any error
	 */
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
				+ ", runAs=" + runAs + ", io=" + io + ", priority=" + priority + ", affinity=" + affinity + "]";
	}

	/**
	 * Get all of the arguments that will actually be run. This will be the contents of {@link #getArguments()},
	 * but adjusted to include wrapper commands that may do things such as change the priority.
	 * 
	 * @return all arguments
	 */
	public List<String> getAllArguments() {
		List<String> a = new ArrayList<String>(arguments);
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
				if (a.size() < 3 || !a.get(0).equals("CMD.EXE") || !a.get(1).equals("/C")
						|| !a.get(2).equals("START")) {
					a.add(0, "CMD.EXE");
					a.add(1, "/C");
					a.add(2, "START");
				}
				a.add(3, "/WAIT");
				a.add(4, "/B");
				switch (priority) {
				case REALTIME:
					a.add(5, "/REALTIME");
					break;
				case HIGH:
					a.add(5, "/HIGH");
					break;
				case NORMAL:
					a.add(5, "/NORMAL");
					break;
				case LOW:
					a.add(5, "/LOW");
					break;
				default:
					break;
				}
				// http://stackoverflow.com/questions/154075/using-the-dos-start-command-with-parameters-passed-to-the-started-program
				a.add(6, "\"Forker\"");
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
				if (a.size() < 3 || !a.get(0).equals("CMD.EXE") || !a.get(1).equals("/C")
						|| !a.get(2).equals("START")) {
					a.add(0, "CMD.EXE");
					a.add(1, "/C");
					a.add(2, "START");
				}
				a.add(3, "/AFFINITY");
				a.add(4, String.format("0x%x", mask));
			} else
				throw new UnsupportedOperationException();
		}
		return a;
	}
}
