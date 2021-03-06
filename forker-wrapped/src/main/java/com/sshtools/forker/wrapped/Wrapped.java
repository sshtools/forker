package com.sshtools.forker.wrapped;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.ObjectName;

public class Wrapped implements WrappedMXBean {

	public interface LaunchListener {
		int launch(String[] args);
	}

	public interface ShutdownListener {
		int shutdown();
	}

	private static Wrapped instance;
	private static boolean wrapped;

	private final List<LaunchListener> launchListeners = new ArrayList<>();
	private final List<ShutdownListener> shutdownListeners = new ArrayList<>();
	private long lastPing;
	private Thread monitor;
	private boolean runMonitor = true;
	private Object monitorLock = new Object();
	private Method mainMethod;

	public static Wrapped get() {
		return instance;
	}

	{
		if (instance != null) {
			throw new IllegalStateException(String.format("May only be a single instance of %s", getClass().getName()));
		}
		instance = this;
	}

	public static boolean isWrapped() {
		return wrapped;
	}

	public void addLaunchListener(LaunchListener listener) {
		launchListeners.add(listener);
	}

	public void removeLaunchListener(LaunchListener listener) {
		launchListeners.remove(listener);
	}

	public void addShutdownListener(ShutdownListener listener) {
		shutdownListeners.add(listener);
	}

	public void removeShutdownListener(ShutdownListener listener) {
		shutdownListeners.remove(listener);
	}

	void init(String[] args) throws Exception {
		ObjectName objectName = new ObjectName(String.format("%s:type=basic,name=%s",
				WrappedMXBean.class.getPackageName(), WrappedMXBean.class.getSimpleName()));
		MBeanServer server = ManagementFactory.getPlatformMBeanServer();

		/* Expose the wrapped application */
		server.registerMBean(this, objectName);
		List<String> argList = new ArrayList<String>(Arrays.asList(args));
		if (argList.isEmpty()) {
			throw new IllegalArgumentException("First argument must be the class to actually run.");
		}
		String classname = argList.remove(0);
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		if (loader == null)
			loader = Wrapped.class.getClassLoader();
		Class<?> clazz = loader.loadClass(classname);

		/* Launch */
		mainMethod = clazz.getMethod("main", String[].class);
		mainMethod.invoke(null, new Object[] { argList.toArray(new String[0]) });
	}

	protected void startMonitor() {
		/*
		 * Start a thread. If no ping has been received in the last 5 seconds, then the
		 * wrapper has died and we should shutdown as well.
		 * 
		 * This is a workaround for the fact that if the wrapper is killed without it's
		 * shutdown hooks getting run (e.g. kill -9, or even just running from Eclipse),
		 * then the wrapped application might continue running.
		 */
		monitor = new Thread("PingMonitorThread") {
			long previousPing = 0;

			public void run() {
				try {
					while (runMonitor) {
						Thread.sleep(5000);
						if (previousPing != 0 && lastPing == previousPing) {
							shutdown();
							break;
						}
						previousPing = lastPing;
					}
				} catch (InterruptedException ie) {
					// Stopped monitoring
				}

			}
		};
		monitor.setDaemon(true);
		monitor.start();
	}

	public static void main(String[] args) throws Exception {
		wrapped = true;
		new Wrapped().init(args);
	}

	@Override
	public int launch(String[] xargs) {
		if(launchListeners.size() == 0) {
			try {
				mainMethod.invoke(null, (Object)xargs);
			} catch (Exception e) {
				throw new IllegalStateException("Failed to launch.", e);
			}
		}
		else {
			for (int i = launchListeners.size() - 1; i >= 0; i--) {
				int ret = launchListeners.get(i).launch(xargs);
				if (ret != Integer.MIN_VALUE)
					return ret;
			}
		}
		return 0;
	}

	static List<String> parseQuotedString(String command) {
		List<String> args = new ArrayList<String>();
		boolean escaped = false;
		boolean quoted = false;
		StringBuilder word = new StringBuilder();
		for (int i = 0; i < command.length(); i++) {
			char c = command.charAt(i);
			if (c == '"' && !escaped) {
				if (quoted) {
					quoted = false;
				} else {
					quoted = true;
				}
			} else if (c == '\\' && !escaped) {
				escaped = true;
			} else if (c == ' ' && !escaped && !quoted) {
				if (word.length() > 0) {
					args.add(word.toString());
					word.setLength(0);
					;
				}
			} else {
				word.append(c);
			}
		}
		if (word.length() > 0)
			args.add(word.toString());
		return args;
	}

	@Override
	public int shutdown() {
		synchronized (monitorLock) {
			if (monitor != null) {
				runMonitor = false;
				monitor.interrupt();
			}
		}
		for (int i = shutdownListeners.size() - 1; i >= 0; i--) {
			int ret = shutdownListeners.get(i).shutdown();
			if (ret != Integer.MIN_VALUE)
				return ret;
		}
		if (shutdownListeners.size() == 0)
			System.exit(0);
		return 0;
	}

	@Override
	public void ping() {
		synchronized (monitorLock) {
			if (monitor == null) {
				startMonitor();
			}
			lastPing = System.currentTimeMillis();
		}
	}

}
