package com.sshtools.forker.wrapper;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.ServiceLoader;
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
import com.sshtools.forker.common.Util.TeeOutputStream;
import com.sshtools.forker.daemon.CommandHandler;
import com.sshtools.forker.daemon.Forker;
import com.sshtools.forker.daemon.Forker.Client;
import com.sshtools.forker.wrapper.JVM.Version;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.PositionalParamSpec;
import picocli.CommandLine.ParseResult;

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
 * <li>Easy configuration of remote debugging</li>
 * <li>.. and more</li>
 * </ul>
 *
 */
@MXBean
public class ForkerWrapper implements ForkerWrapperMXBean {

	/** The Constant CONTINUE_UPGRADE. */
	public final static int CONTINUE_UPGRADE = Integer.MIN_VALUE;
	
	/** The Constant USER_INTERACTION. */
	public final static int USER_INTERACTION = Integer.MIN_VALUE + 2;
	
	/** The Constant EXIT_AFTER_UPGRADE. */
	public final static int EXIT_AFTER_UPGRADE = Integer.MIN_VALUE + 1;
	
	/** The Constant EXIT_OK. */
	public final static int EXIT_OK = 0;
	
	/** The Constant EXIT_ERROR. */
	public final static int EXIT_ERROR = 1;
	
	/** The Constant EXIT_ARGUMENT_SYNTAX. */
	public final static int EXIT_ARGUMENT_SYNTAX = 2;

	/** The Constant WRAPPED_MODULE_NAME. */
	public final static String WRAPPED_MODULE_NAME = "com.sshtools.forker.wrapped";
	
	/** The Constant WRAPPED_CLASS_NAME. */
	public final static String WRAPPED_CLASS_NAME = "com.sshtools.forker.wrapped.Wrapped";
	
	/** The Constant WRAPPED_MX_BEAN_NAME. */
	public final static String WRAPPED_MX_BEAN_NAME = "WrappedMXBean";
	
	/** The Constant EXITED_WRAPPER. */
	public final static String EXITED_WRAPPER = "exited-wrapper";
	
	/** The Constant EXITING_WRAPPER. */
	public final static String EXITING_WRAPPER = "exiting-wrapper";
	
	/** The Constant STARTING_FORKER_DAEMON. */
	public final static String STARTING_FORKER_DAEMON = "starting-forker-daemon";
	
	/** The Constant STARTED_FORKER_DAEMON. */
	public final static String STARTED_FORKER_DAEMON = "started-forker-daemon";
	
	/** The Constant STARTING_APPLICATION. */
	public final static String STARTING_APPLICATION = "starting-application";
	
	/** The Constant STARTED_APPLICATION. */
	public final static String STARTED_APPLICATION = "started-application";
	
	/** The Constant RESTARTING_APPLICATION. */
	public final static String RESTARTING_APPLICATION = "restarting-application";
	
	/** The Constant APPPLICATION_STOPPED. */
	public final static String APPPLICATION_STOPPED = "application-stopped";
	
	/** The Constant EVENT_NAMES. */
	public final static String[] EVENT_NAMES = { EXITED_WRAPPER, EXITING_WRAPPER, STARTING_FORKER_DAEMON,
			STARTED_FORKER_DAEMON, STARTED_APPLICATION, STARTING_APPLICATION, RESTARTING_APPLICATION,
			APPPLICATION_STOPPED };
	
	/** The Constant CROSSPLATFORM_PATH_SEPARATOR. */
	private static final String CROSSPLATFORM_PATH_SEPARATOR = ";|:";

	/** The app. */
	private WrappedApplication app = new WrappedApplication();
	
	/** The configuration. */
	private Configuration configuration = new Configuration();
	
	/** The daemon. */
	private Forker daemon;
	
	/** The cookie. */
	private Instance cookie;
	
	/** The process. */
	private Process process;
	
	/** The temp restart on exit. */
	private boolean tempRestartOnExit;
	
	/** The inited. */
	private boolean inited;
	
	/** The default out. */
	private PrintStream defaultOut = System.out;
	
	/** The default err. */
	private PrintStream defaultErr = System.err;
	
	/** The default in. */
	private InputStream defaultIn = System.in;
	
	/** The stopping. */
	private boolean stopping = false;
	
	/** The prevent restart. */
	private boolean preventRestart = false;
	
	/** The files. */
	private Set<File> files = new LinkedHashSet<>();
	
	/** The config change. */
	private ScheduledExecutorService configChange;
	
	/** The change task. */
	private ScheduledFuture<?> changeTask;
	
	/** The monitor thread. */
	private Thread monitorThread;
	
	/** The file mon thread. */
	private Thread fileMonThread;
	
	/** The plugins. */
	private List<WrapperPlugin> plugins = new ArrayList<>();
	
	/** The system properties. */
	private Properties systemProperties = new Properties();

	/** The logger. */
	protected Logger logger = Logger.getGlobal();
	
	/** The using wrapped. */
	private boolean usingWrapped;
	
	/** The jmx connection to wrapped. */
	private MBeanServerConnection jmxConnectionToWrapped;
	
	/** The jmx object name. */
	private ObjectName jmxObjectName;

	{
		reconfigureLogging();
		for (WrapperPlugin p : ServiceLoader.load(WrapperPlugin.class)) {
			plugins.add(p);
		}
	}

	/**
	 * Ping.
	 */
	@Override
	public void ping() {
		if (logger.isLoggable(Level.FINE))
			logger.log(Level.FINE, "Ping from JMX client.");
	}

	/**
	 * Gets the system properties.
	 *
	 * @return the system properties
	 */
	public Properties getSystemProperties() {
		return systemProperties;
	}

	/**
	 * Gets the cmd.
	 *
	 * @return the cmd
	 */
	public ParseResult getCmd() {
		return configuration.getCmd();
	}

	/**
	 * Gets the wrapped application.
	 *
	 * @return the wrapped application
	 */
	public WrappedApplication getWrappedApplication() {
		return app;
	}

	/**
	 * Gets the default in.
	 *
	 * @return the default in
	 */
	public InputStream getDefaultIn() {
		return defaultIn;
	}

	/**
	 * Sets the default in.
	 *
	 * @param defaultIn the new default in
	 */
	public void setDefaultIn(InputStream defaultIn) {
		this.defaultIn = defaultIn;
	}

	/**
	 * Gets the default out.
	 *
	 * @return the default out
	 */
	public PrintStream getDefaultOut() {
		return defaultOut;
	}

	/**
	 * Sets the default out.
	 *
	 * @param defaultOut the new default out
	 */
	public void setDefaultOut(PrintStream defaultOut) {
		this.defaultOut = defaultOut;
	}

	/**
	 * Gets the default err.
	 *
	 * @return the default err
	 */
	public PrintStream getDefaultErr() {
		return defaultErr;
	}

	/**
	 * Sets the default err.
	 *
	 * @param defaultErr the new default err
	 */
	public void setDefaultErr(PrintStream defaultErr) {
		this.defaultErr = defaultErr;
	}

	/**
	 * Gets the arguments.
	 *
	 * @return the arguments
	 */
	@Override
	public String[] getArguments() {
		return app.getArguments();
	}

	/**
	 * Gets the classname.
	 *
	 * @return the classname
	 */
	@Override
	public String getClassname() {
		return app.getClassname();
	}

	/**
	 * Gets the module.
	 *
	 * @return the module
	 */
	@Override
	public String getModule() {
		return app.getModule();
	}

	/**
	 * Relativize.
	 *
	 * @param context the context
	 * @param path the path
	 * @return the file
	 */
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

	/**
	 * Restart.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Override
	public void restart() throws InterruptedException {
		restart(true);
	}

	/**
	 * Restart.
	 *
	 * @param wait the wait
	 * @throws InterruptedException the interrupted exception
	 */
	@Override
	public void restart(boolean wait) throws InterruptedException {
		stop(true, true);
	}

	/**
	 * Stop.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Override
	public void stop() throws InterruptedException {
		stop(true);
	}

	/**
	 * Stop.
	 *
	 * @param wait the wait
	 * @throws InterruptedException the interrupted exception
	 */
	@Override
	public void stop(boolean wait) throws InterruptedException {
		stop(wait, false);
	}

