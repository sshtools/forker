package com.sshtools.forker.wrapper;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.sshtools.forker.client.ForkerBuilder;

import picocli.CommandLine.Model.CommandSpec;

public interface WrapperPlugin {

	default void init(ForkerWrapper wrapper) throws Exception {
	}

	default void readConfigFile(File file, List<KeyValuePair> properties) throws IOException {
	}

	default boolean event(String name, String cmd, String... args) {
		return false;
	}

	default boolean buildCommand(ForkerBuilder appBuilder) {
		return false;
	}

	default int maybeRestart(int retval, int lastRetVal) {
		return Integer.MIN_VALUE;
	}

	default void addOptions(CommandSpec options) {
	}

	default void beforeProcess() throws IOException {
	}

	default void start() throws IOException {
	}

	default void beforeLaunch() throws IOException {
	}

}
