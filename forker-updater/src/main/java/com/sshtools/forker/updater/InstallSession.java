package com.sshtools.forker.updater;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InstallSession extends AbstractSession {

	private List<Path> files = new ArrayList<>();
	private Path base;
	private long sz = -1;

	public Path base() {
		return base;
	}

	public List<Path> files() {
		return Collections.unmodifiableList(files);
	}
	
	public InstallSession addFile(Path path) {
		files.add(path);
		if(sz > -1) {
			sz = -1;
		}
		return this;
	}

	public InstallSession base(Path base) {
		this.base = base;
		return this;
	}

	public long size() throws IOException {
		if (sz == -1) {
			sz = 0;
			for (Path p : files()) {
				sz += Files.size(p);
			}
		}
		return sz;
	}
}
