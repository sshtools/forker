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
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MXBean;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

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
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

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
 * <li>Automatic reconfiguration when configuration files change.</li>
 * <li>.. and more</li>
 * </ul>
 *
 */
@MXBean
public class ForkerWrapper implements ForkerWrapperMXBean {

	public final static int CONTINUE_UPGRADE = Integer.MIN_VALUE;
	public final static int USER_INTERACTION = Integer.MIN_VALUE + 2;
	public final static int EXIT_AFTER_UPGRADE = Integer.MIN_VALUE + 1;
	public final static int EXIT_OK = 0;
	public final static int EXIT_ERROR = 1;
	public final static int EXIT_ARGUMENT_SYNTAX = 2;

	public final static String WRAPPED_MODULE_NAME = "com.sshtools.forker.wrapped";
	public final static String WRAPPED_CLASS_NAME = "com.sshtools.forker.wrapped.Wrapped";
	public final static String WRAPPED_MX_BEAN_NAME = "WrappedMXBean";
	public final static String EXITED_WRAPPER = "exited-wrapper";
	public final static String EXITING_WRAPPER = "exiting-wrapper";
	public final static String STARTING_FORKER_DAEMON = "started-forker-daemon";
	public final static String STARTED_FORKER_DAEMON = "started-forker-daemon";
	public final static String STARTING_APPLICATION = "starting-application";
	public final static String STARTED_APPLICATION = "started-application";
	public final static String RESTARTING_APPLICATION = "restarting-application";
	public final static String APPPLICATION_STOPPED = "application-stopped";
	public final static String[] EVENT_NAMES = { EXITED_WRAPPER, EXITING_WRAPPER, STARTING_FORKER_DAEMON,
			STARTED_FORKER_DAEMON, STARTED_APPLICATION, STARTING_APPLICATION, RESTARTING_APPLICATION,
			APPPLICATION_STOPPED };
	private static final String CROSSPLATFORM_PATH_SEPARATOR = ";|:";

	private WrappedApplication app = new WrappedApplication();
	private Configuration configuration = new Configuration();
	private Forker daemon;
	private Instance cookie;
	private Process process;
	private boolean tempRestartOnExit;
	private boolean inited;
	private PrintStream defaultOut = System.out;
	private PrintStream defaultErr = System.err;
	private InputStream defaultIn = System.in;
	private boolean stopping = false;
	private boolean preventRestart = false;
	private ScriptEngine engine;
	private Set<File> files = new LinkedHashSet<>();
	private ScheduledExecutorService configChange;
	private ScheduledFuture<?> changeTask;
	private Thread monitorThread;
	private Thread fileMonThread;

	protected Logger logger = Logger.getGlobal();

	{
		reconfigureLogging();
	}

	public CommandLine getCmd() {
		return configuration.getCmd();
	}

