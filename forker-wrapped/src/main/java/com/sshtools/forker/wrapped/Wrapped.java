package com.sshtools.forker.wrapped;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
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
		try {
			ObjectName objectName = new ObjectName(String.format("%s:type=basic,name=%s",
					WrappedMXBean.class.getPackageName(), WrappedMXBean.class.getSimpleName()));
			MBeanServer server = ManagementFactory.getPlatformMBeanServer();
			server.registerMBean(this, objectName);
		} catch (MalformedObjectNameException | InstanceAlreadyExistsException | MBeanRegistrationException
				| NotCompliantMBeanException e) {
			// handle exceptions
			e.printStackTrace();
		}

		List<String> argList = new ArrayList<String>(Arrays.asList(args));
		if (argList.isEmpty()) {
			throw new IllegalArgumentException("First argument must be the class to actually run.");
		}
		String classname = argList.remove(0);
		Class<?> clazz = Class.forName(classname);
		clazz.getMethod("main", String[].class).invoke(null, new Object[] { argList.toArray(new String[0]) });
	}

	public static void main(String[] args) throws Exception {
		wrapped = true;
		new Wrapped().init(args);
	}

	@Override
	public int launch(String[] xargs) {
		for (int i = launchListeners.size() - 1; i >= 0; i--) {
			int ret = launchListeners.get(i).launch(xargs);
			if (ret != Integer.MIN_VALUE)
				return ret;
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
		for (int i = shutdownListeners.size() - 1; i >= 0; i--) {
			int ret = shutdownListeners.get(i).shutdown();
			if (ret != Integer.MIN_VALUE)
				return ret;
		}
		if(shutdownListeners.size() == 0)
			System.exit(0);
		return 0;
	}

}
