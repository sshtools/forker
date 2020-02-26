package com.sshtools.forker.client;

import java.io.File;
import java.io.IOException;
import java.security.AccessControlException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.sshtools.forker.common.Command;
import com.sshtools.forker.common.IO;
import com.sshtools.forker.common.OS;
import com.sshtools.forker.common.Priority;
import com.sshtools.forker.common.Util;

/**
 * A replacement for {@link ProcessBuilder} that uses the Forker framework to
 * launch operating system processes (and returning a compatible {@link Process}
 * ).
 * <p>
 * Processes may be configured to use any one of a number of pluggable 'IO'
 * modes that determine how the process will be handle. The Forker framework
 * makes an effort to configure all defaults sensible for operating system.
 * <p>
 * Priority, affinity and background attributes may be set, as well privileges
 * escalated (to administrator) or switched to another user. Helpers are
 * provided for launching Java based processes using the same runtime (and
 * optionally classpath).
 * <p>
 * This class can also integrate with <i>Forker Daemon</i> to provide a way of
 * lowering fork costs of launching processes on Linux, pseudo terminals for
 * real interactive shells, persistent administrator access (
 * {@link Forker#loadDaemon(boolean)}) and access to administrator only files.
 * <p>
 * It can also integrate with <i>Forker Wrapper</i> to wrap Java or native
 * processes providing restart, logging, Java version detection and more.
 *
 */
public class ForkerBuilder {
	private boolean background;
	private Command command = new Command();
	private ForkerConfiguration configuration;
	private EffectiveUser effectiveUser;

	/**
	 * Construct a new builder given a list of command arguments. The first
	 * element is the command to execute, anything remaining is passed to this
	 * command as arguments.
	 * 
	 * @param configuration configuration
	 * @param command command and command arguments
	 */
	public ForkerBuilder(ForkerConfiguration configuration, List<String> command) {
		if (command == null)
			throw new NullPointerException();
		this.configuration = configuration;
		this.command.getArguments().addAll(command);
		initBuilder();
	}

	/**
	 * Construct a new builder given an array of command arguments. The first
	 * element is the command to execute, anything remaining is passed to this
	 * command as arguments.
	 * 
	 * @param configuration configuration
	 * @param command command and command arguments
	 */
	public ForkerBuilder(ForkerConfiguration configuration, String... command) {
		this.configuration = configuration;
		this.command.getArguments().addAll(Arrays.asList(command));
		initBuilder();
	}

	/**
	 * Construct a new builder given a list of command arguments. The first
	 * element is the command to execute, anything remaining is passed to this
	 * command as arguments.
	 * 
	 * @param command command and command arguments
	 */
	public ForkerBuilder(List<String> command) {
		this(ForkerConfiguration.getDefault(), command);
	}

	/**
	 * Construct a new builder given an array of command arguments. The first
	 * element is the command to execute, anything remaining is passed to this
	 * command as arguments.
	 * 
	 * @param command command and command arguments
	 */
	public ForkerBuilder(String... command) {
		this(ForkerConfiguration.getDefault(), command);
	}

	/**
	 * Get the process affinity list. To bind to particular CPUs, their numbers
	 * should be added to this list. If this list is empty, the process will be
	 * bound according to normal OS rules.
	 * 
	 * @return cpu affinity list
	 */
	public List<Integer> affinity() {
		return command.getAffinity();
	}

	/**
	 * Get whether or not this process will be launched in the background.
	 * 
	 * @return background
	 */
	public boolean background() {
		return background;
	}

	/**
	 * Set whether or not this process will be launched in the background.
	 * 
	 * @param background whether to run in background or not
	 * @return this for chaining
	 */
	public ForkerBuilder background(boolean background) {
		this.background = background;
		return this;
	}

	/**
	 * Get the current command arguments. This list may be freely modified.
	 * 
	 * @return command
	 */
	public List<String> command() {
		return command.getArguments();
	}

