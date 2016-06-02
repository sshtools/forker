package com.sshtools.forker.wrapper;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;

import com.pty4j.unix.PtyHelpers;
import com.sshtools.forker.client.EffectiveUserFactory;
import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.client.OS;
import com.sshtools.forker.common.Cookie.Instance;
import com.sshtools.forker.common.IO;
import com.sshtools.forker.daemon.Forker;

public class ForkerWrapper {

	public static class NameValuePair {
		private String name;
		private String value;
		private boolean bool;

		public NameValuePair(String line) {
			name = line;
			int idx = line.indexOf('=');
			int spcidx = line.indexOf(' ');
			if (spcidx != -1 && (spcidx < idx || idx == -1)) {
				idx = spcidx;
			}
			if (idx != -1) {
				value = line.substring(idx + 1);
				name = line.substring(0, idx);
			} else {
				bool = true;
			}
		}

		public String getName() {
			return name;
		}

		public String getValue() {
			return value;
		}

		public boolean isBool() {
			return bool;
		}
	}

	private String classname;
	private String[] arguments;
	private CommandLine cmd;
	private List<NameValuePair> properties = new ArrayList<>();
	private Forker daemon;
	private Instance cookie;
	private Process process;
	private boolean tempRestartOnExit;
	private String[] originalArgs;

	protected void addOptions(Options options) {
		options.addOption(new Option("r", "restart-on", true,
				"Which exit values from the spawned process will cause the wrapper to attempt to restart it. When not specified, all exit "
						+ "values will cause a restart except those that are configure not to (see dont-restart-on)."));
		options.addOption(new Option("R", "dont-restart-on", true,
				"Which exit values from the spawned process will NOT cause the wrapper to attempt to restart it. By default,"
						+ "this is set to 0, 1 and 2. See also 'restart-on'"));
		options.addOption(
				new Option("w", "restart-wait", true, "How long (in seconds) to wait before attempting a restart."));
		options.addOption(
				new Option("d", "daemon", false, "Fork the process and exit, leaving it running in the background."));
		options.addOption(new Option("n", "no-forker-daemon", false,
				"Do not enable the forker daemon. This will prevent the forked application from executing elevated commands via the daemon and will also disable JVM timeout detection."));
		options.addOption(new Option("q", "quiet", false,
				"Do not output anything on stderr or stdout from the wrapped process."));
		options.addOption(new Option("o", "log-overwrite", false, "Overwriite logfiles instead of appending."));
		options.addOption(new Option("l", "log", true,
				"Where to log stdout (and by default stderr) output. If not specified, will be output on stdout (or stderr) of this process."));
		options.addOption(new Option("e", "errors", true,
				"Where to log stderr. If not specified, will be output on stderr of this process or to 'log' if specified."));
		options.addOption(new Option("cp", "classpath", true,
				"The classpath to use to run the application. If not set, the current runtime classpath is used (the java.class.path system property)."));
		options.addOption(new Option("u", "run-as", true, "The user to run the application as."));
		options.addOption(new Option("p", "pidfile", true, "A filename to write the process ID to. May be used "
				+ "by external application to obtain the PID to send signals to."));
		options.addOption(new Option("b", "buffer-size", true,
				"How big (in byte) to make the I/O buffer. By default this is 1 byte for immediate output."));
		options.addOption(new Option("j", "java", true, "Alternative path to java runtime launcher."));
		options.addOption(new Option("J", "jvmarg", true,
				"Additional VM argument. Specify multiple times for multiple arguments."));
		options.addOption(new Option("W", "cwd", true,
				"Change working directory, the wrapped process will be run from this location."));
		options.addOption(new Option("t", "timeout", true,
				"How long to wait since the last 'ping' from the launched application before "
						+ "considering the process as hung. Requires forker daemon is enabled."));
		options.addOption(new Option("m", "main", true,
				"The classname to run. If this is specified, then the first argument passed to the command "
						+ "becomes the first app argument."));
		options.addOption(new Option("A", "apparg", true,
				"Application arguments. These are overridden by any application arguments provided on the command line."));
	}

