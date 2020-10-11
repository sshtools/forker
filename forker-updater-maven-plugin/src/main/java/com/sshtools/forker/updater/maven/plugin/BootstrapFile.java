package com.sshtools.forker.updater.maven.plugin;

import org.apache.maven.plugins.annotations.Parameter;

public final class BootstrapFile {

	@Parameter(property = "source", required = true) String source;

	@Parameter(property = "target", defaultValue = ".", required = true) String target = ".";

}