	/**
	 * Set the current list of command arguments, replacing all those set
	 * before. The first element is the command to execute, anything remaining
	 * is passed to this command as arguments.
	 * 
	 * @param command command and command arguments
	 * @return this for chaining
	 */
	public ForkerBuilder command(List<String> command) {
		if (command == null)
			throw new NullPointerException();
		this.command.getArguments().clear();
		this.command.getArguments().addAll(command);
		return this;
	}

	/**
	 * Set the current list of command arguments, replacing all those set
	 * before. The first element is the command to execute, anything remaining
	 * is passed to this command as arguments.
	 * 
	 * @param command command and command arguments
	 * @return this for chaining
	 */
	public ForkerBuilder command(String... command) {
		this.command.getArguments().clear();
		this.command.getArguments().addAll(Arrays.asList(command));
		return this;
	}

	/**
	 * Get the configuration being used for this forker instance.
	 * 
	 * @return configuration
	 */
	public ForkerConfiguration configuration() {
		return configuration;
	}

	/**
	 * Get the working directory the process will be executed in.
	 * 
	 * @return directory
	 */
	public File directory() {
		return command.getDirectory();
	}

	/**
	 * Set the working directory the process will be executed in.
	 * 
	 * @param directory directory
	 * @return this for chaining
	 */
	public ForkerBuilder directory(File directory) {
		this.command.setDirectory(directory);
		return this;
	}

	/**
	 * Get the user that will be used to launch the process. See
	 * {@link EffectiveUserFactory#administrator()} and
	 * {@link EffectiveUserFactory#getUserForUsername(String)}.
	 * 
	 * @return user
	 */
	public EffectiveUser effectiveUser() {
		return effectiveUser;
	}

	/**
	 * Set the user that will be used to launch the process. See
	 * {@link EffectiveUserFactory#administrator()} and
	 * {@link EffectiveUserFactory#getUserForUsername(String)}.
	 * 
	 * @param effectiveUser user
	 * @return this for chaining
	 */
	public ForkerBuilder effectiveUser(EffectiveUser effectiveUser) {
		this.effectiveUser = effectiveUser;
		return this;
	}

	/**
	 * Get the environment to pass to the child process. Values may be added,
	 * removed or changed in this map.
	 * 
	 * @return environment
	 */
	public Map<String, String> environment() {
		return command.getEnvironment();
	}

	/**
	 * Set the environment give an array of name=value pair strings.
	 * 
	 * @param envp environment
	 * @return this for chaining
	 */
	public ForkerBuilder environment(String[] envp) {
		if (envp != null) {
			this.command.getEnvironment().clear();
			for (String envstring : envp) {
				if (envstring.indexOf((int) '\u0000') != -1)
					envstring = envstring.replaceFirst("\u0000.*", "");
				int eqlsign = envstring.indexOf('=', 0);
				if (eqlsign != -1)
					this.command.getEnvironment().put(envstring.substring(0, eqlsign), envstring.substring(eqlsign + 1));
			}
		}
		return this;
	}

	/**
	 * Get all arguments will actually be used to launch a command. This may
	 * include additional arguments that are needed to setup the process
	 * environment, for example, on Linux 'nice' may get added to alter process
	 * priority. There should be little need for client applications to use
	 * this.
	 * 
	 * @return all arguments
	 */
	public List<String> getAllArguments() {
		return command.getAllArguments();
	}

	/**
	 * Get the underling {@link Command} that contains the process launch
	 * attributes. This may be transmitted to a remote JVM and decoded, but
	 * would not normally net to be used by client applications.
	 * 
	 * @return command
	 */
	public Command getCommand() {
		return command;
	}

	/**
	 * Get the list of {@link ForkerProcessFactory}s that will be invoked trying
	 * the handle the process. This is exposed to allow special ordering of the
	 * factories if required.
	 * 
	 * @return factories
	 * @deprecated use {@link ForkerConfiguration#getProcessFactories()}.
	 */
	public List<ForkerProcessFactory> getProcessFactories() {
		return configuration.getProcessFactories();
	}

