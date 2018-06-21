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
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MXBean;
import javax.management.ObjectName;

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

import com.sshtools.forker.client.EffectiveUserFactory;
import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.client.OSCommand;
import com.sshtools.forker.common.CSystem;
import com.sshtools.forker.common.Cookie.Instance;
import com.sshtools.forker.common.IO;
import com.sshtools.forker.common.OS;
import com.sshtools.forker.common.Priority;
import com.sshtools.forker.common.Util;
import com.sshtools.forker.daemon.CommandHandler;
import com.sshtools.forker.daemon.Forker;
import com.sshtools.forker.daemon.Forker.Client;
import com.sshtools.forker.wrapper.JVM.Version;

/**
 * <i>Forker Wrapper</i> can be used to wrap java applications in a manner
 * similar to other Java projects such as JSW, YAJSW and more. It provides the
 * following features :-
 * <ul>
 * <li>Multiple ways of configuration, including via command line options,
 * system properties, configuration files and environment variables.</li>
 * <li>JVM selection</li>
 * <li>JVM timeout detection</li>
 * <li>Run as administrator or another user</li>
 * <li>Run in background</li>
 * <li>Capture output to log</li>
 * <li>Restart on certain exit codes</li>
 * <li>Run scripts or Java classes on events</li>
 * <li>Wrap native commands</li>
 * <li>Embeddable or standalone use</li>
 * <li>Single instance enforcement</li>
 * <li>PID file writing</li>
 * <li>Process priority and affinity</li>
 * <li>Forker Daemon integration</li>
 * <li>Wildcard classpaths</li>
 * <li>.. and more</li>
 * </ul>
 *
 */
@MXBean
public class ForkerWrapper implements ForkerWrapperMXBean {
	public final static String EXITED_WRAPPER = "exited-wrapper";
	public final static String EXITING_WRAPPER = "exiting-wrapper";
	public final static String STARTING_FORKER_DAEMON = "started-forker-daemon";
	public final static String STARTED_FORKER_DAEMON = "started-forker-daemon";
	public final static String STARTING_APPLICATION = "starting-application";
	public final static String STARTED_APPLICATION = "started-application";
	public final static String RESTARTING_APPLICATION = "restarting-application";
	public final static String APPPLICATION_STOPPED = "application-stopped";
	public final static String[] EVENT_NAMES = { EXITED_WRAPPER, EXITING_WRAPPER, STARTING_FORKER_DAEMON, STARTED_FORKER_DAEMON,
			STARTED_APPLICATION, STARTING_APPLICATION, RESTARTING_APPLICATION, APPPLICATION_STOPPED };
	private static final String CROSSPLATFORM_PATH_SEPARATOR = ";|:";

	public static class KeyValuePair {
		private String key;
		private String value;
		private boolean bool;

		public KeyValuePair(String line) {
			key = line;
			int idx = line.indexOf('=');
			int spcidx = line.indexOf(' ');
			if (spcidx != -1 && (spcidx < idx || idx == -1)) {
				idx = spcidx;
			}
			if (idx != -1) {
				value = line.substring(idx + 1);
				key = line.substring(0, idx);
			} else {
				bool = true;
			}
		}

		public KeyValuePair(String key, String value) {
			this.key = key;
			this.value = value;
		}

		public String getName() {
			return key;
		}

		public String getValue() {
			return value;
		}

