package com.sshtools.forker.wrapper;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;

import org.apache.commons.lang3.StringUtils;

public class WrappedApplication {

	private String classname;
	private String module;
	private String[] originalArgs;
	private String[] arguments;

	public void set(String main, String jar) throws IOException {
		set(main, jar, null, null, null);
	}

	public void set(String main, String jar, List<String> remainingArgs, List<String> appargs, ArgMode argMode)
			throws IOException {
		if (main == null) {
			if (jar == null) {
				if (remainingArgs != null) {
					if (remainingArgs.isEmpty())
						throw new IllegalArgumentException(
								"Must supply class name of application that contains a main() method.");
					classname = remainingArgs.remove(0);
				}
			} else {
				readMain(jar);
			}
		} else {
			int idx = main.indexOf('/');
			if (idx != -1) {
				classname = main.substring(idx + 1);
				module = main.substring(0, idx);
			} else
				classname = main;
		}
		if (remainingArgs != null && appargs != null && argMode != null) {
			if (argMode != ArgMode.FORCE) {
				if (!remainingArgs.isEmpty()) {
					switch (argMode) {
					case APPEND:
						appargs.addAll(0, remainingArgs);
						break;
					case PREPEND:
						appargs.addAll(remainingArgs);
						break;
					default:
						appargs = remainingArgs;
						break;
					}
				}
			}
			this.arguments = appargs.toArray(new String[0]);
		}
	}

	public String[] getOriginalArgs() {
		return originalArgs;
	}

	public void setOriginalArgs(String[] originalArgs) {
		this.originalArgs = originalArgs;
	}

	public void setClassname(String classname) {
		this.classname = classname;
	}

	public void setModule(String module) {
		this.module = module;
	}

	public String getClassname() {
		return classname;
	}

	public String getModule() {
		return module;
	}

	private String readMain(String jar) throws IOException {
		try (JarFile jf = new JarFile(new File(jar))) {
			String classname = jf.getManifest().getMainAttributes().getValue("Main-Class");
			if (StringUtils.isBlank(classname)) {
				throw new IOException(String.format(
						"An executable jar (--jar %s) was provided, but it does not contain a Main-Class attribute in META-INF/MANIFEST.MF",
						jar));
			}
			return classname;
		}
	}

	public boolean hasClassname() {
		return classname != null && classname.length() > 0;
	}

	public boolean hasArguments() {
		return arguments != null && arguments.length > 0;
	}

	public String fullClassAndModule() {
		return StringUtils.isBlank(module) ? classname : module + "/" + classname;
	}

	public String[] getArguments() {
		return arguments;
	}

	public List<String> getArgumentsList() {
		return hasArguments() ? Arrays.asList(arguments) : Collections.emptyList();
	}
}
