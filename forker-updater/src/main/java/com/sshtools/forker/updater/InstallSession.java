package com.sshtools.forker.updater;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class InstallSession {

	private List<Path> files = new ArrayList<>();
	private Path base;
	private Updater updater;
	private AppManifest manifest;

	public Updater updater() {
		return updater;
	}

	public AppManifest manifest() {
		return manifest;
	}

	public InstallSession manifest(AppManifest manifest) {
		this.manifest = manifest;
		return this;
	}

	public InstallSession updater(Updater updater) {
		this.updater = updater;
		return this;
	}

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