		public boolean isBool() {
			return bool;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

	public enum ArgMode {
		FORCE, APPEND, PREPEND, DEFAULT
	}

	private String classname;
	private String[] arguments;
	private CommandLine cmd;
	private List<KeyValuePair> properties = new ArrayList<KeyValuePair>();
	private Forker daemon;
	private Instance cookie;
	private Process process;
	private boolean tempRestartOnExit;
	private String[] originalArgs;
	private Logger logger = Logger.getLogger(ForkerWrapper.class.getSimpleName());
	private boolean inited;
	private PrintStream defaultOut = System.out;
	private PrintStream defaultErr = System.err;
	private InputStream defaultIn = System.in;
	private boolean stopping = false;
	private boolean preventRestart = false;

	public String[] getArguments() {
		return arguments;
	}

	public InputStream getDefaultIn() {
		return defaultIn;
	}

	public void setDefaultIn(InputStream defaultIn) {
		this.defaultIn = defaultIn;
	}

	public PrintStream getDefaultOut() {
		return defaultOut;
	}

	public void setDefaultOut(PrintStream defaultOut) {
		this.defaultOut = defaultOut;
	}

	public PrintStream getDefaultErr() {
		return defaultErr;
	}

	public void setDefaultErr(PrintStream defaultErr) {
		this.defaultErr = defaultErr;
	}

	public void setArguments(String... arguments) {
		this.arguments = arguments;
	}

	public void setProperty(String key, Object value) {
		for (KeyValuePair nvp : properties) {
			if (nvp.getName().equals(key)) {
				nvp.setValue(String.valueOf(value));
				return;
			}
		}
		properties.add(new KeyValuePair(key, String.valueOf(value)));
	}

	public String getClassname() {
		return classname;
	}

	public File relativize(File context, String path) throws IOException {
		File p = new File(path);
		if (p.isAbsolute()) {
			return p.getCanonicalFile();
		}
		return new File(context, p.getPath()).getCanonicalFile();
	}

	public void restart() throws InterruptedException {
		restart(true);
	}

	public void restart(boolean wait) throws InterruptedException {
		stop(true, true);
	}

	public void stop() throws InterruptedException {
		stop(true);
	}

	public void stop(boolean wait) throws InterruptedException {
		stop(wait, false);
	}

	@SuppressWarnings("resource")
	public int start() throws IOException, InterruptedException {
		if (!inited)
			init(null);
		/*
		 * Calculate CWD. All file paths from this point are calculated relative
		 * to the CWD
		 */
		String cwdpath = getOptionValue("cwd", null);
		File cwd = new File(System.getProperty("user.dir"));
		if (StringUtils.isNotBlank(cwdpath)) {
			cwd = relativize(cwd, cwdpath);
			if (!cwd.exists())
				throw new IOException(String.format("No such directory %s", cwd));
		}
		String javaExe = getJVMPath();
		String forkerClasspath = System.getProperty("java.class.path");
		String wrapperClasspath = getOptionValue("classpath", forkerClasspath);
		String bootClasspath = getOptionValue("boot-classpath", null);
		final boolean nativeMain = getSwitch("native", false);
		final boolean useDaemon = !nativeMain && !getSwitch("no-forker-daemon", nativeMain);
		List<String> jvmArgs = getOptionValues("jvmarg");
		if (nativeMain && StringUtils.isNotBlank(getOptionValue("classpath", null))) {
			throw new IOException("Native main may not be used with classpath option.");
		}
		if (nativeMain && !jvmArgs.isEmpty()) {
			throw new IOException("Native main may not be used with jvmarg option.");
		}
		boolean daemonize = getSwitch("daemon", false);
		String pidfile = getOptionValue("pidfile", null);
		final int exitWait = Integer.parseInt(getOptionValue("exit-wait", "10"));
		if (daemonize(cwd, javaExe, forkerClasspath, daemonize, pidfile))
			return 0;
		/**
		 * LDP: Does not work on OSX. Prevented setProcname from throwing an
		 * exception
		 */
		if (!OS.setProcname(classname)) {
			logger.warning(String.format("Failed to set process name to %s", classname));
		}
		try {
			ManagementFactory.getPlatformMBeanServer().registerMBean(this,
					new ObjectName("com.sshtools.forker.wrapper:type=Wrapper"));
		} catch (Exception e) {
			throw new IOException("Failed to register MBean.", e);
		}
		/*
		 * Create a lock file if 'single instance' was specified
		 */
		FileLock lock = null;
		FileChannel lockChannel = null;
		File lockFile = new File(new File(System.getProperty("java.io.tmpdir")), "forker-wrapper-" + classname + ".lock");
		addShutdownHook(useDaemon, exitWait);
		try {
			if (getSwitch("single-instance", false)) {
				lockChannel = new RandomAccessFile(lockFile, "rw").getChannel();
				try {
					logger.info(String.format("Attempting to acquire lock on %s", lockFile));
					lock = lockChannel.tryLock();
					if (lock == null)
						throw new OverlappingFileLockException();
					lockFile.deleteOnExit();
				} catch (OverlappingFileLockException ofle) {
					throw new IOException(String.format("The application %s is already running.", classname));
				}
			}
			if (useDaemon) {
				monitorWrappedApplication();
			}
			int retval = 2;
			int times = 0;
			int lastRetVal = -1;
			while (true) {
				times++;
				stopping = false;
				process = null;
				boolean quiet = getSwitch("quiet", false);
				boolean quietStdErr = quiet || getSwitch("quiet-stderr", false);
				boolean quietStdOut = quiet || getSwitch("quiet-stdout", false);
				boolean logoverwrite = getSwitch("log-overwrite", false);
				/* Build the command to launch the application itself */
				ForkerBuilder appBuilder = new ForkerBuilder();
				if (!nativeMain) {
					appBuilder.command().add(javaExe);
					String classpath = buildClasspath(cwd, getSwitch("no-forker-classpath", false) ? null : forkerClasspath,
							wrapperClasspath, true);
					if (classpath != null) {
						appBuilder.command().add("-classpath");
						appBuilder.command().add(classpath);
					}
					boolean hasBootCp = false;
					for (String val : jvmArgs) {
						if (val.startsWith("-Xbootclasspath"))
							hasBootCp = true;
						appBuilder.command().add(val);
					}
					if (!hasBootCp) {
						String bootcp = buildClasspath(cwd, null, bootClasspath, false);
						if (bootcp != null && !bootcp.equals("")) {
							/*
							 * Do our own processing of append/prepend as there
							 * are special JVM arguments for it
							 */
							if (bootClasspath != null && bootClasspath.startsWith("+"))
								appBuilder.command().add("-Xbootclasspath/a:" + bootcp);
							else if (bootClasspath != null && bootClasspath.startsWith("-"))
								appBuilder.command().add("-Xbootclasspath/p:" + bootcp);
							else
								appBuilder.command().add("-Xbootclasspath:" + bootcp);
						}
					}
				}
				if (!getSwitch("no-info", false)) {
					if (lastRetVal > -1) {
						appBuilder.command().add(String.format("-Dforker.info.lastExitCode=%d", lastRetVal));
					}
					appBuilder.command().add(String.format("-Dforker.info.attempts=%d", times));
				}
				/*
				 * If the daemon should be used, we assume that forker-client is
				 * on the classpath and execute the application via that, pssing
				 * the forker daemon cookie via stdin. *
				 */
				if (useDaemon) {
					/*
					 * Otherwise we are just running the application directly
					 */
					appBuilder.command().add(com.sshtools.forker.client.Forker.class.getName());
					appBuilder.command().add(String.valueOf(OS.isAdministrator()));
					appBuilder.command().add(classname);
					if (arguments != null)
						appBuilder.command().addAll(Arrays.asList(arguments));
				} else {
					/*
					 * Otherwise we are just running the application directly
					 */
					appBuilder.command().add(classname);
					if (arguments != null)
						appBuilder.command().addAll(Arrays.asList(arguments));
				}
				String priStr = getOptionValue("priority", null);
				if (priStr != null) {
					appBuilder.priority(Priority.valueOf(priStr));
				}
				appBuilder.io(IO.DEFAULT);
				appBuilder.directory(cwd);
				/* Environment variables */
				for (String env : getOptionValues("setenv")) {
					String key = env;
					String value = "";
					int idx = env.indexOf('=');
					if (idx != -1) {
						key = env.substring(0, idx);
						value = env.substring(idx + 1);
					}
					appBuilder.environment().put(key, value);
				}
				List<String> cpus = getOptionValues("cpu");
				for (String cpu : cpus) {
					appBuilder.affinity().add(Integer.parseInt(cpu));
				}
				if (getSwitch("administrator", false)) {
					if (!OS.isAdministrator()) {
						logger.info("Raising privileges to administartor");
						appBuilder.effectiveUser(EffectiveUserFactory.getDefault().administrator());
					}
				} else {
					String runas = getOptionValue("run-as", null);
					if (runas != null && !runas.equals(System.getProperty("user.name"))) {
						logger.info(String.format("Switching user to %s", runas));
						appBuilder.effectiveUser(EffectiveUserFactory.getDefault().getUserForUsername(runas));
					}
				}
				daemon = null;
				cookie = null;
				if (useDaemon) {
					startForkerDaemon();
				}
				event(STARTING_APPLICATION, String.valueOf(times), cwd.getAbsolutePath(), classname, String.valueOf(lastRetVal));
				process = appBuilder.start();
				event(STARTED_APPLICATION, classname);
				if (useDaemon) {
					process.getOutputStream().write(cookie.toString().getBytes("UTF-8"));
					process.getOutputStream().write("\r\n".getBytes("UTF-8"));
					process.getOutputStream().flush();
				}
				String logpath = getOptionValue("log", null);
				String errpath = getOptionValue("errors", null);
				if (errpath == null)
					errpath = logpath;
				OutputStream outlog = null;
				OutputStream errlog = null;
				long logDelay = Long.parseLong(getOptionValue("log-write-delay", "50"));
				if (StringUtils.isNotBlank(logpath)) {
					logger.info(String.format("Writing stdout output to %s", logpath));
					outlog = new LazyLogStream(logDelay, makeDirectoryForFile(relativize(cwd, logpath)), !logoverwrite);
				}
				if (errpath != null) {
					if (Objects.equals(logpath, errpath))
						errlog = outlog;
					else {
						logger.info(String.format("Writing stderr output to %s", logpath));
						errlog = new LazyLogStream(logDelay, makeDirectoryForFile(relativize(cwd, errpath)), !logoverwrite);
					}
				}
				OutputStream stdout = quietStdOut ? null : defaultOut;
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
				OutputStream stderr = quietStdErr ? null : defaultErr;
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
						inThread = new Thread(copyRunnable(defaultIn, process.getOutputStream()), "StdIn");
						inThread.setDaemon(true);
						inThread.start();
					}
					try {
						copy(process.getInputStream(), out, newBuffer());
					} catch (IOException ioe) {
						if (!stopping)
							throw ioe;
					}
					retval = process.waitFor();
				} finally {
					if (inThread != null) {
						inThread.interrupt();
					}
					errThread.interrupt();
					if (outlog != null && !outlog.equals(defaultOut)) {
						outlog.close();
					}
					if (errlog != null && errlog != outlog && !errlog.equals(defaultErr)) {
						errlog.close();
					}
					if (daemon != null) {
						daemon.shutdown(true);
					}
				}
				List<String> restartValues = Arrays.asList(getOptionValue("restart-on", "").split(","));
				List<String> dontRestartValues = new ArrayList<String>(
						Arrays.asList(getOptionValue("dont-restart-on", "0,1,2").split(",")));
				dontRestartValues.removeAll(restartValues);
				String strret = String.valueOf(retval);
				event(APPPLICATION_STOPPED, strret, classname);
				boolean restart = !preventRestart && (((restartValues.size() == 1 && restartValues.get(0).equals(""))
						|| restartValues.size() == 0 || restartValues.contains(strret)) && !dontRestartValues.contains(strret));
				if (tempRestartOnExit || restart) {
					try {
						tempRestartOnExit = false;
						int waitSec = Integer.parseInt(getOptionValue("restart-wait", "0"));
						if (waitSec == 0)
							throw new NumberFormatException();
						event(RESTARTING_APPLICATION, classname, String.valueOf(waitSec));
						logger.warning(String.format("Process exited with %d, attempting restart in %d seconds", retval, waitSec));
						lastRetVal = retval;
						Thread.sleep(waitSec * 1000);
					} catch (NumberFormatException nfe) {
						event(RESTARTING_APPLICATION, classname, "0");
						logger.warning(String.format("Process exited with %d, attempting restart", retval));
					}
				} else
					break;
			}
			// TODO cant find out why just exiting fails (process stays
			// running).
			// Cant get a trace on what is still running either
			// PtyHelpers.getInstance().kill(PtyHelpers.getInstance().getpid(),
			// 9);
			// OSCommand.run("kill", "-9",
			// String.valueOf(PtyHelpers.getInstance().getpid()));
			return retval;
		} finally {
			if (lock != null) {
				logger.fine(String.format("Release lock %s", lockFile));
				lock.release();
			}
			if (lockChannel != null) {
				lockChannel.close();
			}
			stopping = false;
			preventRestart = false;
			tempRestartOnExit = false;
		}
	}