	public String[] getArguments() {
		return arguments;
	}

	public void setArguments(String[] arguments) {
		this.arguments = arguments;
	}

	public String getClassname() {
		return classname;
	}

	public void start() throws IOException, InterruptedException {

		String javaExe = getOptionValue("java",
				System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
		if (SystemUtils.IS_OS_WINDOWS && !javaExe.endsWith(".exe"))
			javaExe += ".exe";

		String forkerClasspath = System.getProperty("java.class.path");
		String wrapperClasspath = getOptionValue("classpath", forkerClasspath);

		boolean daemonize = getSwitch("daemon", false);
		String pidfile = getOptionValue("pidfile", null);
		final int timeout = Integer.parseInt(getOptionValue("timeout", "60"));
		if (daemonize && getOptionValue("fallback-active", null) == null) {

			if ("true".equals(getOptionValue("native-fork", "false"))) {
				/*
				 * This doesn't yet work before of how JNA / Pty4J work with
				 * their native library extraction. The forked VM will not
				 * completely exit. It you use 'pstack' to show the native stack
				 * of the process, it will that it is in a native call for a
				 * file that has been deleted (when the parent process exited)
				 */
				int pid = PtyHelpers.getInstance().fork();
				if (pid > 0) {
					if (pidfile != null) {
						FileUtils.writeLines(new File(pidfile), Arrays.asList(String.valueOf(pid)));
					}
					System.exit(0);
				}
			} else {
				/*
				 * Fallback. Attempt to rebuild the command line. This will not
				 * be exact
				 */
				if (originalArgs == null)
					throw new IllegalStateException("Original arguments must be set.");
				ForkerBuilder fb = new ForkerBuilder(javaExe);
				fb.command().add("-classpath");
				fb.command().add(forkerClasspath);

				for (String s : Arrays.asList("java.library.path", "jna.library.path")) {
					if (System.getProperty(s) != null)
						fb.command().add("-D" + s + "=" + System.getProperty(s));
				}
				fb.environment().put("FORKER_FALLBACK_ACTIVE", "true");

				// Currently needs to be quiet :(
				fb.environment().put("FORKER_QUIET", "true");

				// Doesnt seemm to work
				// fb.environment().put("FORKER_FDOUT", "1");
				// fb.environment().put("FORKER_FDERR", "2");

				fb.command().add(ForkerWrapper.class.getName());
				// fb.command().add("--fdout=1");
				// fb.command().add("--fderr=2");
				fb.command().addAll(Arrays.asList(originalArgs));
				fb.background(true);
				fb.io(IO.OUTPUT);
				fb.start();
				System.exit(0);
			}
		} else {
			if (pidfile != null) {
				FileUtils.writeLines(new File(pidfile),
						Arrays.asList(String.valueOf(PtyHelpers.getInstance().getpid())));
			}
		}

		boolean useDaemon = !getSwitch("no-forker-daemon", false);
		if (useDaemon && timeout > 0) {
			new Thread() {
				{
					setName("ForkerWrapperMonitor");
					setDaemon(true);
				}

				public void run() {
					try {
						while (true) {
							if (process != null && daemon != null && daemon.getLastPing() > 0
									&& (daemon.getLastPing() + timeout * 1000) <= System.currentTimeMillis()) {
								System.err.println(String.format(
										"Process has not sent a ping in %d seconds, attempting to terminate", timeout));
								tempRestartOnExit = true;

								/*
								 * TODO may need to be more forceful than this,
								 * e.g. OS kill
								 */
								process.destroy();
							}
							Thread.sleep(1000);
						}
					} catch (InterruptedException ie) {

					}
				}
			}.start();
		}

		int retval = 2;
		while (true) {
			boolean quiet = getSwitch("quiet", false);
			boolean logoverwrite = getSwitch("log-overwrite", false);

			/* Build the command to launch the application itself */

			ForkerBuilder appBuilder = new ForkerBuilder(javaExe);
			String classpath = wrapperClasspath;
			if (StringUtils.isNotBlank(classpath)) {
				StringBuilder newClasspath = new StringBuilder();
				for (String el : classpath.split(File.pathSeparator)) {
					String basename = FilenameUtils.getName(el);
					if (basename.contains("*") || basename.contains("?")) {
						String dirname = FilenameUtils.getFullPathNoEndSeparator(el);
						File dir = new File(dirname);
						if (dir.isDirectory()) {
							File[] files = dir.listFiles();
							if (files != null) {
								for (File file : files) {
									if (FilenameUtils.wildcardMatch(file.getName(), basename)) {
										if (newClasspath.length() > 0)
											newClasspath.append(File.pathSeparator);
										newClasspath.append(file.getAbsolutePath());
									}
								}
							} else {
								appendPath(newClasspath, el);
							}
						} else {
							appendPath(newClasspath, el);
						}
					} else {
						appendPath(newClasspath, el);
					}
				}

				if (newClasspath.length() > 0) {
					appBuilder.command().add("-classpath");
					appBuilder.command().add(newClasspath.toString());
				}
			}

			for (String val : getOptionValues("jvmarg")) {
				appBuilder.command().add(val);
			}

			/*
			 * If the daemon should be used, we assume that forker-client is on
			 * the classpath and execute the application via that, pssing the
			 * forker daemon cookie via stdin. *
			 */
			if (useDaemon) {
				/*
				 * Otherwise we are just running the application directly
				 */
				appBuilder.command().add(com.sshtools.forker.client.Forker.class.getName());
				appBuilder.command().add(String.valueOf(OS.isAdministrator()));
				appBuilder.command().add(classname);
				appBuilder.command().addAll(Arrays.asList(arguments));
			} else {
				/*
				 * Otherwise we are just running the application directly
				 */
				appBuilder.command().add(classname);
				appBuilder.command().addAll(Arrays.asList(arguments));
			}

			appBuilder.io(IO.DEFAULT);

			String cwd = getOptionValue("cwd", null);
			if (StringUtils.isNotBlank(cwd)) {
				File directory = new File(cwd);
				if (!directory.exists())
					throw new IOException(String.format("No such directory %s", directory));
				appBuilder.directory(directory);
			}

			String runas = getOptionValue("run-as", null);
			if (runas != null && !runas.equals(System.getProperty("user.name"))) {
				appBuilder.effectiveUser(EffectiveUserFactory.getDefault().getUserForUsername(runas));
			}

			daemon = null;
			cookie = null;
			if (useDaemon) {
				/*
				 * Prepare to start a forker daemon. The client application may
				 * (if it wishes) include the forker-client module and use the
				 * daemon to execute administrator commands and perform other
				 * forker daemon operations.
				 */
				daemon = new Forker();
				daemon.setIsolated(true);
				cookie = daemon.prepare();
				new Thread() {

					public void run() {
						try {
							daemon.start(cookie);
						} catch (IOException e) {
						}
					}
				}.start();
			}

			process = appBuilder.start();
			if (useDaemon) {
				PrintWriter pw = new PrintWriter(process.getOutputStream(), true);
				pw.println(cookie.toString());
			} else {
				/*
				 * This is an attempt to make sure any spawned processes are
				 * also killed when this process dies
				 */
				Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
					@Override
					public void run() {
						process.destroy();
					}
				}));
			}