	/**
	 * Get the {@link IO} mode.
	 * 
	 * @return IO mode
	 */
	public IO io() {
		return command.getIO();
	}

	/**
	 * Set the {@link IO} mode.
	 * 
	 * @param io IO mode
	 * @return this for chaining
	 */
	public ForkerBuilder io(IO io) {
		if ((io == IO.SINK || io == IO.OUTPUT) && redirectErrorStream()) {
			throw new IllegalStateException("Cannot set IO mode '" + io + "' because redirectErrorStream() is true.");
		}
		command.setIO(io);
		return this;
	}

	/**
	 * Clear all of the command arguments and replace them with the path to the
	 * current java runtime launcher and the current CLASSPATH.
	 * 
	 * @return this for chaining
	 */
	public ForkerBuilder java() {
		return java(System.getProperty("java.class.path"));
	}

	/**
	 * Clear all of the command arguments and replace them with the path to the
	 * current java runtime launcher and the specified CLASSPATH. Further
	 * arguments may be added to the command using {@link #command()}.
	 * 
	 * @param classpath classpath
	 * @return this for chaining
	 */
	public ForkerBuilder java(String classpath) {
		this.command.getArguments().clear();
		this.command.getArguments().add(OS.getJavaPath());
		this.command.getArguments().add("-classpath");
		this.command.getArguments().add(classpath);
		return this;
	}

	/**
	 * Parse the string into command arguments, , replacing all those set
	 * before. The first element is the command to execute, anything remaining
	 * is passed to this command as arguments.. See
	 * {@link Util#parseQuotedString(String)} for details on how the string is
	 * parsed.
	 * 
	 * @param command command and command arguments
	 * @return this
	 */
	public ForkerBuilder parse(String command) {
		this.command.getArguments().clear();
		this.command.getArguments().addAll(Util.parseQuotedString(command));
		return this;
	}

	/**
	 * Get the priority that will be used to launch the process.
	 * 
	 * @return priority
	 */
	public Priority priority() {
		return command.getPriority();
	}

	/**
	 * Set the priority that will be used to launch the process.
	 * 
	 * @param priority priority
	 * @return this for chaining
	 */
	public ForkerBuilder priority(Priority priority) {
		command.setPriority(priority);
		return this;
	}

	/**
	 * Get the {@link ForkerProcessFactory} instance that might be used to
	 * launch this process given it's class. This allows specific process types
	 * to be configured.
	 * 
	 * @param clazz class of factory
	 * @param <T> type of factory
	 * @return factory instance
	 * @deprecated use {@link ForkerConfiguration#processFactory(Class)}.
	 */
	public <T extends ForkerProcessFactory> T processFactory(Class<T> clazz) {
		return configuration.processFactory(clazz);
	}

	/**
	 * Get whether the stderr stream is being redirected to the stdout stream.
	 * When <code>true</code> {@link Process#getErrorStream()} may not return a
	 * valid stream, data instead will be mixed with
	 * {@link Process#getInputStream()}.
	 * 
	 * @return redirect error stream
	 */
	public boolean redirectErrorStream() {
		return command.isRedirectError();
	}

	/**
	 * Set whether the stderr stream should be redirected to the stdout stream.
	 * When <code>true</code> {@link Process#getErrorStream()} may not return a
	 * valid stream, data instead will be mixed with
	 * {@link Process#getInputStream()}.
	 * 
	 * @param redirectErrorStream redirect error stream
	 * @return this for chaining
	 */
	public ForkerBuilder redirectErrorStream(boolean redirectErrorStream) {
		if (redirectErrorStream && !command.getIO().isAllowStdErrRedirect()) {
			throw new IllegalStateException("Cannot redirect error stream if using IO mode '" + command.getIO() + "'");
		}
		this.command.setRedirectError(redirectErrorStream);
		return this;
	}