	public ArgMode getArgMode() {
		return ArgMode.valueOf(getOptionValue("argmode", ArgMode.DEFAULT.name()));
	}

	public List<KeyValuePair> getProperties() {
		return properties;
	}

	public void setClassname(String classname) {
		this.classname = classname;
	}

	public void readConfigFile(File file) throws IOException {
		logger.info(String.format("Loading configuration file %s", file));
		BufferedReader fin = new BufferedReader(new FileReader(file));
		try {
			String line = null;
			while ((line = fin.readLine()) != null) {
				if (!line.trim().startsWith("#") && !line.trim().equals("")) {
					properties.add(new KeyValuePair(ReplacementUtils.replaceSystemProperties(line)));
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

	public static void main(String[] args) {
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
			wrapper.init(cmd);
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
					formatter.printHelp(new PrintWriter(System.err, true), 132, getAppName(),
							"     <application.class.name> [<argument> [<argument> ..]]\n\n"
									+ "Forker Wrapper is used to launch Java applications, optionally changing "
									+ "the user they are run as, providing automatic restarting, signal handling and "
									+ "other facilities that will be useful running applications as a 'service'.\n\n"
									+ "Configuration may be passed to Forker Wrapper in four different ways :-\n\n"
									+ "1. Command line options.\n" + "2. Configuration files (see -c and -C options)\n"
									+ "3. Java system properties. The key of which is option name prefixed with   'forker.' and with - replaced with a dot (.)\n"
									+ "4. Environment variables. The key of which is the option name prefixed with   'FORKER_' (in upper case) with - replaced with _\n\n" 
									+ "You can also narrow any configuration key down to a specific platform by prefixing\n"
									+ "it with one of 'windows', 'mac-osx', 'linux', 'unix' or 'other'. The exact format\n"
									+ "will depend on whether you are using options, files, system properties or environment\n"
									+ "variables. For example, to specify '-XstartOnFirstThread' as a JVM argument for\n"
									+ "only Max OSX as an option, you would use '--mac-osx-jvmarg=\"-XstartOnFirstThread\".",
							opts, 2, 5, "\nProvided by SSHTOOLS Limited.", true);
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
		} catch (Throwable e) {
			System.err.println(String.format("%s: %s\n", wrapper.getClass().getName(), e.getMessage()));
			formatter.printUsage(new PrintWriter(System.err, true), 80,
					String.format("%s  <application.class.name> [<argument> [<argument> ..]]", getAppName()));
			System.exit(1);
		}
		try {
			System.exit(wrapper.start());
		} catch (Throwable e) {
			e.printStackTrace();
			System.err.println(String.format("%s: %s\n", wrapper.getClass().getName(), e.getMessage()));
			formatter.printUsage(new PrintWriter(System.err, true), 80,
					String.format("%s  <application.class.name> [<argument> [<argument> ..]]", getAppName()));
			System.exit(1);
		}
	}

	public void init(CommandLine cmd) {
		if (!inited) {
			this.cmd = cmd;
			String levelName = getOptionValue("level", "WARNING");
			logger.setLevel(Level.parse(levelName));
			inited = true;
		}
	}

	protected void stop(boolean wait, boolean restart) throws InterruptedException {
		stopping = true;
		preventRestart = !restart;
		tempRestartOnExit = restart;
		if (process != null) {
			process.destroy();
			if (wait) {
				process.waitFor();
			}
		}
	}

	protected String getJVMPath() throws IOException {
		String javaExe = getOptionValue("java", System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
		if (SystemUtils.IS_OS_WINDOWS && !javaExe.endsWith(".exe"))
			javaExe += ".exe";
		String minjava = getOptionValue("min-java", null);
		String maxjava = getOptionValue("max-java", null);
		if (StringUtils.isNotBlank(minjava) || StringUtils.isNotBlank(maxjava)) {
			if (StringUtils.isBlank(minjava))
				minjava = "0.0.0";
			if (StringUtils.isBlank(maxjava))
				maxjava = "9999.9999.9999";
			Version minver = new Version(minjava);
			Version maxver = new Version(maxjava);
			JVM jvm = new JVM(javaExe);
			if (jvm.getVersion().compareTo(minver) < 0 || jvm.getVersion().compareTo(maxver) > 0) {
				logger.info(String.format("Initially chosen JVM %s (%s) is not within the JVM version range of %s to %s", javaExe,
						jvm.getVersion(), minver, maxver));
				for (JVM altJvm : JVM.jvms()) {
					if (altJvm.getVersion().compareTo(minver) >= 0 && altJvm.getVersion().compareTo(maxver) <= 0) {
						javaExe = altJvm.getPath();
						logger.info(String.format("Using alternative JVM %s which version %s", javaExe, altJvm.getVersion()));
						break;
					}
				}
			} else {
				logger.info(String.format("Initially chosen JVM %s (%s) is valid for the version range %s to %s", javaExe,
						jvm.getVersion(), minver, maxver));
			}
		}
		return javaExe;
	}

	protected void event(String name, String... args) throws IOException {
		String eventHandler = getOptionValue("on-" + name, null);
		logger.info(String.format("Event " + name + ": %s", Arrays.asList(args)));
		if (StringUtils.isNotBlank(eventHandler)) {
			// Parse the event handler script
			List<String> handlerArgs = Util.parseQuotedString(eventHandler);
			for (int i = 0; i < handlerArgs.size(); i++) {
				handlerArgs.set(i, handlerArgs.get(i).replace("%0", eventHandler));
				for (int j = 0; j < args.length; j++)
					handlerArgs.set(i, handlerArgs.get(i).replace("%" + (j + 1), args[j]));
				handlerArgs.set(i, handlerArgs.get(i).replace("%%", "%"));
			}
			String cmd = handlerArgs.remove(0);
			try {
				if (!cmd.contains("."))
					throw new Exception("Not a class");
				int idx = cmd.indexOf("#");
				String methodName = "main";
				if (idx != -1) {
					methodName = cmd.substring(idx + 1);
					cmd = cmd.substring(0, idx);
				}
				Class<?> clazz = Class.forName(cmd);
				Method method = clazz.getMethod(methodName, String[].class);
				try {
					logger.info(String.format("Handling with Java class %s (%s)", eventHandler, Arrays.asList(args)));
					method.invoke(null, new Object[] { args });
				} catch (Exception e) {
					throw new IOException("Exception thrown during event handler.", e);
				}
			} catch (Exception cnfe) {
				// Assume to be native command
				List<String> allArgs = new ArrayList<String>();
				allArgs.add(cmd);
				allArgs.addAll(handlerArgs);
				try {
					logger.info(String.format("Handling with command %s", allArgs.toString()));
					OSCommand.run(allArgs);
				} catch (Exception e) {
					throw new IOException("Exception thrown during event handler.", e);
				}
			}
		}
	}

	protected void addOptions(Options options) {
		for (String event : EVENT_NAMES) {
			options.addOption(Option.builder().longOpt("on-" + event).hasArg(true).argName("command-or-classname")
					.desc("Executes a script or a Java class (that must be on wrappers own classpath) "
							+ "when a particular event occurs. If a Java class is to be execute, it "
							+ "must contain a main(String[] args) method. Each event may pass a number of arguments.")
					.build());
		}
		options.addOption(new Option("x", "allow-execute", true,
				"The wrapped application can use it's wrapper to execute commands on it's behalf. If the "
						+ "wrapper itself runs under an administrative user, and the application as a non-privileged user,"
						+ "you may wish to restrict which commands may be run. One or more of these options specifies the "
						+ "name of the command that may be run. The value may be a regular expression, see also 'prevent-execute'"));
		options.addOption(new Option("X", "reject-execute", true,
				"The wrapped application can use it's wrapper to execute commands on it's behalf. If the "
						+ "wrapper itself runs under an administrative user, and the application as a non-privileged user,"
						+ "you may wish to restrict which commands may be run. One or more of these options specifies the "
						+ "name of the commands that may NOT be run. The value may be a regular expression, see also 'allow-execute'"));
		options.addOption(new Option("F", "no-forker-classpath", false,
				"When the forker daemon is being used, the wrappers own classpath will be appened to "
						+ "to the application classpath. This option prevents that behaviour for example if "
						+ "the application includes the modules itself."));
		options.addOption(new Option("r", "restart-on", true,
				"Which exit values from the spawned process will cause the wrapper to attempt to restart it. When not specified, all exit "
						+ "values will cause a restart except those that are configure not to (see dont-restart-on)."));
		options.addOption(new Option("R", "dont-restart-on", true,
				"Which exit values from the spawned process will NOT cause the wrapper to attempt to restart it. By default,"
						+ "this is set to 0, 1 and 2. See also 'restart-on'"));
		options.addOption(new Option("w", "restart-wait", true, "How long (in seconds) to wait before attempting a restart."));
		options.addOption(new Option("d", "daemon", false, "Fork the process and exit, leaving it running in the background."));
		options.addOption(new Option("n", "no-forker-daemon", false,
				"Do not enable the forker daemon. This will prevent the forked application from executing elevated commands via the daemon and will also disable JVM timeout detection."));
		options.addOption(new Option("q", "quiet", false, "Do not output anything on stderr or stdout from the wrapped process."));
		options.addOption(new Option("z", "quiet-stderr", false, "Do not output anything on stderr from the wrapped process."));
		options.addOption(new Option("Z", "quiet-stdout", false, "Do not output anything on stdout from the wrapped process."));
		options.addOption(new Option("S", "single-instance", false,
				"Only allow one instance of the wrapped application to be active at any one time. "
						+ "This is achieved through locked files."));
		options.addOption(new Option("s", "setenv", false,
				"Set an environment on the wrapped process. This is in the format NAME=VALUE. The option may be "
						+ "specified multiple times to specify multiple environment variables."));
		options.addOption(new Option("N", "native", false,
				"This option signals that main is not a Java classname, it is instead the name "
						+ "of a native command. This option is incompatible with 'classpath' and also "
						+ "means the forker daemon will not be used and so hang detection and some other "
						+ "features will not be available."));
		options.addOption(new Option("I", "no-info", false,
				"Ordinary, forker will set some system properties in the wrapped application. These "
						+ "communicate things such as the last exited code (forker.info.lastExitCode), number "
						+ "of times start via (forker.info.attempts) and more. This option prevents those being set."));
		options.addOption(new Option("o", "log-overwrite", false, "Overwriite logfiles instead of appending."));
		options.addOption(new Option("l", "log", true,
				"Where to log stdout (and by default stderr) output. If not specified, will be output on stdout (or stderr) of this process."));
		options.addOption(new Option("L", "level", true,
				"Output level for information and debug output from wrapper itself (NOT the application). By default "
						+ "this is WARNING, with other possible levels being FINE, FINER, FINEST, SEVERE, INFO, ALL."));
		options.addOption(new Option("D", "log-write-delay", true,
				"In order to be compatible with external log rotation, log files are closed as soon as they are "
						+ "written to. You can delay the closing of the log file, so that any new log messages that are "
						+ "written within this time will not need to open the file again. The time is in milliseconds "
						+ "with a default of 50ms. A value of zero indicates to always immmediately reopen the log."));
		options.addOption(new Option("e", "errors", true,
				"Where to log stderr. If not specified, will be output on stderr of this process or to 'log' if specified."));
		options.addOption(new Option("cp", "classpath", true,
				"The classpath to use to run the application. If not set, the current runtime classpath is used (the java.class.path system property). Prefix the "
						+ "path with '+' to add it to the end of the existing classpath, or '-' to add it to the start."));
		options.addOption(new Option("bcp", "boot-classpath", true,
				"The boot classpath to use to run the application. If not set, the current runtime classpath is used (the java.class.path system property). Prefix the "
						+ "path with '+' to add it to the end of the existing classpath, or '-' to add it to the start. Use of a jvmarg that starts with '-Xbootclasspath' will "
						+ "override this setting."));
		options.addOption(new Option("u", "run-as", true, "The user to run the application as."));
		options.addOption(new Option("a", "administrator", false, "Run as administrator."));
		options.addOption(new Option("p", "pidfile", true, "A filename to write the process ID to. May be used "
				+ "by external application to obtain the PID to send signals to."));
		options.addOption(new Option("b", "buffer-size", true,
				"How big (in byte) to make the I/O buffer. By default this is 1 byte for immediate output."));
		options.addOption(new Option("B", "cpu", true,
				"Bind to a particular CPU, may be specified multiple times to bind to multiple CPUs."));
		options.addOption(new Option("j", "java", true, "Alternative path to java runtime launcher."));
		options.addOption(
				new Option("J", "jvmarg", true, "Additional VM argument. Specify multiple times for multiple arguments."));
		options.addOption(
				new Option("W", "cwd", true, "Change working directory, the wrapped process will be run from this location."));
		options.addOption(
				new Option("t", "timeout", true, "How long to wait since the last 'ping' from the launched application before "
						+ "considering the process as hung. Requires forker daemon is enabled."));
		options.addOption(new Option("m", "main", true,
				"The classname to run. If this is specified, then the first argument passed to the command "
						+ "becomes the first app argument."));
		options.addOption(new Option("E", "exit-wait", true,
				"How long to wait after attempting to stop a wrapped appllication before giving up and forcibly killing the applicaton."));
		options.addOption(new Option("M", "argmode", true,
				"Determines how apparg options are treated. May be one FORCE, APPEND, PREPEND or DEFAULT. FORCE "
						+ "passed on only the appargs specified by configuration. APPEND will append all appargs to "
						+ "any command line arguments, PREPEND will prepend them. Finally DEFAULT is the default behaviour "
						+ "and any command line arguments will override all appargs."));
		options.addOption(new Option("A", "apparg", true,
				"Application arguments. How these are treated depends on argmode, but by default the will be overridden by any command line arguments passed in."));
		options.addOption(new Option("P", "priority", true,
				"Scheduling priority, may be one of LOW, NORMAL, HIGH or REALTIME (where supported)."));
		options.addOption(new Option("Y", "min-java", true,
				"Minimum java version. If the selected JVM (default or otherwise) is lower than this, an "
						+ "attempt will be made to locate a later version."));
		options.addOption(new Option("y", "max-java", true,
				"Maximum java version. If the selected JVM (default or otherwise) is lower than this, an "
						+ "attempt will be made to locate an earlier version."));
	}

	protected String buildClasspath(File cwd, String defaultClasspath, String classpath, boolean appendByDefault)
			throws IOException {
		boolean append = appendByDefault;
		boolean prepend = false;
		if (classpath != null) {
			if (classpath.startsWith("-")) {
				prepend = true;
				classpath = classpath.substring(1);
			} else if (classpath.startsWith("+")) {
				classpath = classpath.substring(1);
				append = true;
			} else if (classpath.startsWith("=")) {
				classpath = classpath.substring(1);
				append = false;
				prepend = false;
			}
		}
		StringBuilder newClasspath = new StringBuilder();
		if (StringUtils.isNotBlank(classpath)) {
			for (String el : classpath.split(CROSSPLATFORM_PATH_SEPARATOR)) {
				String basename = FilenameUtils.getName(el);
				if (basename.contains("*") || basename.contains("?")) {
					String dirname = FilenameUtils.getFullPathNoEndSeparator(el);
					File dir = relativize(cwd, dirname);
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
		}
		if (StringUtils.isNotBlank(defaultClasspath)) {
			String cp = newClasspath.toString();
			if (append) {
				classpath = defaultClasspath + File.pathSeparator + cp;
			} else if (prepend) {
				classpath = cp + File.pathSeparator + defaultClasspath;
			} else {
				classpath = cp;
			}
		} else
			classpath = newClasspath.toString();
		return classpath;
	}

	protected void startForkerDaemon() throws IOException {
		/*
		 * Prepare to start a forker daemon. The client application may (if it
		 * wishes) include the forker-client module and use the daemon to
		 * execute administrator commands and perform other forker daemon
		 * operations.
		 */
		daemon = new Forker();
		daemon.setIsolated(true);
		/* Prepare command permissions if there are any */
		CommandHandler cmd = daemon.getHandler(CommandHandler.class);
		CheckCommandPermission permi = cmd.getExecutor(CheckCommandPermission.class);
		permi.setAllow(getOptionValues("allow-execute"));
		permi.setReject(getOptionValues("reject-execute"));
		cookie = daemon.prepare();
		event(STARTING_FORKER_DAEMON, cookie.getCookie(), String.valueOf(cookie.getPort()));
		new Thread() {
			public void run() {
				try {
					daemon.start(cookie);
					event(STARTED_FORKER_DAEMON, cookie.getCookie(), String.valueOf(cookie.getPort()));
				} catch (IOException e) {
				}
			}
		}.start();
	}

	protected boolean daemonize(File cwd, String javaExe, String forkerClasspath, boolean daemonize, String pidfile)
			throws IOException {
		if (daemonize && getOptionValue("fallback-active", null) == null) {
			if ("true".equals(getOptionValue("native-fork", "false"))) {
				/*
				 * This doesn't yet work because of how JNA / Pty4J work with
				 * their native library extraction. The forked VM will not
				 * completely exit. It you use 'pstack' to show the native stack
				 * of the process, it will that it is in a native call for a
				 * file that has been deleted (when the parent process exited).
				 * Both of these libraries by default will extract the native
				 * libraries to files, and mark them as to be deleted when JVM
				 * exit. Because once forked, the original JVM does exit, these
				 * files are deleted, but they are needed by the forked process.
				 */
				logger.info("Running in background using native fork");
				int pid = CSystem.INSTANCE.fork();
				if (pid > 0) {
					if (pidfile != null) {
						FileUtils.writeLines(makeDirectoryForFile(relativize(cwd, pidfile)), Arrays.asList(String.valueOf(pid)));
					}
					return true;
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
				logger.info("Exiting initial runtime");
				return true;
			}
		} else {
			if (pidfile != null) {
				int pid = OS.getPID();
				logger.info(String.format("Writing PID %d", pid));
				FileUtils.writeLines(makeDirectoryForFile(relativize(cwd, pidfile)), Arrays.asList(String.valueOf(pid)));
			}
		}
		return false;
	}

	protected void addShutdownHook(final boolean useDaemon, final int exitWait) {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					event(EXITING_WRAPPER);
				} catch (IOException e1) {
				}
				logger.info("In Shutdown Hook");
				Process p = process;
				Forker f = daemon;
				if (p != null && useDaemon && f != null) {
					/*
					 * Close the client control connection. This will cause the
					 * wrapped process to System.exit(), and so cleanly shutdown
					 */
					List<Client> clients = f.getClients();
					synchronized (clients) {
						for (Client c : clients) {
							if (c.getType() == 2) {
								try {
									logger.info("Closing control connection");
									c.close();
								} catch (IOException e) {
								}
							}
						}
					}
				} else {
					/* Not using daemon, so just destroy process */
					p.destroy();
				}
				final Thread current = Thread.currentThread();
				Thread exitWaitThread = null;
				if (exitWait > 0) {
					exitWaitThread = new Thread() {
						{
							setDaemon(true);
							setName("ExitMonitor");
						}

						public void run() {
							try {
								Thread.sleep(exitWait * 1000);
								current.interrupt();
							} catch (InterruptedException e) {
							}
						}
					};
					exitWaitThread.start();
				}
				/* Now wait for it to actually exit */
				try {
					logger.info("Closing control connection");
					p.waitFor();
					try {
						event(EXITED_WRAPPER);
					} catch (IOException e1) {
					}
				} catch (InterruptedException e) {
					p.destroy();
				} finally {
					if (exitWaitThread != null) {
						exitWaitThread.interrupt();
					}
				}
			}
		});
	}

	protected void monitorWrappedApplication() {
		final int timeout = Integer.parseInt(getOptionValue("timeout", "60"));
		if (timeout > 0) {
			new Thread() {
				{
					setName("ForkerWrapperMonitor");
					setDaemon(true);
				}

				public void run() {
					logger.info("Monitoring pings from wrapped application");
					try {
						while (!stopping) {
							if (process != null && daemon != null) {
								WrapperHandler wrapper = daemon.getHandler(WrapperHandler.class);
								if (wrapper.getLastPing() > 0
										&& (wrapper.getLastPing() + timeout * 1000) <= System.currentTimeMillis()) {
									logger.warning(String
											.format("Process has not sent a ping in %d seconds, attempting to terminate", timeout));
									tempRestartOnExit = true;
									/*
									 * TODO may need to be more forceful than
									 * this, e.g. OS kill
									 */
									process.destroy();
								}
							}
							Thread.sleep(1000);
						}
					} catch (InterruptedException ie) {
					}
				}
			}.start();
		}
	}

	protected void appendPath(StringBuilder newClasspath, String el) {
		if (newClasspath.length() > 0)
			newClasspath.append(File.pathSeparator);
		newClasspath.append(el);
	}

	protected boolean getSwitch(String key, boolean defaultValue) {
		if (cmd != null && cmd.hasOption(key))
			return true;
		if (isBool(key)) {
			return true;
		}
		return !"false".equals(getOptionValue(key, String.valueOf(defaultValue)));
	}

	protected boolean isBool(String key) {
		for (KeyValuePair nvp : properties) {
			if (nvp.getName().equals(key))
				return nvp.isBool();
		}
		return false;
	}

	protected String getProperty(String key) {
		for (KeyValuePair nvp : properties) {
			if (nvp.getName().equals(key))
				return nvp.getValue();
		}
		return null;
	}

	protected List<String> getOptionValues(String key) {
		String os = getOsPrefix();
		
		String[] vals = cmd == null ? null : cmd.getOptionValues(key);
		if (vals != null)
			return Arrays.asList(vals);
		List<String> valList = new ArrayList<String>();
		for (KeyValuePair nvp : properties) {
			if ((nvp.getName().equals(key) || nvp.getName().equals(os + "-" + key)) && nvp.getValue() != null) {
				valList.add(nvp.getValue());
			}
		}
		/*
		 * System properties, e.g. forker.somevar.1=val, forker.somevar.2=val2
		 */
		List<String> varNames = new ArrayList<String>();
		for (Map.Entry<Object, Object> en : System.getProperties().entrySet()) {
			if (((String) en.getKey()).startsWith("forker." + (key.replace("-", ".")) + ".") || ((String) en.getKey()).startsWith("forker." + os.replace("-", ".") + "." + (key.replace("-", ".")) + ".")) {
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
			if (en.getKey().startsWith("FORKER_" + (key.toUpperCase().replace("-", "_")) + "_") || en.getKey().startsWith("FORKER_" + ((os + "-" + key).toUpperCase().replace("-", "_")) + "_")) {
				varNames.add(en.getKey());
			}
		}
		Collections.sort(varNames);
		for (String vn : varNames) {
			valList.add(System.getenv(vn));
		}
		return valList;
	}

	protected String getOsPrefix() {
		if (SystemUtils.IS_OS_WINDOWS)
			return "windows";
		else if (SystemUtils.IS_OS_MAC_OSX)
			return "mac-osx";
		else if (SystemUtils.IS_OS_LINUX)
			return "linux";
		else if (SystemUtils.IS_OS_UNIX)
			return "unix";
		else
			return "other";
	}

	protected String getOptionValue(String key, String defaultValue) {
		String os = getOsPrefix();
		/* Look for OS specific options in preference */
		String val = cmd == null ? null : cmd.getOptionValue(os + "-" + key);
		if (val == null)
			val = cmd == null ? null : cmd.getOptionValue(key);
		if (val == null) {
			val = System.getProperty("forkerwrapper." + key.replace("-", "."),
					System.getProperty("forkerwrapper." + (os + "." + key).replace("-", ".")));
			if (val == null) {
				val = System.getenv("FORKER_" + ( os + "-" + key).replace("-", "_").toUpperCase());
				if (val == null) {
					val = System.getenv("FORKER_" + key.replace("-", "_").toUpperCase());
					if (val == null) {
						val = getProperty(os + "-" + key);
						if (val == null) {
							val = getProperty(key);
							if (val == null)
								val = defaultValue;
						}
					}
				}
			}
		}
		return val;
	}

	private File makeDirectoryForFile(File file) throws IOException {
		File dir = file.getParentFile();
		if (dir != null && !dir.exists() && !dir.mkdirs())
			throw new IOException(String.format("Failed to create directory %s", dir));
		return file;
	}

	private byte[] newBuffer() {
		return new byte[Integer.parseInt(getOptionValue("buffer-size", "1024"))];
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
				}
			}
		};
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
		List<String> arguments = getOptionValues("apparg");
		ArgMode argMode = getArgMode();
		if (argMode != ArgMode.FORCE) {
			if (!args.isEmpty()) {
				switch (argMode) {
				case APPEND:
					arguments.addAll(0, args);
					break;
				case PREPEND:
					arguments.addAll(args);
					break;
				default:
					arguments = args;
					break;
				}
			}
		}
		this.arguments = arguments.toArray(new String[0]);
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