			String logpath = getOptionValue("log", null);
			String errpath = getOptionValue("errors", null);
			if (errpath == null)
				errpath = logpath;

			OutputStream outlog = null;
			OutputStream errlog = null;

			if (StringUtils.isNotBlank(logpath))
				outlog = new FileOutputStream(new File(logpath), !logoverwrite);
			if (errpath != null)
				if (Objects.equals(logpath, errpath))
					errlog = outlog;
				else
					errlog = new FileOutputStream(new File(errpath), !logoverwrite);

			OutputStream stdout = quiet ? null : System.out;
			OutputStream out = null;
			if (stdout != null) {
				if (outlog != null) {
					out = new TeeOutputStream(stdout, outlog);
				} else {
					out = stdout;
				}
			} else if (outlog != null)
				out = outlog;
			if (out == null) {
				out = new SinkOutputStream();
			}

			OutputStream stderr = quiet ? null : System.err;
			OutputStream err = null;
			if (stderr != null) {
				if (errlog != null) {
					err = new TeeOutputStream(stderr, errlog);
				} else {
					err = stderr;
				}
			} else if (errlog != null)
				err = errlog;
			if (err == null) {
				err = out;
			}

			Thread errThread = new Thread(copyRunnable(process.getErrorStream(), err), "StdErr");
			errThread.setDaemon(true);
			errThread.start();
			Thread inThread = null;
			try {
				if (!daemonize) {
					inThread = new Thread(copyRunnable(System.in, process.getOutputStream()), "StdIn");
					inThread.setDaemon(true);
					inThread.start();
				}
				copy(process.getInputStream(), out, newBuffer());

				retval = process.waitFor();
			} finally {
				if (inThread != null) {
					inThread.interrupt();
				}
				errThread.interrupt();
				if (outlog != null && !outlog.equals(System.out)) {
					outlog.close();
				}
				if (errlog != null && errlog != outlog && !errlog.equals(System.err)) {
					errlog.close();
				}
				if (daemon != null) {
					daemon.shutdown(true);
				}
			}

