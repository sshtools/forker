package com.sshtools.forker.wrapper;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.cli.Options;

import com.sshtools.forker.client.ForkerBuilder;

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

	default void addOptions(Options options) {
	}

	default void beforeProcess() throws IOException {
	}

	default void start() throws IOException {
	}

}
