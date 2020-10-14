package com.sshtools.forker.updater;
 
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.sshtools.forker.updater.AppManifest.Entry;
import com.sshtools.forker.updater.AppManifest.Section;

public class UpdateSession extends AbstractSession {

	private Path localDir = Paths.get(System.getProperty("user.dir"));
	private List<String> appArgs;

	UpdateSession(AppManifest manifiest, Updater updater) {
		super();
		manifest(manifiest);
		updater(updater);
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

	public boolean requiresUpdate() throws IOException {
		return !getUpdates().isEmpty();
	}

	public boolean requiresBootstrapUpdate() throws IOException {
		return !doGetUpdates(manifest().entries(Section.BOOTSTRAP)).isEmpty();
	}

	public boolean requiresAppUpdate() throws IOException {
		return !doGetUpdates(manifest().entries(Section.APP)).isEmpty();
	}

	public Collection<? extends Entry> getUpdates() throws IOException {
		return doGetUpdates(manifest().entries());
	}

	protected Collection<? extends Entry> doGetUpdates(List<Entry> entries) throws IOException {
		List<Entry> updates = new ArrayList<>();
		for (Entry entry : entries) {
			Path local = entry.section() == Section.APP ? entry.resolve(localDir, manifest().path())
					: entry.resolve(localDir);
			boolean update = false;
			if (Files.isSymbolicLink(local)) {
				if (!entry.isLink())
					update = true;
				else if (!Files.readSymbolicLink(local).equals(entry.target()))
					update = true;
			} else if (Files.exists(local)) {
				if (entry.isLink())
					update = true;
				else {
					long localLen = Files.size(local);
					if (localLen != entry.size()) {
						update = true;
					} else {
						if (entry.checksum() != AppManifest.checksum(local)) {
							update = true;
						}
					}
				}
			} else {
				update = true;
			}
			if (update) {
				updates.add(entry);
			}
		}
		return updates;
	}
}
