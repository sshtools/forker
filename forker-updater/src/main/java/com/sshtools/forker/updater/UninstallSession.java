package com.sshtools.forker.updater;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class UninstallSession extends AbstractSession<Uninstaller> {

	private Set<Path> files = new LinkedHashSet<>();
	private Path base;
	private long sz = -1;

	public UninstallSession() throws IOException {
		super();
	}

	public UninstallSession(Path propertiesFile) throws IOException {
		super(propertiesFile);
	}

	public Path base() {
		return base;
	}

	@Override
	public int updates() {
		return files.size();
	}

	public Set<Path> files() {
		return Collections.unmodifiableSet(files);
	}

	public UninstallSession addDirectory(Path path) {
		if(Files.exists(path)) {
			try {
				Files.walk(path).forEach(s -> {
					if (Files.isDirectory(s))
						addFile(s);
				});
			}
			catch(IOException ioe) {
				throw new IllegalStateException(String.format("Failed to add directory %s to uninstall list."));
			}
		}
		return this;
	}

	public UninstallSession addFile(Path path) {
		files.add(path);
		if (sz > -1) {
			sz = -1;
		}
		return this;
	}

	public UninstallSession base(Path base) {
		this.base = base;
		return this;
	}

	@Override
	public long size() {
		return updates();
	}
}
