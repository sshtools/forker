package com.sshtools.forker.updater;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
 
public class InstallSession extends AbstractSession {

	private List<Path> files = new ArrayList<>();
	private Path base;

	public Path base() {
		return base;
	}

	public List<Path> files() {
		return files;
	}

	public InstallSession base(Path base) {
		this.base = base;
		return this;
	}
}