	/**
	 * Start the process and return immediately. Upon exit, an active
	 * {@link Process} will be returned that can be used in the normal way.
	 * 
	 * @return process
	 * @throws IOException on any error
	 */
	public ForkerProcess start() throws IOException {
		return start(null);
	}

	/**
	 * Start the process and return immediately. Upon exit, an active
	 * {@link Process} will be returned that can be used in the normal way.
	 * 
	 * @param <P> type of process
	 * @param listener listener
	 * @return process
	 * @throws IOException on any error
	 */
	@SuppressWarnings("unchecked")
	public <P extends ForkerProcess> P start(ForkerProcessListener listener) throws IOException {
		// Must convert to array first -- a malicious user-supplied
		// list might try to circumvent the security check.
		String[] cmdarray = command.getArguments().toArray(new String[command.getArguments().size()]);
		cmdarray = cmdarray.clone();
		for (String arg : cmdarray)
			if (arg == null)
				throw new NullPointerException();
		// Throws IndexOutOfBoundsException if command is empty
		String prog = cmdarray[0];
		SecurityManager security = System.getSecurityManager();
		if (security != null) {
			security.checkExec(prog);
		}
		String dir = command.getDirectory() == null ? null : command.getDirectory().toString();
		for (int i = 1; i < cmdarray.length; i++) {
			if (cmdarray[i].indexOf('\u0000') >= 0) {
				throw new IOException("invalid null character in command");
			}
		}
		P process = null;
		for (ForkerProcessFactory processFactory : configuration.getProcessFactories()) {
			try {
				process = (P)processFactory.createProcess(this, listener);
				if (process != null)
					return process;
			} catch (IOException e) {
				throw handleIOException(prog, security, dir, e);
			} catch (IllegalArgumentException e) {
				throw handleIllegalArgumentException(prog, dir, e);
			}
		}
		throw new UnsupportedOperationException("No factory was willing to handle this type of process.");
	}

	@Override
	public String toString() {
		return "ForkerBuilder [command=" + command + ", background=" + background + ", effectiveUser=" + effectiveUser + "]";
	}

	protected IOException handleIllegalArgumentException(String prog, String dir, IllegalArgumentException e) {
		String exceptionInfo = ": " + e.getMessage();
		return new IOException(
				"Cannot run program \"" + prog + "\"" + (dir == null ? "" : " (in directory \"" + dir + "\")") + exceptionInfo, e);
	}

	protected IOException handleIOException(String prog, SecurityManager security, String dir, IOException e) throws IOException {
		String exceptionInfo = ": " + e.getMessage();
		Throwable cause = e;
		if (security != null) {
			try {
				security.checkRead(prog);
			} catch (AccessControlException ace) {
				exceptionInfo = "";
				cause = ace;
			}
		}
		return new IOException(
				"Cannot run program \"" + prog + "\"" + (dir == null ? "" : " (in directory \"" + dir + "\")") + exceptionInfo,
				cause);
	}

	protected void initBuilder() {
		if (Forker.isDaemonRunning())
			this.command.setIO(IO.DAEMON);
	}

	protected Process startLocalProcess() throws IOException {
		if (effectiveUser != null) {
			effectiveUser.elevate(this, null, command);
		}
		try {
			List<String> allArguments = command.getAllArguments();
			ProcessBuilder pb = new ProcessBuilder(allArguments);
			if (command.isRedirectError()) {
				pb.redirectErrorStream(true);
			}
			if (command.getDirectory() != null) {
				pb.directory(command.getDirectory());
			}
			if (command.getEnvironment() != null) {
				pb.environment().putAll(command.getEnvironment());
			}
			return pb.start();
		} finally {
			if (effectiveUser != null) {
				effectiveUser.descend(this, null, command);
			}
		}
	}
}
