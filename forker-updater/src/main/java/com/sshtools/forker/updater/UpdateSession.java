package com.sshtools.forker.updater;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.sshtools.forker.updater.AppManifest.Entry;

public class UpdateSession {

	private AppManifest manifest;
	private List<Entry> updates;
	private Updater updater;
	private Path localDir = Paths.get(System.getProperty("user.dir"));
	private List<String> appArgs;

	UpdateSession(AppManifest manifiest, Updater updater) {
		super();
		this.manifest = manifiest;
		this.updater = updater;
	}

	public List<String> appArgs() {
		return appArgs;
	}

	public UpdateSession appArgs(List<String> appArgs) {
		this.appArgs = appArgs;
		return this;
	}

	public Path localDir() {
		return localDir;
	}

	public UpdateSession localDir(Path localDir) {
		this.localDir = localDir;
		return this;
	}

	public AppManifest manifest() {
		return manifest;
	}

	public Updater updater() {
		return updater;
	}

	public boolean requiresUpdate() throws IOException {
		return !getUpdates().isEmpty();
	}

	public Collection<? extends Entry> getUpdates() throws IOException {
		if (updates == null) {
			updates = new ArrayList<>();
			for (Entry entry : manifest.entries()) {
				Path local = localDir.resolve(entry.path());
				boolean update = false;
				if (Files.exists(local)) {
					long localLen = Files.size(local);
					if (localLen != entry.size())
						update = true;
					else {
						if (entry.checksum() != AppManifest.checksum(local))
							update = true;
					}
				} else
					update = true;
				if (update) {
					updates.add(entry);
				}
			}
		}
		return updates;
	}
}