	public WrappedApplication getWrappedApplication() {
		return app;
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

	@Override
	public String[] getArguments() {
		return app.getArguments();
	}

	@Override
	public String getClassname() {
		return app.getClassname();
	}

	@Override
	public String getModule() {
		return app.getModule();
	}

	public File relativize(File context, String path) {
		File p = new File(path);
		try {
			if (p.isAbsolute()) {
				return p.getCanonicalFile();
			}
			return new File(context, p.getPath()).getCanonicalFile();
		} catch (IOException ioe) {
			throw new IllegalArgumentException(String.format("Cannot relativize %s and %s.", context, path));
		}
	}

	@Override
	public void restart() throws InterruptedException {
		restart(true);
	}

	@Override
	public void restart(boolean wait) throws InterruptedException {
		stop(true, true);
	}

	@Override
	public void stop() throws InterruptedException {
		stop(true);
	}

	@Override
	public void stop(boolean wait) throws InterruptedException {
		stop(wait, false);
	}

	@SuppressWarnings("resource")
	public int start() throws IOException, InterruptedException {
		if (!inited)
			init(null);
		/*
		 * Calculate CWD. All file paths from this point are calculated relative to the
		 * CWD
		 */
		String javaExe = getJVMPath();
		String wrapperClasspath = resolveWrapperClasspath();
		String wrapperModulepath = resolveWrapperModulepath();
		String forkerClasspath = System.getProperty("java.class.path");
		String forkerModulepath = System.getProperty("java.module.path");
		String bootClasspath = configuration.getOptionValue("boot-classpath", null);
		final boolean nativeMain = configuration.getSwitch("native", false);
		final boolean useDaemon = !nativeMain && !configuration.getSwitch("no-forker-daemon", nativeMain);
		List<String> jvmArgs = configuration.getOptionValues("jvmarg");
		if (nativeMain && StringUtils.isNotBlank(configuration.getOptionValue("classpath", null))) {
			throw new IOException("Native main may not be used with classpath option.");
		}
		if (nativeMain && StringUtils.isNotBlank(configuration.getOptionValue("modulepath", null))) {
			throw new IOException("Native main may not be used with modulepath option.");
		}
		if (nativeMain && !jvmArgs.isEmpty()) {
			throw new IOException("Native main may not be used with jvmarg option.");
		}
		boolean daemonize = isDaemon();
		String pidfile = configuration.getOptionValue("pidfile", null);
		if (daemonize(javaExe, forkerClasspath, forkerModulepath, daemonize, pidfile))
			return 0;
		/**
		 * LDP: Does not work on OSX. Prevented setProcname from throwing an exception
		 */
		if (app.hasClassname() && !OS.setProcname(app.getClassname())) {
			logger.finest(String.format("Failed to set process name to %s", app.getClassname()));
		}
		try {
			ManagementFactory.getPlatformMBeanServer().registerMBean(this,
					new ObjectName("com.sshtools.forker.wrapper:type=Wrapper"));
		} catch (Exception e) {
			logger.warning("Could not start MBean server, no JMX features will be available.");
		}
		/*
		 * Create a lock file if 'single instance' was specified
		 */
		FileLock lock = null;
		FileChannel lockChannel = null;
		File lockFile = new File(new File(System.getProperty("java.io.tmpdir")),
				"forker-wrapper-" + app.getClassname() + ".lock");
		addShutdownHook(useDaemon);
		try {
			if (isSingleInstance()) {
				lockChannel = new RandomAccessFile(lockFile, "rw").getChannel();
				try {
					logger.info(String.format("Attempting to acquire lock on %s", lockFile));
					lock = lockChannel.tryLock();
					if (lock == null)
						throw new OverlappingFileLockException();
					lockFile.deleteOnExit();
				} catch (OverlappingFileLockException ofle) {
					if (configuration.getSwitch("stop", false)) {
						try {
							return (Integer) executeJmxCommandInApp("shutdown");
						} catch (Exception e) {
							if (e.getCause() instanceof EOFException) {
								return 0;
							} else {
								logger.log(Level.SEVERE, "Failed to send stop command.", e);
								return 1;
							}
						}
					} else {
						/*
						 * Try and connect to the MBean service in the hosted app. If this succeeds,
						 * then we send the arguments to the existing instance to process as it wishes
						 * (e.g. open a window, a file or whatever)
						 */
						try {
							return (Integer) executeJmxCommandInApp("launch", (Object) app.getArguments());
						} catch (Exception e) {
							logger.log(Level.FINE, "Could not discover VM to attach to, giving up.", e);
						}
						throw new IOException(
								String.format("The application %s is already running.", app.getClassname()));
					}
				}
			}

			monitorConfigurationFiles();
			monitorWrappedApplication();

			int retval = 2;
			int times = 0;
			int lastRetVal = -1;
			while (true) {
				times++;
				stopping = false;
				process = null;
				boolean quietStdErr = isQuietStderr();
				boolean quietStdOut = isQuietStdout();
				boolean logoverwrite = isLogOverwrite();

				String tempPath = configuration.getOptionValue("init-temp", null);
				if (tempPath != null) {
					initTempFolder(tempPath, resolveCwd());
				}

				/* Build the command to launch the application itself */
				ForkerBuilder appBuilder = buildCommand(javaExe, forkerClasspath, forkerModulepath, wrapperClasspath,
						wrapperModulepath, bootClasspath, nativeMain, useDaemon, times, lastRetVal);
				daemon = null;
				cookie = null;
				if (useDaemon) {
					startForkerDaemon();
				}
				event(STARTING_APPLICATION, String.valueOf(times), resolveCwd().getAbsolutePath(),
						app.fullClassAndModule(), String.valueOf(lastRetVal));
				logger.info(String.format("Executing: %s", String.join(" ", appBuilder.command())));
				process = appBuilder.start();
				event(STARTED_APPLICATION, app.fullClassAndModule());

				/* The process is now started, capture the streams and log or sink them */
				retval = captureStreams(resolveCwd(), useDaemon, daemonize, quietStdErr, quietStdOut, logoverwrite);

				/* Decide whether to restart the process */
				int rv = maybeRestart(retval, lastRetVal);
				if (rv == Integer.MIN_VALUE)
					break;
				else
					lastRetVal = rv;
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

	private Object executeJmxCommandInApp(String method, Object... args)
			throws MalformedObjectNameException, AttachNotSupportedException, IOException, MalformedURLException,
			InstanceNotFoundException, MBeanException, ReflectionException {
		List<VirtualMachineDescriptor> vms = VirtualMachine.list();
		logger.log(Level.FINE,
				String.format("Executing %s in remote app with arguments %s", method, Arrays.asList(args)));
		for (VirtualMachineDescriptor desc : vms) {
			List<String> dn = Arrays.asList(desc.displayName().split(" "));
			if (dn.contains(WRAPPED_MODULE_NAME + "/" + WRAPPED_CLASS_NAME) || dn.contains(WRAPPED_CLASS_NAME)) {
				ObjectName objectName = new ObjectName(
						String.format("%s:type=basic,name=%s", WRAPPED_MODULE_NAME, WRAPPED_MX_BEAN_NAME));
				VirtualMachine vm = desc.provider().attachVirtualMachine(desc);
				String connectorAddress = vm.getAgentProperties()
						.getProperty("com.sun.management.jmxremote.localConnectorAddress", null);
				if (connectorAddress == null) {
					vm.startLocalManagementAgent();
					connectorAddress = vm.getAgentProperties()
							.getProperty("com.sun.management.jmxremote.localConnectorAddress", null);
					if (connectorAddress == null) {
						throw new IllegalStateException("Could not start local management agent.");
					}
				}
				JMXServiceURL jmxUrl = new JMXServiceURL(connectorAddress);
				MBeanServerConnection s = JMXConnectorFactory.connect(jmxUrl).getMBeanServerConnection();
				String[] classNames = new String[args.length];
				for (int i = 0; i < args.length; i++)
					classNames[i] = args[i].getClass().getName();
				return s.invoke(objectName, method, args, classNames);

			}
		}
		throw new IllegalArgumentException("Could not find remote app.");
	}

	private boolean isLogOverwrite() {
		return configuration.getSwitch("log-overwrite", false);
	}

	private boolean isQuietStdout() {
		return isQuiet() || configuration.getSwitch("quiet-stdout", false);
	}

	private boolean isQuietStderr() {
		return isQuiet() || configuration.getSwitch("quiet-stderr", false);
	}

	private boolean isDaemon() {
		return configuration.getSwitch("daemon", false);
	}

	private String resolveWrapperClasspath() {
		String forkerClasspath = System.getProperty("java.class.path");
		String wrapperClasspath = configuration.getOptionValue("classpath", forkerClasspath);
		String jar = configuration.getOptionValue("jar", null);
		if (jar != null) {
			StringBuilder path = new StringBuilder(wrapperClasspath == null ? "" : wrapperClasspath);
			appendPath(path, jar);
			wrapperClasspath = path.toString();
		}
		return wrapperClasspath;
	}

	private String resolveWrapperModulepath() {
		String forkerModulepath = System.getProperty("java.module.path");
		String wrapperClasspath = configuration.getOptionValue("modulepath", forkerModulepath);
		return wrapperClasspath;
	}

	private boolean isNativeMain() {
		return configuration.getSwitch("native", false);
	}

	private boolean isUseDaemon() {
		boolean nativeMain = isNativeMain();
		return !nativeMain && !configuration.getSwitch("no-forker-daemon", nativeMain);
	}

	protected File resolveCwd() {
		String cwdpath = configuration.getOptionValue("cwd", null);
		File cwd = new File(System.getProperty("user.dir"));
		if (StringUtils.isNotBlank(cwdpath)) {
			cwd = relativize(cwd, cwdpath);
			if (!cwd.exists())
				throw new IllegalArgumentException(String.format("No such directory %s", cwd));
		}
		return cwd;
	}

	public ArgMode getArgMode() {
		return ArgMode.valueOf(configuration.getOptionValue("argmode", ArgMode.DEFAULT.name()));
	}

	public static String getAppName() {
		String an = System.getenv("FORKER_APPNAME");
		return an == null || an.length() == 0 ? ForkerWrapper.class.getName() : an;
	}

	public static void main(String[] args) {
		ForkerWrapper wrapper = new ForkerWrapper();
		wrapper.getWrappedApplication().setArguments(args);
		Options opts = new Options();
		// Add the options always available
		wrapper.addOptions(opts);
		wrapperMain(args, wrapper, opts);
	}

	public static void wrapperMain(String[] args, ForkerWrapper wrapper, Options opts) {
		// Add the command line launch options
		opts.addOption(Option.builder("c").argName("file").hasArg()
				.desc("A file to read configuration. This can either be a JavaScript file that evaluates to an object "
						+ "containing keys and values of the configuration options (use arrays for multiple value commands), or "
						+ "it may be a simple text file that contains name=value pairs, where name is the same name as used for command line "
						+ "arguments (see --help for a list of these)")
				.longOpt("configuration").build());
		opts.addOption(Option.builder("C").argName("directory").hasArg().desc(
				"A directory to read configuration files from. Each file can either be a JavaScript file that evaluates to an object "
						+ "containing keys and values of the configuration options (use arrays for multiple value commands), or "
						+ "it may be a simple text file that contains name=value pairs, where name is the same name as used for command line "
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
			String outFdStr = wrapper.getConfiguration().getOptionValue("fdout", null);
			if (outFdStr != null) {
				Constructor<FileDescriptor> cons = FileDescriptor.class.getDeclaredConstructor(int.class);
				cons.setAccessible(true);
				FileDescriptor fdOut = cons.newInstance(Integer.parseInt(outFdStr));
				System.setOut(new PrintStream(new FileOutputStream(fdOut), true));
			}
			String errFdStr = wrapper.getConfiguration().getOptionValue("fdout", null);
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
			String cfgDir = wrapper.getConfiguration().getOptionValue("configuration-directory", null);
			if (cfgDir != null) {
				File dir = new File(cfgDir);
				if (dir.exists()) {
					List<File> fl = new ArrayList<>(Arrays.asList(dir.listFiles()));
					Collections.sort(fl, (f1, f2) -> f1.getName().compareTo(f2.getName()));
					for (File f : fl) {
						if (f.isFile() && !f.isHidden())
							wrapper.readConfigFile(f);
					}
				}
			}
			int ret = wrapper.process(() -> {
				try {
					return wrapper.start();
				} catch (Throwable e) {
					e.printStackTrace();
					System.err.println(String.format("%s: %s\n", wrapper.getClass().getName(), e.getMessage()));
					formatter.printUsage(new PrintWriter(System.err, true), 80,
							String.format("%s  <application.class.name> [<argument> [<argument> ..]]", getAppName()));
					return 1;
				}
			});
			if (ret != Integer.MIN_VALUE)
				System.exit(ret);
		} catch (Throwable e) {
			System.err.println(String.format("%s: %s\n", wrapper.getClass().getName(), e.getMessage()));
			formatter.printUsage(new PrintWriter(System.err, true), 80,
					String.format("%s  <application.class.name> [<argument> [<argument> ..]]", getAppName()));
			System.exit(1);
		}

	}

	public Configuration getConfiguration() {
		return configuration;
	}

	public void init(CommandLine cmd) {
		if (!inited) {
			configuration.init(cmd);
			reconfigureLogging();
			inited = true;
		}
	}

	@Override
	public void setLogLevel(String lvl) {
		setLogLevel(Level.parse(lvl));
	}

	public void setLogLevel(Level lvl) {
		Logger logger = this.logger;
		do {
			logger.setLevel(lvl);
			for (Handler h : logger.getHandlers()) {
				h.setLevel(lvl);
			}
			logger = logger.getParent();
		} while (logger != null);
	}

	private void reconfigureLogging() {
		String levelName = configuration.getOptionValue("level", "WARNING");
		Level lvl = Level.parse(levelName);
		setLogLevel(lvl);
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

	protected String getJVMPath() {
		String javaExe = OS.getJavaPath();
		String altJava = configuration.getOptionValue("java", null);
		if (StringUtils.isNotBlank(altJava)) {
			if (SystemUtils.IS_OS_WINDOWS) {
				if (altJava.toLowerCase().endsWith(".exe")) {
					altJava = altJava.substring(0, altJava.length() - 4);
				}
				if (!altJava.toLowerCase().endsWith("w")) {
					altJava += "w";
				}
				altJava += ".exe";
			}
			javaExe = altJava;
		}
		String minjava = configuration.getOptionValue("min-java", null);
		String maxjava = configuration.getOptionValue("max-java", null);
		if (StringUtils.isNotBlank(minjava) || StringUtils.isNotBlank(maxjava)) {
			if (StringUtils.isBlank(minjava))
				minjava = "0.0.0";
			if (StringUtils.isBlank(maxjava))
				maxjava = "9999.9999.9999";
			Version minver = new Version(minjava);
			Version maxver = new Version(maxjava);
			try {
				JVM jvm = new JVM(javaExe);
				if (jvm.getVersion().compareTo(minver) < 0 || jvm.getVersion().compareTo(maxver) > 0) {
					logger.info(String.format(
							"Initially chosen JVM %s (%s) is not within the JVM version range of %s to %s", javaExe,
							jvm.getVersion(), minver, maxver));
					for (JVM altJvm : JVM.jvms()) {
						if (altJvm.getVersion().compareTo(minver) >= 0 && altJvm.getVersion().compareTo(maxver) <= 0) {
							javaExe = altJvm.getPath();
							logger.info(String.format("Using alternative JVM %s which version %s", javaExe,
									altJvm.getVersion()));
							break;
						}
					}
				} else {
					logger.info(String.format("Initially chosen JVM %s (%s) is valid for the version range %s to %s",
							javaExe, jvm.getVersion(), minver, maxver));
				}
			} catch (IOException ioe) {
				throw new IllegalArgumentException("Invalid JVM Path.", ioe);
			}
		}
		return javaExe;
	}

	protected void event(String name, String... args) throws IOException {
		String eventHandler = configuration.getOptionValue("on-" + name, null);
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
		options.addOption(new Option(null, "stop", false,
				"If single-instance mode is enabled, and the wrapped application includes the forker-wrapper module,"
						+ "then a stop command is sent. It is up to app whether or not to exit the runtime through the use of "
						+ "the 'ShutdownListener' registered on the 'Wrapped' instance. If it is happy to stop, it should do it's "
						+ "own clean up, then System.exit(). "));
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
		options.addOption(new Option("K", "monitor-configuration", false,
				"Monitor for configuration file changes. Some changes can be applied while the wrapped application is running, while "
						+ "some may cause the application to be restarted, and finally others may have no effect at all (until forker itself is restarted)."));
		options.addOption(new Option("k", "never-restart", false,
				"Prevent wrapper from restarting the process, regardless of the exit value from the spawned process. Totall overrides "
						+ "dont-restart-on and restart-on options."));
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
		options.addOption(
				new Option("z", "quiet-stderr", false, "Do not output anything on stderr from the wrapped process."));
		options.addOption(
				new Option("Z", "quiet-stdout", false, "Do not output anything on stdout from the wrapped process."));
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
		options.addOption(new Option("mp", "modulepath", true,
				"The modulepath to use to run the application. If not set, the current runtime default is used. Prefix the "
						+ "path with '+' to add it to the end of the existing modulepath, or '-' to add it to the start."));
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
		options.addOption(new Option("J", "jvmarg", true,
				"Additional VM argument. Specify multiple times for multiple arguments."));
		options.addOption(new Option("W", "cwd", true,
				"Change working directory, the wrapped process will be run from this location."));
		options.addOption(new Option("t", "timeout", true,
				"How long to wait since the last 'ping' from the launched application before "
						+ "considering the process as hung. Requires forker daemon is enabled."));
		options.addOption(new Option("f", "jar", true,
				"The path of a jar file to run. If this is specified, then this path will be added to the classpath, and META-INF/MANIFEST.MF examined for Main-Class for the"
						+ "main class to run. The first argument passed to the command becomes the first app argument."));
		options.addOption(new Option("m", "main", true,
				"The classname to run. If this is specified, then the first argument passed to the command "
						+ "becomes the first app argument. This may also be a module path (<module>/<class>), in which case the -m argument will be appended to the command as well."));
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
		options.addOption(new Option("i", "init-temp", true,
				"Initialise a named temporary folder before execution of application. The folder will be created if it does not exist, and emptied if it exists and has contents."));
		options.addOption(new Option("T", "to-temp", true,
				"Copy file(s) to the named temporary folder. Supports glob syntax for final part of the path."));
		options.addOption(new Option("G", "service-mode", true,
				"When enabled, 'start', 'stop', 'restart' and 'status' arguments can be passed which act in the same way as service control commands on Linux and similar operating systems."));
	}

	protected String buildPath(File cwd, String defaultClasspath, String classpath, boolean appendByDefault)
			throws IOException {
		logger.log(Level.INFO, "Building path ..");
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
				logger.log(Level.INFO, "    " + el);
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
									logger.log(Level.INFO, "        " + file.getAbsolutePath());
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
		 * Prepare to start a forker daemon. The client application may (if it wishes)
		 * include the forker-client module and use the daemon to execute administrator
		 * commands and perform other forker daemon operations.
		 */
		daemon = new Forker();
		daemon.setIsolated(true);
		/* Prepare command permissions if there are any */
		CommandHandler cmd = daemon.getHandler(CommandHandler.class);
		CheckCommandPermission permi = cmd.getExecutor(CheckCommandPermission.class);
		permi.setAllow(configuration.getOptionValues("allow-execute"));
		permi.setReject(configuration.getOptionValues("reject-execute"));
		cookie = daemon.prepare();
		event(STARTING_FORKER_DAEMON, cookie.getCookie(), String.valueOf(cookie.getPort()));
		new Thread() {
			@Override
			public void run() {
				try {
					daemon.start(cookie);
					event(STARTED_FORKER_DAEMON, cookie.getCookie(), String.valueOf(cookie.getPort()));
				} catch (IOException e) {
				}
			}
		}.start();
	}

	protected boolean daemonize(String javaExe, String forkerClasspath, String forkerModulepath, boolean daemonize,
			String pidfile) throws IOException {
		if (daemonize && configuration.getOptionValue("fallback-active", null) == null) {
			if ("true".equals(configuration.getOptionValue("native-fork", "false"))) {
				/*
				 * This doesn't yet work because of how JNA / Pty4J work with their native
				 * library extraction. The forked VM will not completely exit. It you use
				 * 'pstack' to show the native stack of the process, it will show that it is in
				 * a native call for a file that has been deleted (when the parent process
				 * exited). Both of these libraries by default will extract the native libraries
				 * to files, and mark them as to be deleted when JVM exit. Because once forked,
				 * the original JVM doesn't exit, these files are deleted, but they are needed
				 * by the forked process.
				 */
				logger.info("Running in background using native fork");
				int pid = CSystem.INSTANCE.fork();
				if (pid > 0) {
					if (pidfile != null) {
						FileUtils.writeLines(makeDirectoryForFile(relativize(resolveCwd(), pidfile)),
								Arrays.asList(String.valueOf(pid)));
					}
					return true;
				}
			} else {
				/*
				 * Fallback. Attempt to rebuild the command line. This will not be exact
				 */
				if (app.getOriginalArgs() == null)
					throw new IllegalStateException("Original arguments must be set.");
				ForkerBuilder fb = new ForkerBuilder(javaExe);
				if (StringUtils.isNotBlank(forkerModulepath)) {
					fb.command().add("-p");
					fb.command().add(forkerModulepath);
				}
				if (StringUtils.isNotBlank(forkerClasspath)) {
					fb.command().add("-classpath");
					fb.command().add(forkerClasspath);
				}
				for (String s : Arrays.asList("java.library.path", "jna.library.path")) {
					if (System.getProperty(s) != null)
						fb.command().add("-D" + s + "=" + System.getProperty(s));
				}
				fb.environment().put("FORKER_FALLBACK_ACTIVE", "true");
				// Currently needs to be quiet :(
				fb.environment().put("FORKER_QUIET", "true");
				// Doesnt seem to work
				// fb.environment().put("FORKER_FDOUT", "1");
				// fb.environment().put("FORKER_FDERR", "2");
				fb.command().add(ForkerWrapper.class.getName());
				// fb.command().add("--fdout=1");
				// fb.command().add("--fderr=2");
				fb.command().addAll(Arrays.asList(app.getOriginalArgs()));
				fb.background(true);
				fb.io(IO.OUTPUT);
				logger.info(String.format("Executing: %s", String.join(" ", fb.command())));
				fb.start();
				logger.info("Exiting initial runtime");
				return true;
			}
		} else {
			if (pidfile != null) {
				int pid = OS.getPID();
				logger.info(String.format("Writing PID %d", pid));
				FileUtils.writeLines(makeDirectoryForFile(relativize(resolveCwd(), pidfile)),
						Arrays.asList(String.valueOf(pid)));
			}
		}
		return false;
	}

	protected void addShutdownHook(final boolean useDaemon) {
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
					 * Close the client control connection. This will cause the wrapped process to
					 * System.exit(), and so cleanly shutdown
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
					if (p != null)
						p.destroy();
				}
				if (p != null) {
					final Thread current = Thread.currentThread();
					Thread exitWaitThread = null;
					final int exitWait = Integer.parseInt(configuration.getOptionValue("exit-wait", "10"));
					if (exitWait > 0) {
						exitWaitThread = new Thread() {
							{
								setDaemon(true);
								setName("ExitMonitor");
							}

							@Override
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
			}
		});
	}

	protected void monitorConfigurationFiles() {
		if (configuration.getSwitch("monitor-configuration", false) && fileMonThread == null) {
			fileMonThread = new Thread() {
				{
					setName("MonitorConfigurationFiles");
					setDaemon(true);
				}

				@Override
				public void run() {
					logger.info("Monitoring configuration files for changes");
					try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
						/*
						 * Find all of the common parent directories of all known configuration files *?
						 */
						Set<File> parents = new LinkedHashSet<>();
						for (File cfg : files) {
							parents.add(cfg.getCanonicalFile().getCanonicalFile());
						}
						/* Register all parents for watching */
						for (File parent : parents) {
							Path path = parent.toPath().getParent();
							path.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
						}
						while (true) {
							WatchKey key;
							try {
								key = watcher.poll(500, TimeUnit.MILLISECONDS);
							} catch (InterruptedException e) {
								return;
							}
							if (key == null) {
								Thread.yield();
								continue;
							}
							for (WatchEvent<?> event : key.pollEvents()) {
								WatchEvent.Kind<?> kind = event.kind();
								@SuppressWarnings("unchecked")
								WatchEvent<Path> ev = (WatchEvent<Path>) event;
								Path filename = ev.context();
								if (kind == StandardWatchEventKinds.OVERFLOW) {
									Thread.yield();
									continue;
								} else if (kind == java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
										&& files.contains(filename.toFile())) {
									configurationFileChanged(filename.toFile());
								}
								boolean valid = key.reset();
								if (!valid) {
									break;
								}
							}
							Thread.yield();
						}
					} catch (Throwable e) {
						// Log or rethrow the error
					}
				}
			};
			fileMonThread.start();
		} else if (!configuration.getSwitch("monitor-configuration", false) && fileMonThread != null) {
			fileMonThread.interrupt();
			fileMonThread = null;
			logger.info("Stoppe monitoring configuration files.");
		}
	}

	protected void monitorWrappedJMXApplication() {
		if (isSingleInstance() && Integer.parseInt(configuration.getOptionValue("timeout", "60")) > 0
				&& monitorThread == null) {
			monitorThread = new Thread() {
				{
					setName("ForkerWrapperMonitor");
					setDaemon(true);
				}

				@Override
				public void run() {
					logger.info("Monitoring pings from wrapped application");
					try {
						while (!stopping) {
							if (process != null && daemon != null) {
								WrapperHandler wrapper = daemon.getHandler(WrapperHandler.class);
								int timeout = Integer.parseInt(configuration.getOptionValue("timeout", "60"));
								if (wrapper.getLastPing() > 0
										&& (wrapper.getLastPing() + timeout * 1000) <= System.currentTimeMillis()) {
									logger.warning(String.format(
											"Process has not sent a ping in %d seconds, attempting to terminate",
											timeout));
									tempRestartOnExit = true;
									/*
									 * TODO may need to be more forceful than this, e.g. OS kill
									 */
									process.destroy();
								}
							}
							Thread.sleep(1000);
						}
					} catch (InterruptedException ie) {
					}
				}
			};
			monitorThread.start();
		} else if ((!isUseDaemon() || Integer.parseInt(configuration.getOptionValue("timeout", "60")) == 0)
				&& monitorThread != null) {
			monitorThread.interrupt();
			monitorThread = null;
			logger.info("Stopping forker monitor thread.");
		}
	}

	protected void monitorWrappedApplication() {
		if (isUseDaemon() && Integer.parseInt(configuration.getOptionValue("timeout", "60")) > 0
				&& monitorThread == null) {
			monitorThread = new Thread() {
				{
					setName("ForkerWrapperMonitor");
					setDaemon(true);
				}

				@Override
				public void run() {
					logger.info("Monitoring pings from wrapped application");
					try {
						while (!stopping) {
							if (process != null && daemon != null) {
								WrapperHandler wrapper = daemon.getHandler(WrapperHandler.class);
								int timeout = Integer.parseInt(configuration.getOptionValue("timeout", "60"));
								if (wrapper.getLastPing() > 0
										&& (wrapper.getLastPing() + timeout * 1000) <= System.currentTimeMillis()) {
									logger.warning(String.format(
											"Process has not sent a ping in %d seconds, attempting to terminate",
											timeout));
									tempRestartOnExit = true;
									/*
									 * TODO may need to be more forceful than this, e.g. OS kill
									 */
									process.destroy();
								}
							}
							Thread.sleep(1000);
						}
					} catch (InterruptedException ie) {
					}
				}
			};
			monitorThread.start();
		} else if ((!isUseDaemon() || Integer.parseInt(configuration.getOptionValue("timeout", "60")) == 0)
				&& monitorThread != null) {
			monitorThread.interrupt();
			monitorThread = null;
			logger.info("Stopping forker monitor thread.");
		}
	}

	protected void appendPath(StringBuilder newClasspath, String el) {
		if (newClasspath.length() > 0)
			newClasspath.append(File.pathSeparator);
		newClasspath.append(el);
	}

	public void readConfigFile(File file) throws IOException {
		readConfigFile(file, configuration.getProperties());
	}

	protected void readConfigFile(File file, List<KeyValuePair> properties) throws IOException {
		synchronized (files) {
			if (files.contains(file)) {
				logger.info(String.format("Re-loading configuration file %s", file));
			} else {
				logger.info(String.format("Loading configuration file %s", file));
				files.add(file);
			}

			//
			// TODO restart app and/or adjust other configuration on reload
			// TODO it shouldnt reload one at a time, it should wait a short while for
			// all changes, then reload all configuration files in the same order
			// 'properties' should
			// be cleared before all are reloaded.

			if (file.getName().endsWith(".js")) {
				if (engine == null) {
					ScriptEngineManager engineManager = new ScriptEngineManager();
					engine = engineManager.getEngineByName("nashorn");
					Bindings bindings = engine.createBindings();
					bindings.put("wrapper", this);
					bindings.put("log", logger);
					if (engine == null)
						throw new IOException("Cannot find JavaScript engine. Are you on at least Java 8?");
				}
				FileReader r = new FileReader(file);
				try {
					@SuppressWarnings("unchecked")
					Map<String, Object> o = (Map<String, Object>) engine.eval(r);
					for (Map.Entry<String, Object> en : o.entrySet()) {
						if (en.getValue() instanceof Map) {
							@SuppressWarnings("unchecked")
							Map<String, Object> m = (Map<String, Object>) en.getValue();
							for (Map.Entry<String, Object> men : m.entrySet()) {
								properties.add(new KeyValuePair(en.getKey(),
										men.getValue() == null ? null : String.valueOf(men.getValue())));
							}
						} else
							properties.add(new KeyValuePair(en.getKey(),
									en.getValue() == null ? null : String.valueOf(en.getValue())));
					}
				} catch (ScriptException e) {
					throw new IOException("Failed to evaluate configuration script.", e);
				}
			}
			BufferedReader fin = new BufferedReader(new FileReader(file));
			try {
				String line = null;
				while ((line = fin.readLine()) != null) {
					if (!line.trim().startsWith("#") && !line.trim().equals("")) {
						properties.add(new KeyValuePair(Replace.replaceSystemProperties(line)));
					}
				}
			} finally {
				fin.close();
			}
			app.set(configuration.getOptionValue("main", null), configuration.getOptionValue("jar", null));
			reconfigureLogging();
		}
	}

	protected boolean onBeforeProcess(Callable<Void> task) {
		return true;
	}

	private File makeDirectoryForFile(File file) throws IOException {
		File dir = file.getParentFile();
		if (dir != null && !dir.exists() && !dir.mkdirs())
			throw new IOException(String.format("Failed to create directory %s", dir));
		return file;
	}

	private byte[] newBuffer() {
		return new byte[Integer.parseInt(configuration.getOptionValue("buffer-size", "1024"))];
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

	private void initTempFolder(String tempFolder, File cwd) throws IOException {

		File tempFile = new File(tempFolder);
		if (!tempFile.isAbsolute()) {
			tempFile = new File(cwd, tempFolder);
		}
		delTree(tempFile);

		List<String> paths = configuration.getOptionValues("to-temp");
		if (!paths.isEmpty()) {
			for (String path : paths) {
				String parentPath = FilenameUtils.getPath(path);
				String pattern = FilenameUtils.getName(path);
				PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
				File parentFile = new File(parentPath);
				if (!parentFile.isAbsolute()) {
					parentFile = new File(cwd, parentPath);
				}
				File[] children = parentFile.listFiles();
				if (children != null) {
					for (File child : children) {
						if (matcher.matches(child.toPath().getFileName())) {
							FileUtils.copyFile(child, new File(tempFile, child.getName()));
						}
					}
				}
			}
		}
	}

	private void delTree(File file) {

		File[] children = file.listFiles();
		if (children != null) {
			for (File child : children) {
				if (child.isDirectory()) {
					delTree(child);
					child.delete();
				} else {
					child.delete();
				}
			}
		}
	}

	protected int process(Callable<Integer> task) throws Exception {
		if (onBeforeProcess(() -> {
			continueProcessing();
			System.exit(task.call());
			return null;
		})) {
			continueProcessing();
			return task.call();
		} else {
			return Integer.MIN_VALUE;
		}
	}

	protected void continueProcessing() throws ParseException, IOException {
		app.set(configuration.getOptionValue("main", null), configuration.getOptionValue("jar", null),
				configuration.getCmd().getArgList(), configuration.getOptionValues("apparg"), getArgMode());
	}

	private void configurationFileChanged(File file) {
		synchronized (configuration.getCfgLock()) {
			if (configChange == null) {
				configChange = Executors.newSingleThreadScheduledExecutor();
			}
			if (changeTask != null) {
				changeTask.cancel(false);
			}
			changeTask = configChange.schedule(() -> {
				synchronized (configuration.getCfgLock()) {

					String wasClassname = app.getClassname();
					String wasModule = app.getModule();
					File wasCwd = resolveCwd();
					String wasJavaExe = getJVMPath();
					String wasBootClasspath = configuration.getOptionValue("boot-classpath", null);
					boolean wasNativeMain = isNativeMain();
					boolean wasUseDaemon = isUseDaemon();
					boolean wasDaemon = isDaemon();
					String wasPidfile = configuration.getOptionValue("pidfile", null);
					List<String> wasJvmArgs = configuration.getOptionValues("jvmarg");
					boolean wasSingleInstance = isSingleInstance();
					boolean wasQuietStdErr = isQuietStderr();
					boolean wasQuietStdOut = isQuietStdout();
					boolean wasLogoverwrite = isLogOverwrite();
					String wasInitTemp = configuration.getOptionValue("init-temp", null);
					boolean wasNoForkerClasspath = isNoForkerClasspath();

					List<KeyValuePair> kvp = new ArrayList<>();
					kvp.addAll(configuration.getProperties());
					for (File f : new ArrayList<>(files)) {
						try {
							readConfigFile(f, kvp);
						} catch (IOException ioe) {
							logger.log(Level.WARNING, String.format("Failed to re-load configuration file %s", file),
									ioe);
						}
					}

					configuration.getProperties().clear();
					configuration.getProperties().addAll(kvp);

					/* Decide if to restart, or to try to reconfigure while running */
					boolean restart = false;
					boolean fullRestart = false;
					if (!Objects.equals(wasUseDaemon, isUseDaemon()) || !Objects.equals(wasDaemon, isDaemon())
							|| !Objects.equals(wasPidfile, configuration.getOptionValue("pidfile", null))) {
						logger.warning(
								"Changing daemon mode or pidfile requires restart of forker JVM for full effect.");
						fullRestart = restart = true;
					} else if (!Objects.equals(wasClassname, app.getClassname())
							|| !Objects.equals(wasModule, app.getModule()) || !Objects.equals(wasCwd, resolveCwd())
							|| !Objects.equals(wasJavaExe, getJVMPath())
							|| !Objects.equals(wasBootClasspath, configuration.getOptionValue("boot-classpath", null))
							|| !Objects.equals(wasNativeMain, isNativeMain())
							|| !Objects.equals(wasJvmArgs, configuration.getOptionValues("jvmarg"))
							|| !Objects.equals(wasSingleInstance, isSingleInstance())) {
						fullRestart = restart = true;
					} else if (!Objects.equals(wasQuietStdOut, isQuietStderr())
							|| !Objects.equals(wasQuietStdErr, isQuietStdout())
							|| !Objects.equals(wasLogoverwrite, isLogOverwrite())
							|| !Objects.equals(wasNoForkerClasspath, isNoForkerClasspath())
							|| !Objects.equals(wasInitTemp, configuration.getOptionValue("init-temp", null))) {
						restart = true;
					}

					if (fullRestart) {
						logger.info(String.format("The configuration change will cause a full restart of forker."));
						try {
							stop();
						} catch (InterruptedException e) {
							logger.fine("Restart interrupted.");
						}
						try {
							start();
						} catch (IOException e) {
						} catch (InterruptedException e) {
						}
					} else if (restart) {
						logger.info(String.format("The configuration change will cause a restart of the application."));
						try {
							restart();
						} catch (InterruptedException e) {
							logger.fine("Restart interrupted.");
						}
					} else {
						logger.info(String.format("The configuration change will adjust the running service."));
						monitorWrappedApplication();
						monitorConfigurationFiles();
					}
				}
			}, 1, TimeUnit.SECONDS);
		}

	}

	private boolean isNoForkerClasspath() {
		return configuration.getSwitch("no-forker-classpath", false);
	}

	private boolean isQuiet() {
		return configuration.getSwitch("quiet", false);
	}

	private boolean isSingleInstance() {
		return configuration.getSwitch("single-instance", false);
	}

	protected Boolean onMaybeRestart(int retval, int lastRetVal) throws Exception {
		return null;
	}

	private int maybeRestart(int retval, int lastRetVal) throws IOException, InterruptedException {

		List<String> restartValues = Arrays.asList(configuration.getOptionValue("restart-on", "").split(","));
		List<String> dontRestartValues = new ArrayList<String>(
				Arrays.asList(configuration.getOptionValue("dont-restart-on", "0,1,2").split(",")));
		dontRestartValues.removeAll(restartValues);
		String strret = String.valueOf(retval);
		event(APPPLICATION_STOPPED, strret, app.fullClassAndModule());

		boolean restart = false;
		Boolean hookRestart = null;
		try {
			hookRestart = onMaybeRestart(retval, lastRetVal);
		} catch (Exception e) {
			logger.log(Level.WARNING, "Failed to check for restart. Assuming not.", e);
		}
		if (hookRestart != null)
			restart = hookRestart;
		else
			restart = !configuration.getSwitch("never-restart", false) && !preventRestart
					&& (((restartValues.size() == 1 && restartValues.get(0).equals("")) || restartValues.size() == 0
							|| restartValues.contains(strret)) && !dontRestartValues.contains(strret));

		if (tempRestartOnExit || restart) {
			try {
				tempRestartOnExit = false;
				int waitSec = Integer.parseInt(configuration.getOptionValue("restart-wait", "0"));
				if (waitSec == 0)
					throw new NumberFormatException();
				event(RESTARTING_APPLICATION, app.fullClassAndModule(), String.valueOf(waitSec));
				logger.warning(
						String.format("Process exited with %d, attempting restart in %d seconds", retval, waitSec));
				lastRetVal = retval;
				Thread.sleep(waitSec * 1000);
			} catch (NumberFormatException nfe) {
				event(RESTARTING_APPLICATION, app.fullClassAndModule(), "0");
				logger.warning(String.format("Process exited with %d, attempting restart", retval));
			}
		} else
			return Integer.MIN_VALUE;
		return lastRetVal;
	}

	private int captureStreams(File cwd, final boolean useDaemon, boolean daemonize, boolean quietStdErr,
			boolean quietStdOut, boolean logoverwrite)
			throws IOException, UnsupportedEncodingException, InterruptedException {
		int retval;
		if (useDaemon) {
			process.getOutputStream().write(cookie.toString().getBytes("UTF-8"));
			process.getOutputStream().write("\r\n".getBytes("UTF-8"));
			process.getOutputStream().flush();
		}
		String logpath = configuration.getOptionValue("log", null);
		String errpath = configuration.getOptionValue("errors", null);
		if (errpath == null)
			errpath = logpath;
		OutputStream outlog = null;
		OutputStream errlog = null;
		long logDelay = Long.parseLong(configuration.getOptionValue("log-write-delay", "50"));
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
		return retval;
	}

	private ForkerBuilder buildCommand(String javaExe, String forkerClasspath, String forkerModulepath,
			String wrapperClasspath, String wrapperModulePath, String bootClasspath, final boolean nativeMain,
			final boolean useDaemon, int times, int lastRetVal) throws IOException {

		ForkerBuilder appBuilder = new ForkerBuilder();
		String modulepath = null;
		File cwd = resolveCwd();
		boolean isUsingWrappedOnClasspath = false;
		boolean isUsingWrappedOnModulepath = false;
		if (!nativeMain) {
			/* This is launching a Java class, so construct the classpath */
			appBuilder.command().add(javaExe);
			logger.log(Level.INFO, "Building classpath");
			String classpath = buildPath(cwd, isNoForkerClasspath() ? null : forkerClasspath, wrapperClasspath, true);
			if (classpath != null && !classpath.equals("")) {
				appBuilder.command().add("-classpath");
				appBuilder.command().add(classpath);
				isUsingWrappedOnClasspath = isUsingWrapped(classpath);
			}

			logger.log(Level.INFO, "Building modulepath");
			modulepath = buildPath(cwd, isNoForkerClasspath() ? null : forkerModulepath, wrapperModulePath, true);
			if (modulepath != null && !modulepath.equals("")) {
				appBuilder.command().add("-p");
				appBuilder.command().add(modulepath);
				isUsingWrappedOnModulepath = isUsingWrapped(modulepath);
			}

			boolean hasBootCp = false;
			for (String val : configuration.getOptionValues("jvmarg")) {
				if (val.startsWith("-Xbootclasspath"))
					hasBootCp = true;
				appBuilder.command().add(val);
			}
			if (!hasBootCp) {
				String bootcp = buildPath(cwd, null, bootClasspath, false);
				if (bootcp != null && !bootcp.equals("")) {
					/*
					 * Do our own processing of append/prepend as there are special JVM arguments
					 * for it
					 */
					if (bootClasspath != null && bootClasspath.startsWith("+"))
						appBuilder.command().add("-Xbootclasspath/a:" + bootcp);
					else if (bootClasspath != null && bootClasspath.startsWith("-"))
						appBuilder.command().add("-Xbootclasspath/p:" + bootcp);
					else
						appBuilder.command().add("-Xbootclasspath:" + bootcp);
				}
			}

			if (StringUtils.isBlank(app.getClassname()))
				throw new IllegalArgumentException(
						"Must provide a 'main' property to specify the class that contains the main() method that is your applications entry point.");
		}

		/*
		 * Pass information to the launched application about the last exit status and
		 * number of attempts
		 */
		if (!configuration.getSwitch("no-info", false)) {
			if (nativeMain) {
				appBuilder.environment().put("FORKER_INFO_LAST_EXIT_CODE", String.valueOf(lastRetVal));
				appBuilder.environment().put("FORKER_INFO_ATTEMPTS", String.valueOf(times));
			} else {
				if (lastRetVal > -1) {
					appBuilder.command().add(String.format("-Dforker.info.lastExitCode=%d", lastRetVal));
				}
				appBuilder.command().add(String.format("-Dforker.info.attempts=%d", times));
			}
		}

		/*
		 * If the daemon should be used, we assume that forker-client is on the
		 * classpath and execute the application via that, passing the forker daemon
		 * cookie via stdin. *
		 */
		if (useDaemon) {
			if (isUsingWrappedOnModulepath) {
				if (modulepath != null && isUsingClient(modulepath)) {
					appBuilder.command().add("--add-modules");
					appBuilder.command().add(com.sshtools.forker.client.Forker.class.getPackageName());
				}
				appBuilder.command().add(WRAPPED_MODULE_NAME + "/" + WRAPPED_CLASS_NAME);
				appBuilder.command().add(com.sshtools.forker.client.Forker.class.getName());

			} else if (isUsingWrappedOnClasspath) {
				appBuilder.command().add(WRAPPED_CLASS_NAME);
				appBuilder.command().add(com.sshtools.forker.client.Forker.class.getName());
			} else {
				if (modulepath != null && isUsingClient(modulepath)) {
					appBuilder.command().add("-m");
					appBuilder.command().add(com.sshtools.forker.client.Forker.class.getPackageName() + "/"
							+ com.sshtools.forker.client.Forker.class.getName());
				} else
					appBuilder.command().add(com.sshtools.forker.client.Forker.class.getName());
			}
			appBuilder.command().add(String.valueOf(OS.isAdministrator()));
			appBuilder.command().add(app.getClassname());
			if (app.hasArguments())
				appBuilder.command().addAll(Arrays.asList(app.getArguments()));
		} else {
			/*
			 * Otherwise we are just running the application directly or via Wrapped
			 */
			if (modulepath != null && StringUtils.isNotBlank(app.getModule())) {
				appBuilder.command().add("--add-modules");
				appBuilder.command().add(app.getModule());
			}

			if (isUsingWrappedOnModulepath) {
				appBuilder.command().add("-m");
				appBuilder.command().add(WRAPPED_MODULE_NAME + "/" + WRAPPED_CLASS_NAME);
				appBuilder.command().add(app.getClassname());
			} else if (isUsingWrappedOnClasspath) {
				appBuilder.command().add(WRAPPED_CLASS_NAME);
				appBuilder.command().add(app.getClassname());
			} else {
				if (StringUtils.isNotBlank(app.getModule())) {
					appBuilder.command().add("-m");
					appBuilder.command().add(app.fullClassAndModule());
				} else
					appBuilder.command().add(app.getClassname());
			}
			if (app.hasArguments())
				appBuilder.command().addAll(Arrays.asList(app.getArguments()));
		}

		/* Process priority */
		String priStr = configuration.getOptionValue("priority", null);
		if (priStr != null) {
			appBuilder.priority(Priority.valueOf(priStr));
		}

		/* Directory and IO */
		appBuilder.io(IO.DEFAULT);
		appBuilder.directory(cwd);

		/* Environment variables */

		for (String env : configuration.getOptionValues("setenv")) {
			String key = env;
			String value = "";
			int idx = env.indexOf('=');
			if (idx != -1) {
				key = env.substring(0, idx);
				value = env.substring(idx + 1);
			}
			appBuilder.environment().put(key, value);
		}

		/* Affinity with CPU cores */
		List<String> cpus = configuration.getOptionValues("cpu");
		for (String cpu : cpus) {
			appBuilder.affinity().add(Integer.parseInt(cpu));
		}

		/* Run as as administrator or a specific user */
		if (configuration.getSwitch("administrator", false)) {
			if (!OS.isAdministrator()) {
				logger.info("Raising privileges to administartor");
				appBuilder.effectiveUser(EffectiveUserFactory.getDefault().administrator());
			}
		} else {
			String runas = configuration.getOptionValue("run-as", null);
			if (runas != null && !runas.equals(System.getProperty("user.name"))) {
				logger.info(String.format("Switching user to %s", runas));
				appBuilder.effectiveUser(EffectiveUserFactory.getDefault().getUserForUsername(runas));
			}
		}
		return appBuilder;
	}

	private boolean isUsingClient(String path) {
		if (path == null)
			return false;
		for (String p : path.split(File.pathSeparator)) {
			if (p.matches(".*forker-client.*\\.jar"))
				return true;
		}
		return false;
	}

	private boolean isUsingWrapped(String path) {
		if (path == null)
			return false;
		for (String p : path.split(File.pathSeparator)) {
			if (p.matches(".*forker-wrapped.*\\.jar"))
				return true;
		}
		return false;
	}
}
