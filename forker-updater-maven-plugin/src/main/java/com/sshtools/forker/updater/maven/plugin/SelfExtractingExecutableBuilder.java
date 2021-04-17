package com.sshtools.forker.updater.maven.plugin;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.lang3.SystemUtils;

public class SelfExtractingExecutableBuilder {

	private Path output;
	private Path script;
	private Path image;

	public Path output() {
		return output;
	}

	public SelfExtractingExecutableBuilder output(Path output) {
		this.output = output;
		return this;
	}

	public Path script() {
		return script;
	}

	public SelfExtractingExecutableBuilder script(Path script) {
		this.script = script;
		return this;
	}

	public Path image() {
		return image;
	}

	public SelfExtractingExecutableBuilder image(Path image) {
		this.image = image;
		return this;
	}

	public void make() throws IOException {
		if (SystemUtils.IS_OS_WINDOWS) {

		}
		throw new UnsupportedOperationException();
	}
}
