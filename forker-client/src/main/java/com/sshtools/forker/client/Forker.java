package com.sshtools.forker.client;

import java.io.File;

/**
 * Replacement for {@link Runtime#exec(String)} and friends.
 * 
 * @see ForkerBuilder
 *
 */
public class Forker {
	final static String[] FORKER_DIRS = { ".*[/\\\\]forker-client[/\\\\].*", ".*[/\\\\]forker-wrapper[/\\\\].*",
			".*[/\\\\]forker-pty[/\\\\].*", ".*[/\\\\]forker-common[/\\\\].*", ".*[/\\\\]forker-daemon[/\\\\].*",
			".*[/\\\\]pty4j[/\\\\].*" };
	/*
	 * The jars forker daemon needs. If it's dependencies ever change, this will
	 * have to updated too.
	 * 
	 * TODO Is there a better way to discover this? perhaps looking at maven
	 * meta-data
	 */
	final static String[] FORKER_JARS = { "^jna.*", "^commons-lang3.*", "^commons-io.*", "^jna-platform.*", "^purejavacomm.*",
			"^guava.*", "^log4j.*", "^forker-common.*", "^forker-client.*", "^forker-daemon.*", "^pty4j.*",
			"^forker-wrapper.*", "^forker-pty.*", "^jsr305.*", "^checker-qua.*", "^error_prone_annotations.*",
			"^jobjc-annotations.*", "^animal-sniffer-annotations.*" };
	
	private static String forkerComponentsClasspath;


	/**
	 * Get the classpath to be used to load forker. Attempts will be made to
	 * determine this automatically, but this may not always work, for example
	 * when running inside Maven the classpath must be built from the plugin
	 * dependencies.
	 * 
	 * @return classpath
	 */
	public static String getForkerComponentsClasspath() {
		return forkerComponentsClasspath == null ? System.getProperty("java.class.path") : forkerComponentsClasspath;
	}

	/**
	 * Set the classpath to be used to load forker. Attempts will be made to
	 * determine this automatically, but this may not always work, for example
	 * when running inside Maven the classpath must be built from the plugin
	 * dependencies.
	 * 
	 * @param forkerComponentsClasspath classpath
	 */
	public static void setForkerComponentsClasspath(String forkerComponentsClasspath) {
		Forker.forkerComponentsClasspath = forkerComponentsClasspath;
	}

	/**
	 * Get a cut-down classpath that may be used to launch forker from the
	 * current classpath.
	 * 
	 * @return forker classpath
	 */
	public static String getForkerClasspath() {
		return getForkerClasspath(getForkerComponentsClasspath());
	}

	/**
	 * Get a cut-down classpath that may be used to launch forker given a
	 * complete classpath.
	 * 
	 * @param forkerClasspath complete forker classpath
	 * @return cut down forker classpath
	 */
	public static String getForkerClasspath(String forkerClasspath) {
		StringBuilder cp = new StringBuilder();
		for (String p : forkerClasspath.split(File.pathSeparator)) {
			File f = new File(p);
			if (f.isDirectory()) {
				/*
				 * A directory, so this is likely in dev environment
				 */
				for (String regex : FORKER_DIRS) {
					if (f.getPath().matches(regex)) {
						if (cp.length() > 0)
							cp.append(File.pathSeparator);
						cp.append(p);
					}
				}
			} else {
				for (String regex : FORKER_JARS) {
					if (f.getName().matches(regex)) {
						if (cp.length() > 0)
							cp.append(File.pathSeparator);
						cp.append(p);
						break;
					}
				}
			}
		}
		String classpath = cp.toString();
		return classpath;
	}

	
}
