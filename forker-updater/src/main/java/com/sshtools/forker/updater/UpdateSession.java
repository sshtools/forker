package com.sshtools.forker.updater;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.sshtools.forker.updater.AppManifest.Section;

public class UpdateSession extends AbstractSession<Updater> {

	private Path localDir = Paths.get(System.getProperty("user.dir"));
	private List<String> appArgs;
	private boolean systemWideBootstrapInstall;
	private long sz = 0;
	private int updates = 0;
	
	public UpdateSession() {
		super();
	}

	public UpdateSession(Path propertiesFile, Updater updater) throws IOException {
		super(propertiesFile);
		tool(updater);
	}

	@Override
	public AbstractSession<Updater> manifest(AppManifest manifest) {
		AbstractSession<Updater> m = super.manifest(manifest);
		sz = 0;
		try {
			Collection<? extends Entry> l = getUpdates();
			updates = l.size();
			for (Entry p : l) {
				sz += p.size();
			}
		} catch (IOException ioe) {
			throw new IllegalStateException("Could not get update size.", ioe);
		}
		return m;
	}

	public boolean systemWideBootstrapInstall() {
		return systemWideBootstrapInstall;
	}

	public UpdateSession systemWideBootstrapInstall(boolean systemWideBootstrapInstall) {
		this.systemWideBootstrapInstall = systemWideBootstrapInstall;
		return this;
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

	@Override
	public long size() {
		return sz;
	}

	protected Collection<? extends Entry> doGetUpdates(List<Entry> entries) throws IOException {
		List<Entry> updates = new ArrayList<>();
		for (Entry entry : entries) {
			Path local = entry.resolve(localDir);

			if (systemWideBootstrapInstall && entry.section() == Section.BOOTSTRAP)
				continue;

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

	@Override
	public int updates() {
		return updates;
	}
}