			List<String> restartValues = Arrays.asList(getOptionValue("restart-on", "").split(","));
			List<String> dontRestartValues = Arrays.asList(getOptionValue("dont-restart-on", "0,1,2").split(","));

			String strret = String.valueOf(retval);

			boolean restart = (((restartValues.size() == 1 && restartValues.get(0).equals(""))
					|| restartValues.size() == 0 || restartValues.contains(strret))
					&& !dontRestartValues.contains(strret));

			if (tempRestartOnExit || restart) {
				try {
					tempRestartOnExit = false;
					int waitSec = Integer.parseInt(getOptionValue("restart-wait", "0"));
					if (waitSec == 0)
						throw new NumberFormatException();
					System.err.println(
							String.format("Process exited with %d, attempting restart in %d seconds", retval, waitSec));
					Thread.sleep(waitSec * 1000);
				} catch (NumberFormatException nfe) {
					System.err.println(String.format("Process exited with %d, attempting restart", retval));
				}
			} else
				break;
		}

		// TODO cant find out why just exiting fails (process stays running).
		// Cant get a trace on what is still running either
		// PtyHelpers.getInstance().kill(PtyHelpers.getInstance().getpid(), 9);
		// OSCommand.run("kill", "-9",
		// String.valueOf(PtyHelpers.getInstance().getpid()));
		System.exit(retval);

	}

	protected void appendPath(StringBuilder newClasspath, String el) {
		if (newClasspath.length() > 0)
			newClasspath.append(File.pathSeparator);
		newClasspath.append(el);
	}

	private byte[] newBuffer() {
		return new byte[Integer.parseInt(getOptionValue("buffer-size", "1"))];
	}

	private void copy(final InputStream in, final OutputStream out, byte[] buf) throws IOException {
		int r;
		while ((r = in.read(buf, 0, buf.length)) != -1) {
			out.write(buf, 0, r);
			out.flush();
		}
	}

	private Runnable copyRunnable(final InputStream in, final OutputStream out) {
		return new Runnable() {
			@Override
			public void run() {
				try {
					copy(in, out == null ? new SinkOutputStream() : out, newBuffer());
				} catch (EOFException e) {
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
		};
	}

	protected boolean getSwitch(String key, boolean defaultValue) {
		if (cmd.hasOption(key))
			return true;
		if (isBool(key)) {
			return true;
		}
		return !"false".equals(getOptionValue(key, String.valueOf(defaultValue)));
	}

	protected boolean isBool(String key) {
		for (NameValuePair nvp : properties) {
			if (nvp.getName().equals(key))
				return nvp.isBool();
		}
		return false;
	}

	protected String getProperty(String key) {
		for (NameValuePair nvp : properties) {
			if (nvp.getName().equals(key))
				return nvp.getValue();
		}
		return null;
	}

	protected List<String> getOptionValues(String key) {
		String[] vals = cmd.getOptionValues(key);
		if (vals != null)
			return Arrays.asList(vals);
		List<String> valList = new ArrayList<>();
		for (NameValuePair nvp : properties) {
			if (nvp.getName().equals(key) && nvp.getValue() != null) {
				valList.add(nvp.getValue());
			}
		}

		/*
		 * System properties, e.g. forker.somevar.1=val, forker.somevar.2=val2
		 */
		List<String> varNames = new ArrayList<>();
		for (Map.Entry<Object, Object> en : System.getProperties().entrySet()) {
			if (((String) en.getKey()).startsWith("forker." + (key.replace("-", ".")) + ".")) {
				varNames.add((String) en.getKey());
			}
		}
		Collections.sort(varNames);
		for (String vn : varNames) {
			valList.add(System.getProperty(vn));
		}

		/*
		 * Environment variables, e.g. FORKER_SOMEVAR_1=val,
		 * FORKER_SOMEVAR_2=val2
		 */
		varNames.clear();
		for (Map.Entry<String, String> en : System.getenv().entrySet()) {
			if (en.getKey().startsWith("FORKER_" + (key.toUpperCase().replace("-", "_")) + "_")) {
				varNames.add(en.getKey());
			}
		}
		Collections.sort(varNames);
		for (String vn : varNames) {
			valList.add(System.getenv(vn));
		}
		return valList;
	}

	protected String getOptionValue(String key, String defaultValue) {
		String val = cmd.getOptionValue(key);
		if (val == null) {
			val = System.getProperty("forkerwrapper." + key);
			if (val == null) {
				val = System.getenv("FORKER_" + key.toUpperCase());
				if (val == null) {
					val = getProperty(key);
					if (val == null)
						val = defaultValue;
				}
			}
		}
		return val;
	}

	private void process() throws ParseException, IOException {
		List<String> args = cmd.getArgList();

		String main = getOptionValue("main", null);
		if (main == null) {
			if (args.isEmpty())
				throw new ParseException("Must supply class name of application that contains a main() method.");
			classname = args.remove(0);
		} else
			classname = main;

		arguments = getOptionValues("apparg").toArray(new String[0]);
		if (!args.isEmpty())
			arguments = args.toArray(new String[0]);

	}

	public List<NameValuePair> getProperties() {
		return properties;
	}

	public void setClassname(String classname) {
		this.classname = classname;
	}

	public void readConfigFile(File file) throws IOException {
		BufferedReader fin = new BufferedReader(new FileReader(file));
		try {
			String line = null;
			while ((line = fin.readLine()) != null) {
				if (!line.trim().startsWith("#") && !line.trim().equals("")) {
					properties.add(new NameValuePair(line));
				}
			}
		} finally {
			fin.close();
		}
	}

	public static String getAppName() {
		String an = System.getenv("FORKER_APPNAME");
		return an == null || an.length() == 0 ? ForkerWrapper.class.getName() : an;
	}

	public static void main(String[] args) throws Exception {
		ForkerWrapper wrapper = new ForkerWrapper();
		wrapper.originalArgs = args;

		Options opts = new Options();

		// Add the options always available
		wrapper.addOptions(opts);

		// Add the command line launch options
		opts.addOption(Option.builder("c").argName("file").hasArg()
				.desc("A file to read configuration. The file "
						+ "should contain name=value pairs, where name is the same name as used for command line "
						+ "arguments (see --help for a list of these)")
				.longOpt("configuration").build());

		opts.addOption(Option.builder("C").argName("directory").hasArg()
				.desc("A directory to read configuration files from. Each file "
						+ "should contain name=value pairs, where name is the same name as used for command line "
						+ "arguments (see --help for a list of these)")
				.longOpt("configuration-directory").build());

		opts.addOption(Option.builder("h")
				.desc("Show command line help. When the optional argument is supplied, help will "
						+ "be displayed for the option with that name")
				.optionalArg(true).hasArg().argName("option").longOpt("help").build());

		opts.addOption(Option.builder("O").desc("File descriptor for stdout").optionalArg(true).hasArg().argName("fd")
				.longOpt("fdout").build());

		opts.addOption(Option.builder("E").desc("File descriptor for stderr").optionalArg(true).hasArg().argName("fd")
				.longOpt("fderr").build());

		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		try {
			CommandLine cmd = parser.parse(opts, args);
			wrapper.cmd = cmd;

			String outFdStr = wrapper.getOptionValue("fdout", null);
			if (outFdStr != null) {
				Constructor<FileDescriptor> cons = FileDescriptor.class.getDeclaredConstructor(int.class);
				cons.setAccessible(true);
				FileDescriptor fdOut = cons.newInstance(Integer.parseInt(outFdStr));
				System.setOut(new PrintStream(new FileOutputStream(fdOut), true));
			}

			String errFdStr = wrapper.getOptionValue("fdout", null);
			if (errFdStr != null) {
				Constructor<FileDescriptor> cons = FileDescriptor.class.getDeclaredConstructor(int.class);
				cons.setAccessible(true);
				FileDescriptor fdErr = cons.newInstance(Integer.parseInt(errFdStr));
				System.setErr(new PrintStream(new FileOutputStream(fdErr), true));
			}

			if (cmd.hasOption('h')) {
				String optionName = cmd.getOptionValue('h');
				if (optionName == null) {
					formatter.printHelp(new PrintWriter(System.err, true), 80, getAppName(),
							"     <application.class.name> [<argument> [<argument> ..]]\n\n"
									+ "Forker Wrapper is used to launch Java applications, optionally changing"
									+ "the user they are run as, providing automatic restarting, signal handling and "
									+ "other facilities that will be useful running applications as a 'service'.\n\n"
									+ "Configuration may be passed to Forker Wrapper in four different ways :-\n\n"
									+ "1. Command line options.\n" + "2. Configuration files (see -c and -C options)\n"
									+ "3. Java system properties. The key of which is option name prefixed with   'forker.' and with - replaced with a dot (.)\n"
									+ "4. Environment variables. The key of which is the option name prefixed with   'FORKER_' (in upper case) with - replaced with _\n\n",
							opts, 2, 5, "\nProvided by SSHTools.", true);
					System.exit(1);
				} else {
					Option opt = opts.getOption(optionName);
					if (opt == null) {
						throw new Exception(String.format("No option named", optionName));
					} else {
						System.err.println(optionName);
						System.err.println();
						System.err.println(opt.getDescription());
					}
				}
			}

			if (cmd.hasOption("configuration")) {
				wrapper.readConfigFile(new File(cmd.getOptionValue('c')));
			}
			String cfgDir = wrapper.getOptionValue("configuration-directory", null);
			if (cfgDir != null) {
				File dir = new File(cfgDir);
				if (dir.exists()) {
					for (File f : dir.listFiles()) {
						if (f.isFile() && !f.isHidden())
							wrapper.readConfigFile(f);
					}
				}
			}

			wrapper.process();
		} catch (Exception e) {
			System.err.println(String.format("%s: %s\n", wrapper.getClass().getName(), e.getMessage()));
			formatter.printUsage(new PrintWriter(System.err, true), 80,
					String.format("%s  <application.class.name> [<argument> [<argument> ..]]", getAppName()));
			System.exit(1);
		}

		wrapper.start();
		System.exit(0);
	}

	class SinkOutputStream extends OutputStream {

		@Override
		public void write(int b) throws IOException {
		}

		@Override
		public void write(byte[] b) throws IOException {
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
		}

		@Override
		public void flush() throws IOException {
		}

		@Override
		public void close() throws IOException {
		}

	}
}