	/**
	 * Start.
	 *
	 * @return the int
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws InterruptedException the interrupted exception
	 */
	@SuppressWarnings("resource")
	public int start() throws IOException, InterruptedException {
		if (!inited)
			init(null);

		for (WrapperPlugin plugin : plugins) {
			plugin.start();
		}

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
		List<String> systemProperties = configuration.getOptionValues("system");
		if (nativeMain && StringUtils.isNotBlank(configuration.getOptionValue("classpath", null))) {
			throw new IOException("Native main may not be used with classpath option.");
		}
		if (nativeMain && StringUtils.isNotBlank(configuration.getOptionValue("modulepath", null))) {
			throw new IOException("Native main may not be used with modulepath option.");
		}
		if (nativeMain && !jvmArgs.isEmpty()) {
			throw new IOException("Native main may not be used with jvmarg option.");
		}
		if (nativeMain && !systemProperties.isEmpty()) {
			throw new IOException("Native main may not be used with system option.");
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
		File lockFile = getLockFile();
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
						return shutdownWrapped();
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

				for (WrapperPlugin plugin : plugins) {
					plugin.beforeLaunch();
				}

				if (isNoFork()) {
					retval = noFork(daemonize, wrapperClasspath, forkerClasspath, times, lastRetVal);
				} else {
					retval = forked(javaExe, wrapperClasspath, wrapperModulepath, forkerClasspath, forkerModulepath,
							bootClasspath, nativeMain, useDaemon, daemonize, times, lastRetVal, quietStdErr,
							quietStdOut, logoverwrite);

				}

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

	/**
	 * Shutdown wrapped.
	 *
	 * @return the int
	 */
	protected int shutdownWrapped() {
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
	}

	/**
	 * Forked.
	 *
	 * @param javaExe the java exe
	 * @param wrapperClasspath the wrapper classpath
	 * @param wrapperModulepath the wrapper modulepath
	 * @param forkerClasspath the forker classpath
	 * @param forkerModulepath the forker modulepath
	 * @param bootClasspath the boot classpath
	 * @param nativeMain the native main
	 * @param useDaemon the use daemon
	 * @param daemonize the daemonize
	 * @param times the times
	 * @param lastRetVal the last ret val
	 * @param quietStdErr the quiet std err
	 * @param quietStdOut the quiet std out
	 * @param logoverwrite the logoverwrite
	 * @return the int
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws UnsupportedEncodingException the unsupported encoding exception
	 * @throws InterruptedException the interrupted exception
	 */
	protected int forked(String javaExe, String wrapperClasspath, String wrapperModulepath, String forkerClasspath,
			String forkerModulepath, String bootClasspath, final boolean nativeMain, final boolean useDaemon,
			boolean daemonize, int times, int lastRetVal, boolean quietStdErr, boolean quietStdOut,
			boolean logoverwrite) throws IOException, UnsupportedEncodingException, InterruptedException {
		int retval;
		/* Build the command to launch the application itself */
		ForkerBuilder appBuilder = buildCommand(javaExe, forkerClasspath, forkerModulepath, wrapperClasspath,
				wrapperModulepath, bootClasspath, nativeMain, useDaemon, times, lastRetVal);
		for (WrapperPlugin plugin : plugins) {
			if (plugin.buildCommand(appBuilder))
				break;
		}

		daemon = null;
		cookie = null;
		if (useDaemon) {
			startForkerDaemon();
		}

		monitorConfigurationFiles();
		monitorWrappedApplication();
		monitorWrappedJMXApplication();

		event(STARTING_APPLICATION, String.valueOf(times), resolveCwd().getAbsolutePath(), app.fullClassAndModule(),
				String.valueOf(lastRetVal));
		logger.info(
				String.format("Executing in %s: %s", appBuilder.directory(), String.join(" ", appBuilder.command())));
		process = appBuilder.start();
		event(STARTED_APPLICATION, app.fullClassAndModule());

		/* The process is now started, capture the streams and log or sink them */
		retval = captureStreams(resolveCwd(), useDaemon, daemonize, quietStdErr, quietStdOut, logoverwrite);
		return retval;
	}

	/**
	 * No fork.
	 *
	 * @param useDaemon the use daemon
	 * @param wrapperClasspath the wrapper classpath
	 * @param forkerClasspath the forker classpath
	 * @param times the times
	 * @param lastRetVal the last ret val
	 * @return the int
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	protected int noFork(boolean useDaemon, String wrapperClasspath, String forkerClasspath, int times, int lastRetVal)
			throws IOException {
		if (StringUtils.isNotBlank(app.getModule()))
			throw new IOException("no-fork does not currently work with modules.");

//		String modulepath = null;
		File cwd = resolveCwd();
		boolean isUsingWrappedOnClasspath = false;
//		boolean isUsingWrappedOnModulepath = false;

		List<URL> urls = new ArrayList<>();

		/* This is launching a Java class, so construct the classpath */
		logger.log(Level.INFO, "Building classpath");
		String classpath = buildPath(cwd, isNoForkerClasspath() ? null : forkerClasspath, wrapperClasspath, true);
		if (classpath != null && !classpath.equals("")) {
			for (String el : classpath.split(File.pathSeparator)) {
				URL url = new File(el).toURI().toURL();
				logger.log(Level.INFO, "   " + url);
				urls.add(url);
			}
			isUsingWrappedOnClasspath = isUsingWrapped(classpath);
		}

		// TODO how?
//			logger.log(Level.INFO, "Building modulepath");
//			modulepath = buildPath(cwd, isNoForkerClasspath() ? null : forkerModulepath, wrapperModulePath, true);
//			if (modulepath != null && !modulepath.equals("")) {
//				command.add("-p");
//				command.add(modulepath);
//				isUsingWrappedOnModulepath = isUsingWrapped(modulepath);
//			}

		/* Can't isolate these, so just set them */
		for (Object key : systemProperties.keySet()) {
			System.setProperty((String) key, systemProperties.getProperty((String) key));
		}
		for (String jvmArg : configuration.getOptionValues("jvmarg")) {
			if (jvmArg.startsWith("-D")) {
				String[] nv = nameValue(jvmArg.substring(2));
				System.setProperty(nv[0], nv[1]);
			}
		}
		for (String jvmArg : configuration.getOptionValues("system")) {
			String[] nv = nameValue(jvmArg);
			System.setProperty(nv[0], nv[1]);
		}

		if (configuration.getSwitch("debug", false)) {
			throw new IOException("Remote debug helper may not be used with no-fork");
		}

		if (StringUtils.isBlank(app.getClassname()))
			throw new IllegalArgumentException(
					"Must provide a 'main' property to specify the class that contains the main() method that is your applications entry point.");

		/*
		 * If the daemon should be used, we assume that forker-client is on the
		 * classpath and execute the application via that, passing the forker daemon
		 * cookie via stdin. *
		 */
		String classname = null;
		List<String> tail = new ArrayList<>();
		List<String> headArgs = new ArrayList<>();
		if (useDaemon) {
			// TODO how? ModuleLayer may be an answer
//			if (isUsingWrappedOnModulepath) {
//				if (modulepath != null && isUsingClient(modulepath)) {
//					command.add("--add-modules");
//					command.add(com.sshtools.forker.client.Forker.class.getPackageName());
//				}
//				headArgs.add(WRAPPED_MODULE_NAME + "/" + WRAPPED_CLASS_NAME);
//				headArgs.add(com.sshtools.forker.client.Forker.class.getName());
//
//			} else 
			if (isUsingWrappedOnClasspath) {
				classname = WRAPPED_CLASS_NAME;
				tail.add(com.sshtools.forker.client.Forker.class.getName());
			} else {
//				if (modulepath != null && isUsingClient(modulepath)) {
//					headArgs.add("-m");
//					headArgs.add(com.sshtools.forker.client.Forker.class.getPackageName() + "/"
//							+ com.sshtools.forker.client.Forker.class.getName());
//				} else
				classname = com.sshtools.forker.client.Forker.class.getName();
			}
			headArgs.add(String.valueOf(OS.isAdministrator()));
			tail.add(app.getClassname());
			if (app.hasArguments())
				tail.addAll(Arrays.asList(app.getArguments()));
		} else {
			/*
			 * Otherwise we are just running the application directly or via Wrapped
			 */

			// TODO how
//			if (modulepath != null && StringUtils.isNotBlank(app.getModule())) {
//				command.add("--add-modules");
//				command.add(app.getModule());
//			}

//			if (isUsingWrappedOnModulepath) {
//				headArgs.add("-m");
//				headArgs.add(WRAPPED_MODULE_NAME + "/" + WRAPPED_CLASS_NAME);
//				tail.add(app.getClassname());
//			} else 
			if (isUsingWrappedOnClasspath) {
				classname = WRAPPED_CLASS_NAME;
				tail.add(app.getClassname());
			} else {
//				if (StringUtils.isNotBlank(app.getModule())) {
//					headArgs.add("-m");
//					tail.add(app.fullClassAndModule());
//				} else
				classname = app.getClassname();
			}
			if (app.hasArguments())
				tail.addAll(Arrays.asList(app.getArguments()));
		}

		List<String> allArgs = new ArrayList<>();
		allArgs.addAll(headArgs);
		allArgs.addAll(tail);

		usingWrapped = isUsingWrappedOnClasspath; // TODO || isUsingWrappedOnModulepath

		/* Directory and IO */
		// appBuilder.directory(cwd);

		/* Environment variables */

//		for (String env : configuration.getOptionValues("setenv")) {
//			String key = env;
//			String value = "";
//			int idx = env.indexOf('=');
//			if (idx != -1) {
//				key = env.substring(0, idx);
//				value = env.substring(idx + 1);
//			}
//			appBuilder.environment().put(key, value);
//		}

		/* Run as as administrator or a specific user */
		if (configuration.getSwitch("administrator", false)) {
			if (!OS.isAdministrator()) {
				throw new IOException("Cannot raise privileges of entire application when running with no-fork.");
			}
		} else {
			String runas = configuration.getOptionValue("run-as", null);
			if (runas != null && !runas.equals(System.getProperty("user.name"))) {
				throw new IOException("Cannot switch user when running with no-fork.");
			}
		}

		URLClassLoader urlClassLoader = new URLClassLoader(urls.toArray(new URL[0]),
				ClassLoader.getSystemClassLoader().getParent());

		// relative to that classloader, find the main class
		// you want to bootstrap, which is the first cmd line arg
		Class<?> mainClass;
		try {
			mainClass = urlClassLoader.loadClass(classname);
		} catch (ClassNotFoundException e) {
			throw new IOException("Could not find application class.", e);
		}
		logger.info(String.format("Created isolated classloader %s", urlClassLoader.hashCode()));
		Method main;
		try {
			main = mainClass.getMethod("main", new Class[] { String[].class });
		} catch (NoSuchMethodException | SecurityException e) {
			throw new IOException("Application class does not have a main(String[]) method.", e);
		}

		// well-behaved Java packages work relative to the
		// context classloader. Others don't (like commons-logging)
		Thread.currentThread().setContextClassLoader(urlClassLoader);

		// you want to prune the first arg because its your main class.
		// you want to pass the remaining args as the "real" args to your main

		/*
		 * Take note of what threads are running now. We then invoke the main method,
		 * and wait for any threads that weren't there before to complete (so to cleanly
		 * shutdown the app must stop all of it's own threads).
		 */
		Thread[] threads = new Thread[Thread.activeCount()];
		Thread.enumerate(threads);

		try {
			event(STARTING_APPLICATION, String.valueOf(times), resolveCwd().getAbsolutePath(), app.fullClassAndModule(),
					String.valueOf(lastRetVal));
			logger.info(String.format("Executing: %s %s", app.getClassname(), String.join(" ", allArgs)));

//			AtomicInteger exitCode = new AtomicInteger(Integer.MIN_VALUE);
//			SecurityManager exitDefeat = new SecurityManager() {
//				@Override
//				public void checkExit(int status) {
//					exitCode.set(status);
//					Set<Thread> remaining = getAppThreads(threads);
//					for (Thread t : remaining) {
//						if (t != Thread.currentThread())
//							t.interrupt();
//					}
//					throw new SecurityException();
//				}
//
//				@Override
//				public void checkPermission(Permission perm) {
//					// Allow other activities by default
//					// TODO delegate to previous manager if there is one
//				}
//			};
//			System.setSecurityManager(exitDefeat);

			monitorConfigurationFiles();

			main.invoke(null, new Object[] { allArgs.toArray(new String[0]) });
			event(STARTED_APPLICATION, app.fullClassAndModule());
			// while (exitCode.get() != Integer.MIN_VALUE) {
			while (true) {
				Set<Thread> remaining = getAppThreads(threads);
				if (remaining.isEmpty())
					break;
				logger.info(
						String.format("There are %d additional threads after launching, waiting for completion of all.",
								remaining.size()));
				for (Thread remain : remaining) {
					if (remain == null)
						break;
					if (!remain.isDaemon()) {
						try {
							remain.join();
						} catch (InterruptedException e) {
						}
					}
				}
			}
			return 0;
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			logger.log(Level.SEVERE, "Failed to launch app via no-fork.", e);
			return 1;
		} finally {
			urlClassLoader.close();
		}

	}

	/**
	 * Gets the app threads.
	 *
	 * @param threads the threads
	 * @return the app threads
	 */
	protected Set<Thread> getAppThreads(Thread[] threads) {
		Thread[] nowThreads = new Thread[Thread.activeCount()];
		Thread.enumerate(nowThreads);
		Set<Thread> remaining = new HashSet<>(Arrays.asList(nowThreads));
		remaining.removeAll(Arrays.asList(threads));
		return remaining;
	}

	/**
	 * Gets the lock file.
	 *
	 * @return the lock file
	 */
	protected File getLockFile() {
		if (isSingleInstancePerUser())
			return new File(new File(System.getProperty("java.io.tmpdir")),
					"forker-wrapper-" + app.getClassname() + "-" + System.getProperty("user.name") + ".lock");
		else
			return new File(new File(System.getProperty("java.io.tmpdir")),
					"forker-wrapper-" + app.getClassname() + ".lock");
	}

	/**
	 * Execute jmx command in app.
	 *
	 * @param method the method
	 * @param args the args
	 * @return the object
	 * @throws MalformedObjectNameException the malformed object name exception
	 * @throws AttachNotSupportedException the attach not supported exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws MalformedURLException the malformed URL exception
	 * @throws InstanceNotFoundException the instance not found exception
	 * @throws MBeanException the m bean exception
	 * @throws ReflectionException the reflection exception
	 */
	protected Object executeJmxCommandInApp(String method, Object... args)
			throws MalformedObjectNameException, AttachNotSupportedException, IOException, MalformedURLException,
			InstanceNotFoundException, MBeanException, ReflectionException {
		logger.log(Level.FINE,
				String.format("Executing %s in remote app with arguments %s", method, Arrays.asList(args)));
		checkJMXConnectionToWrapped();
		if (jmxConnectionToWrapped != null) {
			String[] classNames = new String[args.length];
			for (int i = 0; i < args.length; i++)
				classNames[i] = args[i].getClass().getName();
			return jmxConnectionToWrapped.invoke(jmxObjectName, method, args, classNames);
		}
		throw new IllegalArgumentException("Could not find remote app.");
	}

	/**
	 * Check JMX connection to wrapped.
	 *
	 * @throws MalformedObjectNameException the malformed object name exception
	 * @throws AttachNotSupportedException the attach not supported exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws MalformedURLException the malformed URL exception
	 */
	protected void checkJMXConnectionToWrapped()
			throws MalformedObjectNameException, AttachNotSupportedException, IOException, MalformedURLException {
		if (jmxConnectionToWrapped == null) {
			List<VirtualMachineDescriptor> vms = VirtualMachine.list();
			for (VirtualMachineDescriptor desc : vms) {
				List<String> dn = Arrays.asList(desc.displayName().split(" "));
				if (dn.contains(WRAPPED_MODULE_NAME + "/" + WRAPPED_CLASS_NAME) || dn.contains(WRAPPED_CLASS_NAME)) {
					jmxObjectName = new ObjectName(
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
					jmxConnectionToWrapped = JMXConnectorFactory.connect(jmxUrl).getMBeanServerConnection();
					break;

				}
			}
		}
	}

	/**
	 * Checks if is log overwrite.
	 *
	 * @return true, if is log overwrite
	 */
	private boolean isLogOverwrite() {
		return configuration.getSwitch("log-overwrite", false);
	}

	/**
	 * Checks if is quiet stdout.
	 *
	 * @return true, if is quiet stdout
	 */
	private boolean isQuietStdout() {
		return isQuiet() || configuration.getSwitch("quiet-stdout", false);
	}

	/**
	 * Checks if is quiet stderr.
	 *
	 * @return true, if is quiet stderr
	 */
	private boolean isQuietStderr() {
		return isQuiet() || configuration.getSwitch("quiet-stderr", false);
	}

	/**
	 * Checks if is daemon.
	 *
	 * @return true, if is daemon
	 */
	private boolean isDaemon() {
		return configuration.getSwitch("daemon", false);
	}

	/**
	 * Resolve wrapper classpath.
	 *
	 * @return the string
	 */
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

	/**
	 * Resolve wrapper modulepath.
	 *
	 * @return the string
	 */
	private String resolveWrapperModulepath() {
		String forkerModulepath = System.getProperty("java.module.path");
		String wrapperClasspath = configuration.getOptionValue("modulepath", forkerModulepath);
		return wrapperClasspath;
	}

	/**
	 * Checks if is native main.
	 *
	 * @return true, if is native main
	 */
	private boolean isNativeMain() {
		return configuration.getSwitch("native", false);
	}

	/**
	 * Checks if is use daemon.
	 *
	 * @return true, if is use daemon
	 */
	private boolean isUseDaemon() {
		boolean nativeMain = isNativeMain();
		return !nativeMain && !configuration.getSwitch("no-forker-daemon", nativeMain);
	}

	/**
	 * Resolve cwd.
	 *
	 * @return the file
	 */
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

	/**
	 * Gets the arg mode.
	 *
	 * @return the arg mode
	 */
	public ArgMode getArgMode() {
		return ArgMode.valueOf(configuration.getOptionValue("argmode", ArgMode.DEFAULT.name()));
	}

	/**
	 * Gets the argfile mode.
	 *
	 * @return the argfile mode
	 */
	public ArgfileMode getArgfileMode() {
		return ArgfileMode.valueOf(configuration.getOptionValue("argfilemode", ArgfileMode.COMPACT.name()));
	}

	/**
	 * Gets the app name.
	 *
	 * @return the app name
	 */
	public static String getAppName() {
		String an = System.getenv("FORKER_APPNAME");
		return an == null || an.length() == 0 ? ForkerWrapper.class.getName() : an;
	}

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		ForkerWrapper wrapper = new ForkerWrapper();
		wrapper.getWrappedApplication().setArguments(args);

		CommandSpec opts = CommandSpec.create();
//		opts.parser().stopAtUnmatched(true);
//		opts.parser().unmatchedOptionsArePositionalParams(true);
		opts.mixinStandardHelpOptions(true);
		// Add the options always available

		opts.usageMessage().header("Forker Wrapper", "Provided by JAdpative.");
		opts.usageMessage().description("Forker Wrapper is used to launch Java applications, optionally changing "
				+ "the user they are run as, providing automatic restarting, signal handling and "
				+ "other facilities that will be useful running applications as a 'service'.\n\n"
				+ "Configuration may be passed to Forker Wrapper in four different ways :-\n\n"
				+ "1. Command line options.\n\n" + "2. Configuration files (see -c and -C options)\n\n"
				+ "3. Java system properties. The key of which is option name prefixed with 'forker.' and with - replaced with a dot (.)\n\n"
				+ "4. Environment variables. The key of which is the option name prefixed with   'FORKER_' (in upper case) with - replaced with _\n\n"
				+ "You can also narrow any configuration key down to a specific platform by prefixing it with "
				+ "one of 'windows', 'mac-osx', 'linux', 'unix' or 'other'. The exact format will depend on "
				+ "whether you are using options, files, system properties or environment variables. For "
				+ "example, to specify '-XstartOnFirstThread' as a JVM argument for only Max OSX as an "
				+ "option, you would use '--mac-osx-jvmarg=\"-XstartOnFirstThread\".");

		wrapper.addOptions(opts);
		wrapperMain(args, wrapper, opts);
	}

	/**
	 * Wrapper main.
	 *
	 * @param args the args
	 * @param wrapper the wrapper
	 * @param opts the opts
	 */
	public static void wrapperMain(String[] args, ForkerWrapper wrapper, CommandSpec opts) {
		CommandLine cl = new CommandLine(opts);
		cl.setTrimQuotes(true);
		cl.setUnmatchedArgumentsAllowed(true);
		cl.setUnmatchedOptionsAllowedAsOptionParameters(true);
		cl.setUnmatchedOptionsArePositionalParams(true);
		try {
			ParseResult cmd = cl.parseArgs(args);
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
			if (CommandLine.printHelpIfRequested(cmd)) {
				System.exit(0);
			}
			for (String cfg : wrapper.getConfiguration().getOptionValues("configuration")) {
				wrapper.readConfigFile(new File(cfg));
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
					cl.usage(new PrintWriter(System.err, true));
					return 1;
				}
			});
			if (ret != Integer.MIN_VALUE)
				System.exit(ret);
		} catch (Throwable e) {
			e.printStackTrace();
			System.err.println(String.format("%s: %s\n", wrapper.getClass().getName(), e.getMessage()));
			cl.usage(new PrintWriter(System.err, true));
			System.exit(1);
		}

	}

	/**
	 * Gets the configuration.
	 *
	 * @return the configuration
	 */
	public Configuration getConfiguration() {
		return configuration;
	}

	/**
	 * Inits the.
	 *
	 * @param cmd the cmd
	 */
	public void init(ParseResult cmd) {
		if (!inited) {
			configuration.init(cmd);
			reconfigureLogging();
			for (WrapperPlugin p : plugins) {
				try {
					p.init(this);
				} catch (Exception e) {
					logger.log(Level.SEVERE, "Failed to load plugin.", e);
				}
			}
			inited = true;
		}
	}

	/**
	 * Sets the log level.
	 *
	 * @param lvl the new log level
	 */
	@Override
	public void setLogLevel(String lvl) {
		setLogLevel(Level.parse(lvl));
	}

	/**
	 * Gets the logger.
	 *
	 * @return the logger
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * Sets the log level.
	 *
	 * @param lvl the new log level
	 */
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

	/**
	 * Reconfigure logging.
	 */
	private void reconfigureLogging() {
		String levelName = configuration.getOptionValue("level", "WARNING");
		Level lvl = Level.parse(levelName);
		setLogLevel(lvl);
	}

	/**
	 * Stop.
	 *
	 * @param wait the wait
	 * @param restart the restart
	 * @throws InterruptedException the interrupted exception
	 */
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

	/**
	 * Gets the JVM path.
	 *
	 * @return the JVM path
	 */
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

	/**
	 * Event.
	 *
	 * @param name the name
	 * @param args the args
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
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
			for (WrapperPlugin plugin : plugins) {
				if (plugin.event(name, cmd, args)) {
					return;
				}
			}

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

	/**
	 * Adds the options.
	 *
	 * @param options the options
	 */
	protected void addOptions(CommandSpec options) {

		for (WrapperPlugin plugin : plugins) {
			plugin.addOptions(options);
		}
		for (String event : EVENT_NAMES) {
			options.addOption(OptionSpec.builder("--on-" + event).paramLabel("command-or-classname").type(String.class)
					.description("Executes a script or a Java class (that must be on wrappers own classpath) "
							+ "when a particular event occurs. If a Java class is to be execute, it "
							+ "must contain a main(String[] args) method. Each event may pass a number of arguments.")
					.build());
		}
		options.addOption(OptionSpec.builder("--stop").description(
				"If single-instance mode is enabled, and the wrapped application includes the forker-wrapped module,"
						+ "then a stop command is sent. It is up to app whether or not to exit the runtime through the use of "
						+ "the 'ShutdownListener' registered on the 'Wrapped' instance. If it is happy to stop, it should do it's "
						+ "own clean up, then System.exit(). ")
				.build());
		options.addOption(OptionSpec.builder("-x", "--allow-execute").paramLabel("spec").type(String.class)
				.description("The wrapped application can use it's wrapper to execute commands on it's behalf. If the "
						+ "wrapper itself runs under an administrative user, and the application as a non-privileged user,"
						+ "you may wish to restrict which commands may be run. One or more of these options specifies the "
						+ "name of the command that may be run. The value may be a regular expression, see also 'prevent-execute'")
				.build());
		options.addOption(OptionSpec.builder("-X", "--reject-execute").paramLabel("pattern").type(String.class)
				.description("The wrapped application can use it's wrapper to execute commands on it's behalf. If the "
						+ "wrapper itself runs under an administrative user, and the application as a non-privileged user,"
						+ "you may wish to restrict which commands may be run. One or more of these options specifies the "
						+ "name of the commands that may NOT be run. The value may be a regular expression, see also 'allow-execute'")
				.build());
		options.addOption(OptionSpec.builder("-F", "--no-forker-classpath")
				.description("When the forker daemon is being used, the wrappers own classpath will be appened to "
						+ "to the application classpath. This option prevents that behaviour for example if "
						+ "the application includes the modules itself.")
				.build());
		options.addOption(OptionSpec.builder("-K", "--monitor-configuration").description(
				"Monitor for configuration file changes. Some changes can be applied while the wrapped application is running, while "
						+ "some may cause the application to be restarted, and finally others may have no effect at all (until forker itself is restarted).")
				.build());
		options.addOption(OptionSpec.builder("-k", "--never-restart").description(
				"Prevent wrapper from restarting the process, regardless of the exit value from the spawned process. Totall overrides "
						+ "dont-restart-on and restart-on options.")
				.build());
		options.addOption(OptionSpec.builder("-r", "--restart-on").paramLabel("exitCodes").type(String.class)
				.description(
						"Which exit values from the spawned process will cause the wrapper to attempt to restart it. When not specified, all exit "
								+ "values will cause a restart except those that are configure not to (see dont-restart-on).")
				.build());
		options.addOption(OptionSpec.builder("-R", "--dont-restart-on").paramLabel("exitCodes").type(String.class)
				.description(
						"Which exit values from the spawned process will NOT cause the wrapper to attempt to restart it. By default,"
								+ "this is set to 0, 1 and 2. See also 'restart-on'")
				.build());
		options.addOption(OptionSpec.builder("-w", "--restart-wait").paramLabel("seconds").type(int.class)
				.description("How long (in seconds) to wait before attempting a restart.").build());
		options.addOption(OptionSpec.builder("-d", "--daemon")
				.description("Fork the process and exit, leaving it running in the background.").build());
		options.addOption(OptionSpec.builder("-n", "--no-forker-daemon").description(
				"Do not enable the forker daemon. This will prevent the forked application from executing elevated commands via the daemon and will also disable JVM timeout detection.")
				.build());
		options.addOption(OptionSpec.builder("-q", "--quiet")
				.description("Do not output anything on stderr or stdout from the wrapped process.").build());
		options.addOption(OptionSpec.builder("-z", "--quiet-stderr")
				.description("Do not output anything on stderr from the wrapped process.").build());
		options.addOption(OptionSpec.builder("-Z", "--quiet-stdout")
				.description("Do not output anything on stdout from the wrapped process.").build());
		options.addOption(OptionSpec.builder("-S", "--single-instance")
				.description("Only allow one instance of the wrapped application to be active at any one time. "
						+ "This is achieved through locked files.")
				.build());
		options.addOption(OptionSpec.builder("--single-instance-per-user").description(
				"When single-instance is installed, by default it means a single instance on the entire local system. Adding "
						+ "this flag allows a single instance per username.")
				.build());
		options.addOption(OptionSpec.builder("-s", "--setenv").paramLabel("name=value").type(String.class).description(
				"Set an environment on the wrapped process. This is in the format NAME=VALUE. The option may be "
						+ "specified multiple times to specify multiple environment variables.")
				.build());
		options.addOption(OptionSpec.builder("-N", "--native")
				.description("This option signals that main is not a Java classname, it is instead the name "
						+ "of a native command. This option is incompatible with 'classpath' and also "
						+ "means the forker daemon will not be used and so hang detection and some other "
						+ "features will not be available.")
				.build());
		options.addOption(OptionSpec.builder("--no-fork")
				.description("When this option is specified, instead of starting a new JVM an isolated "
						+ "classloader will be created and the application loaded using the same JVM as the wrapper. "
						+ "A number of features will not be available in this mode.")
				.build());
		options.addOption(OptionSpec.builder("-I", "--no-info")
				.description("Ordinary, forker will set some system properties in the wrapped application. These "
						+ "communicate things such as the last exited code (forker.info.lastExitCode), number "
						+ "of times start via (forker.info.attempts) and more. This option prevents those being set.")
				.build());
		options.addOption(OptionSpec.builder("-o", "--log-overwrite")
				.description("Overwriite logfiles instead of appending.").build());
		options.addOption(OptionSpec.builder("-l", "--log").paramLabel("file").type(File.class).description(
				"Where to log stdout (and by default stderr) output. If not specified, will be output on stdout (or stderr) of this process.")
				.build());
		options.addOption(OptionSpec.builder("-L", "--level").paramLabel("level").type(String.class).description(
				"Output level for information and debug output from wrapper itself (NOT the application). By default "
						+ "this is WARNING, with other possible levels being FINE, FINER, FINEST, SEVERE, INFO, ALL.")
				.build());
		options.addOption(OptionSpec.builder("-D", "--log-write-delay").paramLabel("milliseconds").type(Long.class)
				.description(
						"In order to be compatible with external log rotation, log files are closed as soon as they are "
								+ "written to. You can delay the closing of the log file, so that any new log messages that are "
								+ "written within this time will not need to open the file again. The time is in milliseconds "
								+ "with a default of 50ms. A value of zero indicates to always immmediately reopen the log.")
				.build());
		options.addOption(OptionSpec.builder("-e", "--errors").paramLabel("file").type(String.class).description(
				"Where to log stderr. If not specified, will be output on stderr of this process or to 'log' if specified.")
				.build());
		options.addOption(OptionSpec.builder("-cp", "--classpath").paramLabel("classpath").type(String.class)
				.description(
						"The classpath to use to run the application. If not set, the current runtime classpath is used (the java.class.path system property). Prefix the "
								+ "path with '+' to add it to the end of the existing classpath, or '-' to add it to the start.")
				.build());
		options.addOption(OptionSpec.builder("-mp", "--modulepath").paramLabel("modulepath").type(String.class)
				.description(
						"The modulepath to use to run the application. If not set, the current runtime default is used. Prefix the "
								+ "path with '+' to add it to the end of the existing modulepath, or '-' to add it to the start.")
				.build());
		options.addOption(OptionSpec.builder("-bcp", "--boot-classpath").paramLabel("classpath").type(String.class)
				.description(
						"The boot classpath to use to run the application. If not set, the current runtime classpath is used (the java.class.path system property). Prefix the "
								+ "path with '+' to add it to the end of the existing classpath, or '-' to add it to the start. Use of a jvmarg that starts with '-Xbootclasspath' will "
								+ "override this setting.")
				.build());
		options.addOption(OptionSpec.builder("-u", "--run-as").paramLabel("user").type(String.class)
				.description("The user to run the application as.").build());
		options.addOption(OptionSpec.builder("--argfile").paramLabel("argfile").type(String.class).description(
				"By default, the wrapper will try and create the argfile in the working directory. If this is "
						+ "not possible, the system temporary directory is used. This option forces a particular file path to be used.")
				.build());
		options.addOption(OptionSpec.builder("--argfilemode").paramLabel("mode").type(ArgfileMode.class).description(
				"Specifies how arguments will be provided to the java command. Possible values are 'COMPACT', "
						+ "(all arguments except for the classname and it's arguments are placed in an @argfile), "
						+ "'ARGFILE' (all arguments including classname and it's arguments in place in an @argfile), 'EXPANDED' ("
						+ "an @argfile is not used, all argumentes are part of the command). ")
				.build());
		options.addOption(OptionSpec.builder("-a", "--administrator").description("Run as administrator.").build());
		options.addOption(OptionSpec.builder("-p", "--pidfile").paramLabel("file").type(String.class)
				.description("A filename to write the process ID to. May be used "
						+ "by external application to obtain the PID to send signals to.")
				.build());
		options.addOption(OptionSpec.builder("-b", "--buffer-size").paramLabel("bytes").type(String.class)
				.description(
						"How big (in byte) to make the I/O buffer. By default this is 1 byte for immediate output.")
				.build());
		options.addOption(OptionSpec.builder("-B", "--cpu").paramLabel("cpus").type(String.class)
				.description("Bind to a particular CPU, may be specified multiple times to bind to multiple CPUs.")
				.build());
		options.addOption(OptionSpec.builder("-j", "--java").paramLabel("file").type(String.class)
				.description("Alternative path to java runtime launcher.").build());
		options.addOption(OptionSpec.builder("-J", "--jvmarg").paramLabel("jvmarg").type(String.class)
				.description("Additional VM argument. Specify multiple times for multiple arguments.").build());
		options.addOption(OptionSpec.builder("-sp", "--system").paramLabel("name=value").type(String.class)
				.description("Additional system properties.").build());
		options.addOption(OptionSpec.builder("-W", "--cwd").paramLabel("directory").type(File.class)
				.description("Change working directory, the wrapped process will be run from this location.").build());
		options.addOption(OptionSpec.builder("-t", "--timeout").paramLabel("milliseconds").type(Long.class)
				.description("How long to wait since the last 'ping' from the launched application before "
						+ "considering the process as hung. Requires forker daemon is enabled.")
				.build());
		options.addOption(OptionSpec.builder("-f", "--jar").paramLabel("jar").type(File.class).description(
				"The path of a jar file to run. If this is specified, then this path will be added to the classpath, and META-INF/MANIFEST.MF examined for Main-Class for the"
						+ "main class to run. The first argument passed to the command becomes the first app argument.")
				.build());
		options.addOption(OptionSpec.builder("-m", "--main").paramLabel("main").type(String.class).description(
				"The classname to run. If this is specified, then the first argument passed to the command "
						+ "becomes the first app argument. This may also be a module path (<module>/<class>), in which case the -m argument will be appended to the command as well.")
				.build());
		options.addOption(OptionSpec.builder("-E", "--exit-wait").paramLabel("milliseconds").type(Long.class)
				.description(
						"How long to wait after attempting to stop a wrapped appllication before giving up and forcibly killing the applicaton.")
				.build());
		options.addOption(OptionSpec.builder("-M", "--argmode").paramLabel("argmode").type(ArgMode.class).description(
				"Determines how apparg options are treated. May be one FORCE, APPEND, PREPEND or DEFAULT. FORCE "
						+ "passed on only the appargs specified by configuration. APPEND will append all appargs to "
						+ "any command line arguments, PREPEND will prepend them. Finally DEFAULT is the default behaviour "
						+ "and any command line arguments will override all appargs.")
				.build());
		options.addOption(OptionSpec.builder("-A", "--apparg").paramLabel("apparg").type(String.class).description(
				"Application arguments. How these are treated depends on argmode, but by default the will be overridden by any command line arguments passed in.")
				.build());
		options.addOption(OptionSpec.builder("-H", "--splash").paramLabel("image-path").type(String.class).description(
				"Specify a splash image. May be specified multiple times, the first existing file will be found, and added the appropriate option added to the launcher. ")
				.build());
		options.addOption(OptionSpec.builder("-P", "--priority").paramLabel("priority").type(String.class)
				.description("Scheduling priority, may be one of LOW, NORMAL, HIGH or REALTIME (where supported).")
				.build());
		options.addOption(OptionSpec.builder("-Y", "--min-java").paramLabel("version").type(String.class)
				.description("Minimum java version. If the selected JVM (default or otherwise) is lower than this, an "
						+ "attempt will be made to locate a later version.")
				.build());
		options.addOption(OptionSpec.builder("-y", "--max-java").paramLabel("version").type(String.class)
				.description("Maximum java version. If the selected JVM (default or otherwise) is lower than this, an "
						+ "attempt will be made to locate an earlier version.")
				.build());
		options.addOption(OptionSpec.builder("-i", "--init-temp").paramLabel("directory").type(File.class).description(
				"Initialise a named temporary folder before execution of application. The folder will be created if it does not exist, and emptied if it exists and has contents.")
				.build());
		options.addOption(OptionSpec.builder("-T", "--to-temp").paramLabel("directory").type(File.class)
				.description(
						"Copy file(s) to the named temporary folder. Supports glob syntax for final part of the path.")
				.build());
		options.addOption(OptionSpec.builder("-G", "--service-mode").paramLabel("serviceOp").type(String.class)
				.description(
						"When enabled, 'start', 'stop', 'restart' and 'status' arguments can be passed which act in the same way as service control commands on Linux and similar operating systems.")
				.build());
		options.addOption(OptionSpec.builder("-U", "--debug").arity("0..1").paramLabel("debugOptions")
				.type(String.class)
				.description(
						"Adds default neccessary properties for remote debugging. If an argument is provided, is should either be true,false, or a list of comma separated name=value pairs of any parameters to pass to the debugger agent.")
				.build());

		// Add the command line launch options
		options.addOption(OptionSpec.builder("-c", "--configuration").paramLabel("file").type(String.class).description(
				"A file to read configuration. This can either be a JavaScript file that evaluates to an object "
						+ "containing keys and values of the configuration options (use arrays for multiple value commands), or "
						+ "it may be a simple text file that contains name=value pairs, where name is the same name as used for command line "
						+ "arguments (see --help for a list of these)")
				.build());
		options.addOption(OptionSpec.builder("-C", "--configuration-directory").paramLabel("directory")
				.type(String.class)
				.description(
						"A directory to read configuration files from. Each file can either be a JavaScript file that evaluates to an object "
								+ "containing keys and values of the configuration options (use arrays for multiple value commands), or "
								+ "it may be a simple text file that contains name=value pairs, where name is the same name as used for command line "
								+ "arguments (see --help for a list of these)")
				.build());
		options.addOption(OptionSpec.builder("-FO", "--fdout").description("File descriptor for stdout")
				.paramLabel("fd").type(int.class).build());
		options.addOption(OptionSpec.builder("-FE", "--fderr").description("File descriptor for stderr")
				.paramLabel("fd").type(int.class).build());

		options.addPositional(PositionalParamSpec.builder().paramLabel("classNameOrExecutable").type(String.class)
				.description("The classname or executable name or path of the application to run.").build());
		options.addPositional(PositionalParamSpec.builder().paramLabel("arguments").type(String.class).arity("1..*")
				.description("All other arguments to pass on to the wrapped application.").build());

		options.name(getAppName());
	}

	/**
	 * Builds the path.
	 *
	 * @param cwd the cwd
	 * @param defaultClasspath the default classpath
	 * @param classpath the classpath
	 * @param appendByDefault the append by default
	 * @return the string
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	protected static String buildPath(File cwd, String defaultClasspath, String classpath, boolean appendByDefault)
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
			for (String el : splitCrossPlatformPath(classpath)) {
				el = el.replace('/', File.separatorChar);
				if (el.contains("*") || el.contains("?")) {
					Path root = cwd.toPath();
					
					/* If the element path is an absolute pattern, then we must discover the best
					 * root to use. Using CWD won't work for example if the absolute path pattern
					 * is outside of CWD.
					 * 
					 * To do this we find the first occurrence of a wild card separator, and 
					 * use the first WHOLE path up to this point.
					 * 
					 * If this path is absolute, we then use that as the root for the wildcard 
					 * matching and make the element path relative to that.
					 */
					int firstSingleIdx = el.indexOf('?');
					int firstAnyIdx = el.indexOf('*');
					int firstWildcardIdx = Math.min(firstAnyIdx == -1 ? Integer.MAX_VALUE : firstAnyIdx, 
							firstSingleIdx == -1 ? Integer.MAX_VALUE : firstSingleIdx);
					String elRootPath = el.substring(0, firstWildcardIdx);
					int lastSeparator = elRootPath.lastIndexOf(File.separatorChar);
					if(lastSeparator != -1) {
						elRootPath = elRootPath.substring(0, lastSeparator);
						Path possiblePath = Paths.get(elRootPath);
						if(possiblePath.isAbsolute()) {
							root = possiblePath;
							el = el.substring(lastSeparator + 1);
						}
					}
					Finder finder = new Finder(root, el, newClasspath);					
					Files.walkFileTree(root, finder);
					
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

	/**
	 * Split cross platform path.
	 *
	 * @param paths the paths
	 * @return the string[]
	 */
	static String[] splitCrossPlatformPath(String paths) {
		if (paths.contains("|")) {
			/* Generic path */
			return paths.split("\\|");
		} else if (paths.contains("\\") || paths.contains(";")) {
			/* Windows path */
			return paths.split(";");
		} else {
			/* Windows forward slash path with drive */
			StringBuilder el = new StringBuilder();
			char[] chars = paths.toCharArray();
			List<String> parts = new ArrayList<>();
			for (int i = 0; i < chars.length; i++) {
				char c = chars[i];
				if (el.length() == 0 && Character.isAlphabetic(c) && i < chars.length - 2 && chars[i + 1] == ':'
						&& (chars[i + 2] == '/' || chars[i + 2] == '\\')) {
					/* This an the next two characters is a path with a drive */
					el.append(c);
					el.append(chars[++i]);
					el.append(chars[++i]);
				}
				else if(c == ':') {
					/* Colon that isn't part of the part, must be a colon separated path with forward slashes */
					parts.add(el.toString());
					el.setLength(0);;
				}
				else
					el.append(c);
			}
			if(el.length() > 0)
				parts.add(el.toString());
			return parts.toArray(new String[0]);
		}

	}

	/**
	 * Start forker daemon.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
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

	/**
	 * Checks if is no fork.
	 *
	 * @return true, if is no fork
	 */
	protected boolean isNoFork() {
		return configuration.getSwitch("no-fork", false);
	}

	/**
	 * Daemonize.
	 *
	 * @param javaExe the java exe
	 * @param forkerClasspath the forker classpath
	 * @param forkerModulepath the forker modulepath
	 * @param daemonize the daemonize
	 * @param pidfile the pidfile
	 * @return true, if successful
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	protected boolean daemonize(String javaExe, String forkerClasspath, String forkerModulepath, boolean daemonize,
			String pidfile) throws IOException {
		if (daemonize && configuration.getOptionValue("fallback-active", null) == null) {
			if ("true".equals(configuration.getOptionValue("native-fork", "false"))) {
				if (isNoFork())
					throw new IOException("Cannot daemonize when no-fork is set.");

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
						writeLines(makeDirectoryForFile(relativize(resolveCwd(), pidfile)),
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
				writeLines(makeDirectoryForFile(relativize(resolveCwd(), pidfile)), Arrays.asList(String.valueOf(pid)));
			}
		}
		return false;
	}

	/**
	 * Write lines.
	 *
	 * @param file the file
	 * @param lines the lines
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private static void writeLines(File file, List<String> lines) throws IOException {
		try (PrintWriter w = new PrintWriter(new FileWriter(file), true)) {
			for (String line : lines)
				w.println(line);
		}
	}

	/**
	 * Adds the shutdown hook.
	 *
	 * @param useDaemon the use daemon
	 */
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
				if (p != null && usingWrapped) {
					logger.info("Shutting down via JMX as host applicaction is using the forker-wrapped module.");
					shutdownWrapped();
					p = null;
				}
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

	/**
	 * Monitor configuration files.
	 */
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
			logger.info("Stopped monitoring configuration files.");
		}
	}

	/**
	 * Monitor wrapped JMX application.
	 */
	protected void monitorWrappedJMXApplication() {
		if (!isNoFork() && usingWrapped && monitorThread == null) {
			monitorThread = new Thread() {
				{
					setName("ForkerWrapperMonitor");
					setDaemon(true);
				}

				@Override
				public void run() {
					logger.info("Pinging wrapped application");
					try {
						int timeout = Integer.parseInt(configuration.getOptionValue("timeout", "60"));
						long lastPing = 0;
						while (!stopping) {
							if (process != null) {
								try {
									executeJmxCommandInApp("ping");
									lastPing = System.currentTimeMillis();
								} catch (Exception e) {
									if (lastPing > 0 && (lastPing + timeout * 1000) <= System.currentTimeMillis()) {
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

	/**
	 * Monitor wrapped application.
	 */
	protected void monitorWrappedApplication() {
		if (!usingWrapped && isUseDaemon() && Integer.parseInt(configuration.getOptionValue("timeout", "60")) > 0
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

	/**
	 * Append path.
	 *
	 * @param newClasspath the new classpath
	 * @param el the el
	 */
	protected static void appendPath(StringBuilder newClasspath, String el) {
		if (newClasspath.length() > 0)
			newClasspath.append(File.pathSeparator);
		newClasspath.append(el);
	}

	/**
	 * Read config file.
	 *
	 * @param file the file
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void readConfigFile(File file) throws IOException {
		readConfigFile(file, configuration.getProperties());
	}

	/**
	 * Read config file.
	 *
	 * @param file the file
	 * @param properties the properties
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	protected void readConfigFile(File file, List<KeyValuePair> properties) throws IOException {
		synchronized (files) {
			if (files.contains(file)) {
				logger.info(String.format("Re-loading configuration file %s", file));
			} else {
				logger.info(String.format("Loading configuration file %s", file));
				files.add(file);
			}
			for (WrapperPlugin plugin : plugins) {
				plugin.readConfigFile(file, properties);
			}
			BufferedReader fin = new BufferedReader(new FileReader(file));
			try {
				String line = null;
				while ((line = fin.readLine()) != null) {
					if (!line.trim().startsWith("#") && !line.trim().equals("")) {
						properties.add(new KeyValuePair(replaceProperties(file, line)));
					}
				}
			} finally {
				fin.close();
			}
			app.set(configuration.getOptionValue("main", null), configuration.getOptionValue("jar", null));
			reconfigureLogging();
		}
	}

	/**
	 * Replace properties.
	 *
	 * @param file the file
	 * @param line the line
	 * @return the string
	 */
	protected String replaceProperties(File file, String line) {
		Replace replace = new Replace();
		replace.pattern("\\$\\{(.*?)\\}", (p, m, r) -> {
			String key = m.group().substring(2, m.group().length() - 1);
			if (key.equals("cwd")) {
				return resolveCwd().getPath();
			} else if (key.equals("file")) {
				return file.getPath();
			} else if (key.equals("directory")) {
				return file.getParentFile().getPath();
			} else if (key.equals("user.home")) {
				return System.getProperty(key).replace("\\", "/");
			} else if (key.equals("os")) {
				if(SystemUtils.IS_OS_WINDOWS)
					return "win";
				else if(SystemUtils.IS_OS_MAC_OSX)
					return "osx";
				else if(SystemUtils.IS_OS_LINUX)
					return "linux";
				else
					return "other";
			} else if (key.equals("arch")) {
				String arch = System.getProperty("os.arch");
				if(arch.equals("amd64"))
					return "x86_64";
				return arch;
			} else
				return System.getProperty(key);
		});
		return replace.replace(line);
	}

	/**
	 * On before process.
	 *
	 * @param task the task
	 * @return true, if successful
	 */
	protected boolean onBeforeProcess(Callable<Void> task) {
		return true;
	}

	/**
	 * Make directory for file.
	 *
	 * @param file the file
	 * @return the file
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private File makeDirectoryForFile(File file) throws IOException {
		File dir = file.getParentFile();
		if (dir != null && !dir.exists() && !dir.mkdirs())
			throw new IOException(String.format("Failed to create directory %s", dir));
		return file;
	}

	/**
	 * New buffer.
	 *
	 * @return the byte[]
	 */
	private byte[] newBuffer() {
		return new byte[Integer.parseInt(configuration.getOptionValue("buffer-size", "1024"))];
	}

	/**
	 * Copy.
	 *
	 * @param in the in
	 * @param out the out
	 * @param buf the buf
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private void copy(final InputStream in, final OutputStream out, byte[] buf) throws IOException {
		int r;
		while ((r = in.read(buf, 0, buf.length)) != -1) {
			out.write(buf, 0, r);
			out.flush();
		}
	}

	/**
	 * Copy runnable.
	 *
	 * @param in the in
	 * @param out the out
	 * @return the runnable
	 */
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

	/**
	 * Inits the temp folder.
	 *
	 * @param tempFolder the temp folder
	 * @param cwd the cwd
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private void initTempFolder(String tempFolder, File cwd) throws IOException {

		File tempFile = new File(tempFolder);
		if (!tempFile.isAbsolute()) {
			tempFile = new File(cwd, tempFolder);
		}
		delTree(tempFile);

		List<String> paths = configuration.getOptionValues("to-temp");
		if (!paths.isEmpty()) {
			for (String path : paths) {
				int idx = path.lastIndexOf('/');
				String parentPath = null;
				if (idx != -1) {
					parentPath = path.substring(0, idx);
					path = path.substring(idx + 1);
				}
				PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + path);
				File parentFile = new File(parentPath);
				if (!parentFile.isAbsolute()) {
					parentFile = new File(cwd, parentPath);
				}
				File[] children = parentFile.listFiles();
				if (children != null) {
					for (File child : children) {
						if (matcher.matches(child.toPath().getFileName())) {
							Util.copy(child, new File(tempFile, child.getName()));
						}
					}
				}
			}
		}
	}

	/**
	 * Del tree.
	 *
	 * @param file the file
	 */
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

	/**
	 * Process.
	 *
	 * @param task the task
	 * @return the int
	 * @throws Exception the exception
	 */
	protected int process(Callable<Integer> task) throws Exception {
		for (WrapperPlugin plugin : plugins) {
			plugin.beforeProcess();
		}

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

	/**
	 * Continue processing.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	protected void continueProcessing() throws IOException {
		app.set(configuration.getOptionValue("main", null), configuration.getOptionValue("jar", null),
				configuration.getRemaining(), configuration.getOptionValues("apparg"), getArgMode());
	}

	/**
	 * Configuration file changed.
	 *
	 * @param file the file
	 */
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
					List<String> wasSplashes = configuration.getOptionValues("splash");
					List<String> wasSyspropArgs = configuration.getOptionValues("system");
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
							|| !Objects.equals(wasSplashes, configuration.getOptionValues("splash"))
							|| !Objects.equals(wasSyspropArgs, configuration.getOptionValues("system"))
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
						monitorWrappedJMXApplication();
						monitorConfigurationFiles();
					}
				}
			}, 1, TimeUnit.SECONDS);
		}

	}

	/**
	 * Checks if is no forker classpath.
	 *
	 * @return true, if is no forker classpath
	 */
	private boolean isNoForkerClasspath() {
		return configuration.getSwitch("no-forker-classpath", false);
	}

	/**
	 * Checks if is quiet.
	 *
	 * @return true, if is quiet
	 */
	private boolean isQuiet() {
		return configuration.getSwitch("quiet", false);
	}

	/**
	 * Checks if is single instance.
	 *
	 * @return true, if is single instance
	 */
	private boolean isSingleInstance() {
		return configuration.getSwitch("single-instance", false) || isSingleInstancePerUser();
	}

	/**
	 * Checks if is single instance per user.
	 *
	 * @return true, if is single instance per user
	 */
	private boolean isSingleInstancePerUser() {
		return configuration.getSwitch("single-instance-per-user", false);
	}

	/**
	 * On maybe restart.
	 *
	 * @param retval the retval
	 * @param lastRetVal the last ret val
	 * @return the boolean
	 * @throws Exception the exception
	 */
	protected Boolean onMaybeRestart(int retval, int lastRetVal) throws Exception {
		return null;
	}

	/**
	 * Maybe restart.
	 *
	 * @param retval the retval
	 * @param lastRetVal the last ret val
	 * @return the int
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws InterruptedException the interrupted exception
	 */
	private int maybeRestart(int retval, int lastRetVal) throws IOException, InterruptedException {

		logger.info(String.format("App has exited with %d.", retval));

		for (WrapperPlugin plugin : plugins) {
			int retVal = plugin.maybeRestart(retval, lastRetVal);
			if (retVal != Integer.MIN_VALUE)
				return retVal;
		}

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

	/**
	 * Capture streams.
	 *
	 * @param cwd the cwd
	 * @param useDaemon the use daemon
	 * @param daemonize the daemonize
	 * @param quietStdErr the quiet std err
	 * @param quietStdOut the quiet std out
	 * @param logoverwrite the logoverwrite
	 * @return the int
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws UnsupportedEncodingException the unsupported encoding exception
	 * @throws InterruptedException the interrupted exception
	 */
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
			outlog = new LazyLogStream(logDelay, makeDirectoryForFile(relativize(cwd, logpath)), !logoverwrite);
		}
		if (errpath != null) {
			if (Objects.equals(logpath, errpath))
				errlog = outlog;
			else {
				errlog = new LazyLogStream(logDelay, makeDirectoryForFile(relativize(cwd, errpath)), !logoverwrite);
			}
		}
		OutputStream stdout = quietStdOut ? null : defaultOut;
		OutputStream out = null;
		if (stdout != null) {
			if (outlog != null) {
				logger.info(String.format("Writing stdout output to stdout and %s", logpath));
				out = new TeeOutputStream(stdout, outlog);
			} else {
				logger.info("Stdout passthrough");
				out = stdout;
			}
		} else if (outlog != null) {
			logger.info(String.format("Writing stdout output %s", logpath));
			out = outlog;
		}
		if (out == null) {
			logger.info("Sinking all stdout");
			out = new SinkOutputStream();
		}
		OutputStream stderr = quietStdErr ? null : defaultErr;
		OutputStream err = null;
		if (stderr != null) {
			if (errlog != null) {
				logger.info(String.format("Writing stderr output to stderr and %s", errlog));
				err = new TeeOutputStream(stderr, errlog);
			} else {
				logger.info("Stderr passthrough");
				err = stderr;
			}
		} else if (errlog != null) {
			logger.info(String.format("Writing stderr output %s", errlog));
			err = errlog;
		}
		if (err == null) {
			if (out instanceof SinkOutputStream)
				logger.info("Sinking all stderr");
			else
				logger.info("Sending stderr to stdout");
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

	/**
	 * Builds the command.
	 *
	 * @param javaExe the java exe
	 * @param forkerClasspath the forker classpath
	 * @param forkerModulepath the forker modulepath
	 * @param wrapperClasspath the wrapper classpath
	 * @param wrapperModulePath the wrapper module path
	 * @param bootClasspath the boot classpath
	 * @param nativeMain the native main
	 * @param useDaemon the use daemon
	 * @param times the times
	 * @param lastRetVal the last ret val
	 * @return the forker builder
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private ForkerBuilder buildCommand(String javaExe, String forkerClasspath, String forkerModulepath,
			String wrapperClasspath, String wrapperModulePath, String bootClasspath, final boolean nativeMain,
			final boolean useDaemon, int times, int lastRetVal) throws IOException {

		ForkerBuilder appBuilder = new ForkerBuilder();
		String modulepath = null;
		File cwd = resolveCwd();
		boolean isUsingWrappedOnClasspath = false;
		boolean isUsingWrappedOnModulepath = false;

		/*
		 * ArgfileMode determines which arguments are placed in @argfile, and which are
		 * passed directly (tail args).
		 * 
		 * Native launching always is effectively EXPANDED
		 */
		ArgfileMode argfileMode = getArgfileMode();
		logger.log(Level.INFO, String.format("Argfile mode is %s", argfileMode));
		List<Argument> tail = new ArrayList<>();
		List<Argument> head = new ArrayList<>();
		List<Argument> command = new ArrayList<>();

		if (!nativeMain) {

			/* This is launching a Java class, so construct the classpath */
			head.add(new Argument(ArgumentType.QUOTED, javaExe));
			logger.log(Level.INFO, "Building classpath");
			String classpath = buildPath(cwd, isNoForkerClasspath() ? null : forkerClasspath, wrapperClasspath, true);
			if (classpath != null && !classpath.equals("")) {
				command.add(new Argument(ArgumentType.OPTION, "-classpath"));
				command.add(new Argument(ArgumentType.QUOTED, classpath));
				isUsingWrappedOnClasspath = isUsingWrapped(classpath);
			}

			logger.log(Level.INFO, "Building modulepath");
			modulepath = buildPath(cwd, isNoForkerClasspath() ? null : forkerModulepath, wrapperModulePath, true);
			if (modulepath != null && !modulepath.equals("")) {
				command.add(new Argument(ArgumentType.OPTION, "-p"));
				command.add(new Argument(ArgumentType.QUOTED, modulepath));
				isUsingWrappedOnModulepath = isUsingWrapped(modulepath);
			}

			for (String val : configuration.getOptionValues("splash")) {
				if(new File(val).exists()) {
					command.add(new Argument(ArgumentType.VALUED_OPTION, "-splash:" + val));
					break;
				}
			}

			boolean hasBootCp = false;
			for (String val : configuration.getOptionValues("jvmarg")) {
				if (val.startsWith("-Xbootclasspath"))
					hasBootCp = true;
				if (val.startsWith("-D")) {
					command.add(new Argument(ArgumentType.VALUED_OPTION, val));
				}
				else
					command.add(new Argument(ArgumentType.OPTION, val));
			}
			for (Object key : systemProperties.keySet()) {
				command.add(new Argument(ArgumentType.VALUED_OPTION, "-D" + key + "=" + systemProperties.getProperty((String) key)));
			}

			for (String val : configuration.getOptionValues("system")) {
				int idx = val.indexOf("=");
				/* Make sure jvmarg's are quoted */
				if (idx != -1 && !val.substring(idx + 1).startsWith("\"")) {
					command.add(new Argument(ArgumentType.VALUED_OPTION, val = val.substring(0, idx) + "=" + val.substring(idx + 1)));
				}
				else
					command.add(new Argument(ArgumentType.OPTION, "-D" + val));
			}

			if (configuration.getSwitch("debug", false)) {
				addDebugOptions(command);
			}

			if (!hasBootCp) {
				String bootcp = buildPath(cwd, null, bootClasspath, false);
				if (bootcp != null && !bootcp.equals("")) {
					/*
					 * Do our own processing of append/prepend as there are special JVM arguments
					 * for it
					 */
					if (bootClasspath != null && bootClasspath.startsWith("+"))
						command.add(new Argument(ArgumentType.VALUED_EXTENDED_OPTION, "-Xbootclasspath/a:" + bootcp));
					else if (bootClasspath != null && bootClasspath.startsWith("-"))
						command.add(new Argument(ArgumentType.VALUED_EXTENDED_OPTION, "-Xbootclasspath/p:" + bootcp));
					else
						command.add(new Argument(ArgumentType.VALUED_EXTENDED_OPTION, "-Xbootclasspath:" + bootcp));
				}
			}

			if (StringUtils.isBlank(app.getClassname()))
				throw new IllegalArgumentException(
						"Must provide a 'main' property to specify the class that contains the main() method that is your applications entry point.");
		} else {
			argfileMode = ArgfileMode.EXPANDED;
			tail = command;
		}

		usingWrapped = isUsingWrappedOnClasspath || isUsingWrappedOnModulepath;

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
					command.add(new Argument(String.format("-Dforker.info.lastExitCode=%d", lastRetVal)));
				}
				command.add(new Argument(String.format("-Dforker.info.attempts=%d", times)));
			}
		}

		/*
		 * If the daemon should be used, we assume that forker-client is on the
		 * classpath and execute the application via that, passing the forker daemon
		 * cookie via stdin. *
		 */
		List<Argument> headArgs = new ArrayList<>();
		if (useDaemon) {
			if (isUsingWrappedOnModulepath) {
				if (modulepath != null && isUsingClient(modulepath)) {
					command.add(new Argument("--add-modules"));
					command.add(new Argument(com.sshtools.forker.client.Forker.class.getPackageName()));
				}
				headArgs.add(new Argument(WRAPPED_MODULE_NAME + "/" + WRAPPED_CLASS_NAME));
				headArgs.add(new Argument(com.sshtools.forker.client.Forker.class.getName()));

			} else if (isUsingWrappedOnClasspath) {
				headArgs.add(new Argument(WRAPPED_CLASS_NAME));
				headArgs.add(new Argument(com.sshtools.forker.client.Forker.class.getName()));
			} else {
				if (modulepath != null && isUsingClient(modulepath)) {
					headArgs.add(new Argument("-m"));
					headArgs.add(new Argument(com.sshtools.forker.client.Forker.class.getPackageName() + "/"
							+ com.sshtools.forker.client.Forker.class.getName()));
				} else
					headArgs.add(new Argument(com.sshtools.forker.client.Forker.class.getName()));
			}
			headArgs.add(new Argument(String.valueOf(OS.isAdministrator())));
			tail.add(new Argument(app.getClassname()));
			if (app.hasArguments())
				for(String arg : app.getArguments())
					tail.add(new Argument(arg));
		} else {
			/*
			 * Otherwise we are just running the application directly or via Wrapped
			 */
			if (modulepath != null && StringUtils.isNotBlank(app.getModule())) {
				command.add(new Argument("--add-modules"));
				command.add(new Argument(app.getModule()));
			}

			if (isUsingWrappedOnModulepath) {
				headArgs.add(new Argument("-m"));
				headArgs.add(new Argument(WRAPPED_MODULE_NAME + "/" + WRAPPED_CLASS_NAME));
				tail.add(new Argument(app.getClassname()));
			} else if (isUsingWrappedOnClasspath) {
				headArgs.add(new Argument(WRAPPED_CLASS_NAME));
				tail.add(new Argument(app.getClassname()));
			} else {
				if (StringUtils.isNotBlank(app.getModule())) {
					headArgs.add(new Argument("-m"));
					tail.add(new Argument(app.fullClassAndModule()));
				} else
					tail.add(new Argument(app.getClassname()));
			}
			if (app.hasArguments()) {
				for(String arg : app.getArguments())
					tail.add(new Argument(arg));
			}
		}
		for(Argument arg : head) {
			appBuilder.command().add(arg.toProcessBuildArgument());
		}
		if (argfileMode.equals(ArgfileMode.ARGFILE) || argfileMode.equals(ArgfileMode.COMPACT)) {
			String argfilePath = configuration.getOptionValue("argfile", "");
			if (argfilePath.equals("")) {
				try (PrintWriter w = new PrintWriter(new FileWriter(new File(cwd, "app.args")), true)) {
					for (Argument arg : command) {
						w.println(arg.toArgFileLine());
					}
					argfilePath = "app.args";
				} catch (IOException ioe) {
					argfilePath = File.createTempFile("app", "args").getAbsolutePath();
					try (PrintWriter w = new PrintWriter(new FileWriter(argfilePath), true)) {
						for (Argument arg : command) {
							w.println(arg.toArgFileLine());
						}
					}
				}
			} else {
				try (PrintWriter w = new PrintWriter(new FileWriter(argfilePath), true)) {
					for (Argument arg : command) {
						w.println(arg.toArgFileLine());
					}
				}
			}
			appBuilder.command().add("@" + argfilePath);
		} else {
			for(Argument arg : headArgs) {
				appBuilder.command().add(arg.toProcessBuildArgument());
			}
		}

		for(Argument arg : headArgs) {
			appBuilder.command().add(arg.toProcessBuildArgument());
		}
		for(Argument arg : tail) {
			appBuilder.command().add(arg.toProcessBuildArgument());
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
			String[] nv = nameValue(env.substring(2));
			appBuilder.environment().put(nv[0], nv[1]);
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

	/**
	 * Name value.
	 *
	 * @param spec the spec
	 * @return the string[]
	 */
	private String[] nameValue(String spec) {
		String key = spec;
		String value = "";
		int idx = spec.indexOf('=');
		if (idx != -1) {
			key = spec.substring(0, idx);
			value = spec.substring(idx + 1);
		}
		return new String[] { key, value };
	}

	/**
	 * Adds the debug options.
	 *
	 * @param command the command
	 */
	private void addDebugOptions(List<Argument> command) {
		String spec = configuration.getOptionValue("debug", "").trim();
		if (!spec.equals("false")) {
			Map<String, String> debugProperties = new LinkedHashMap<>();
			debugProperties.put("server", "y");
			debugProperties.put("transport", "dt_socket");
			debugProperties.put("address", "1044");
			debugProperties.put("suspend", "y");
			if (spec.length() > 0 && !spec.equals("true")) {
				for (String prop : spec.split(",")) {
					String propName = prop;
					String propValue = "";
					int idx = propName.indexOf("=");
					if (idx > -1) {
						propValue = propName.substring(idx + 1);
						propName = propName.substring(0, idx);
					}
					debugProperties.put(propName, propValue);
				}
			}
			StringBuilder propStr = new StringBuilder();
			for (Map.Entry<String, String> en : debugProperties.entrySet()) {
				if (propStr.length() > 0)
					propStr.append(",");
				propStr.append(en.getKey());
				propStr.append("=");
				propStr.append(en.getValue());
			}
			command.add(new Argument("-Xrunjdwp:" + propStr.toString()));
			logger.log(Level.WARNING,
					String.format("Remote debugging enabled on port %s", debugProperties.get("address")));
			if ("y".equals(debugProperties.get("suspend"))) {
				logger.log(Level.WARNING, String
						.format("Suspend is enabled, so the application will not start until a debugging connects."));
			}
		}

	}

	/**
	 * Checks if is using client.
	 *
	 * @param path the path
	 * @return true, if is using client
	 */
	private boolean isUsingClient(String path) {
		if (path == null)
			return false;
		for (String p : path.split(File.pathSeparator)) {
			if (p.matches(".*forker-client.*\\.jar"))
				return true;
		}
		return false;
	}

	/**
	 * Checks if is using wrapped.
	 *
	 * @param path the path
	 * @return true, if is using wrapped
	 */
	private boolean isUsingWrapped(String path) {
		if (path == null)
			return false;
		for (String p : path.split(File.pathSeparator)) {
			if (p.matches(".*forker-wrapped.*\\.jar") || p.matches(".*/forker-wrapped/target/classes"))
				return true;
		}
		return false;
	}

	/**
	 * The Class Finder.
	 */
	static class Finder extends SimpleFileVisitor<Path> {

		/** The matcher. */
		private final PathMatcher matcher;
		
		/** The new classpath. */
		private StringBuilder newClasspath;
		
		/** The root. */
		private Path root;

		/**
		 * Instantiates a new finder.
		 *
		 * @param root the root
		 * @param pattern the pattern
		 * @param newClasspath the new classpath
		 */
		Finder(Path root, String pattern, StringBuilder newClasspath) {
			this.root = root;
			this.newClasspath = newClasspath;
			matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern.replace("\\", "\\\\"));
		}

		/**
		 * Find.
		 *
		 * @param file the file
		 */
		void find(Path file) {
			Path name = root.relativize(file);
			if (name != null && matcher.matches(name)) {
				appendPath(newClasspath, file.toFile().getPath());
			}
		}

		/**
		 * Done.
		 */
		void done() {
		}

		/**
		 * Visit file.
		 *
		 * @param file the file
		 * @param attrs the attrs
		 * @return the file visit result
		 */
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
			find(file);
			return FileVisitResult.CONTINUE;
		}

		/**
		 * Pre visit directory.
		 *
		 * @param dir the dir
		 * @param attrs the attrs
		 * @return the file visit result
		 */
		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
			find(dir);
			return FileVisitResult.CONTINUE;
		}

		/**
		 * Visit file failed.
		 *
		 * @param file the file
		 * @param exc the exc
		 * @return the file visit result
		 */
		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) {
			return FileVisitResult.CONTINUE;
		}
	}
}
