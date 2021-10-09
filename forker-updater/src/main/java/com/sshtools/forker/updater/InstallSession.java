package com.sshtools.forker.updater;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InstallSession extends AbstractSession<Updater> {

	private List<Path> files = new ArrayList<>();
	private Path base;
	private long sz = -1;
	
	public InstallSession() {
		super();
	}

	public InstallSession(Path propertiesFile) throws IOException {
		super(propertiesFile);
	}

	public Path base() {
		return base;
	}

	@Override
	public int updates() {
		return files.size();
	}

	public List<Path> files() {
		return Collections.unmodifiableList(files);
	}

	public InstallSession addFile(Path path) {
		files.add(path);
		if (sz > -1) {
			sz = -1;
		}
		return this;
	}

	public InstallSession base(Path base) {
		this.base = base;
		return this;
	}

	public long size() {
		if (sz == -1) {
			sz = 0;
			try {
				for (Path p : files()) {
					sz += Files.size(p);
				}
			} catch (IOException ioe) {
				throw new IllegalStateException("Could not get update size.", ioe);
			}
		}
		return sz;
	}
}
