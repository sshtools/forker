package com.sshtools.forker.updater;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.sshtools.forker.updater.AppManifest.Entry;
import com.sshtools.forker.wrapper.ForkerWrapper;

public class Updater extends ForkerWrapper {

	public static void main(String[] args) {
		Updater wrapper = new Updater();
		wrapper.setOriginalArgs(args);
		Options opts = new Options();
		wrapper.addOptions(opts);
		opts.addOption(new Option(null, "update-on-exit", true,
				"Update when hosted application returns this exit status. The hosted application can also detect if there is an update by examining the system property forker.updateAvailable or then environment variable FORKER_UPDATE_AVAILABLE."));
		opts.addOption(new Option(null, "local-manifest", true, "The location of the local manifest."));
		opts.addOption(new Option(null, "remote-manifest", true, "The location of the remote manifest."));

		wrapperMain(args, wrapper, opts);
	}

	protected UpdateHandler updateHandler;
	private UpdateSession session;

	protected UpdateHandler getUpdateHandler() {

		if (updateHandler == null) {
			ServiceLoader<UpdateHandler> handlers = ServiceLoader.load(UpdateHandler.class);
			Iterator<UpdateHandler> it = handlers.iterator();
			if (it.hasNext()) {
				updateHandler = it.next();
			} else
				updateHandler = new ConsoleUpdateHandler();
		}
		return updateHandler;
	}

	@Override
	protected Boolean onMaybeRestart(int retval, int lastRetVal) throws Exception {
		String updateOnExitStr = getOptionValue("update-on-exit-value", null);
		if(updateOnExitStr == null)
			return null;
		else {
			update(() -> {
				return null;
			}, session);
			return Integer.parseInt(updateOnExitStr) == retval;
		}
	}

	protected boolean manifest(Callable<Void> task, AppManifest manifest, UpdateHandler handler, UpdateSession session,
			URL url, Reader in) throws IOException, Exception {
		manifest.load(in);

		handler.completedManifestLoad(url);

		if (manifest.getProperties().containsKey("main")) {
			setProperty("main", manifest.getProperties().get("main"));
		}

		if (session.requiresUpdate()) {
			boolean updateOnExit = getOptionValue("update-on-exit-value", null) != null;
			if (updateOnExit) {
				this.session = session;
				addProperty("jvmarg", "-Dforker.updateAvailable=true");
				task.call();
				return true;
			}
			return update(task, session);
		} else {
			return noUpdates(task, session);
		}
	}

	protected boolean noUpdates(Callable<Void> task, UpdateSession session) {
		return getUpdateHandler().noUpdates(task);
	}

	@Override
	protected boolean onBeforeProcess(Callable<Void> task) {

		String remoteManifestLocation = getOptionValue("remote-manifest", null);
		if (remoteManifestLocation == null || remoteManifestLocation.equals(""))
			throw new IllegalStateException(
					"A remote-manifest option must be provided, with a URL pointing to remote XML manifest.");

		String localManifestLocation = getOptionValue("local-manifest", null);
		if (localManifestLocation == null || remoteManifestLocation.equals(""))
			throw new IllegalStateException(
					"A local-manifest option must be provided, with a URL pointing to local XML manifest.");
		Path localManifestPath = Paths.get(localManifestLocation);

		/* Try get the remote manifest first */
		boolean continueProcessing = true;
		try {
			AppManifest manifest = new AppManifest();
			UpdateHandler handler = getUpdateHandler();
			UpdateSession session = new UpdateSession(manifest, this);
			session.appArgs(getCmd().getArgList());
			handler.init(session);

			URL url = new URL(remoteManifestLocation);
			handler.startingManifestLoad(url);
			try (Reader in = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8)) {
				continueProcessing = manifest(task, manifest, handler, session, url, in);
			} catch (IOException e) {
				e.printStackTrace();
				// Could not load remote config, falling back to local.

				url = localManifestPath.toUri().toURL();
				handler.startingManifestLoad(url);
				try (Reader in = Files.newBufferedReader(localManifestPath)) {
					continueProcessing = manifest(task, manifest, handler, session, url, in);
				}
			}

			/* Now update the local manifest */
			try (OutputStream localManifestOut = Files.newOutputStream(localManifestPath)) {
				manifest.save(localManifestOut);
			}
		} catch (Exception e) {
			e.printStackTrace();
			getUpdateHandler().failed(e);
		} finally {
			getUpdateHandler().complete();
		}

		return continueProcessing;
	}

	protected boolean update(Callable<Void> task, UpdateSession session) throws Exception {
		UpdateHandler handler = getUpdateHandler();
		Collection<? extends Entry> updates = session.getUpdates();
		handler.startDownloads();
		long grandTotal = 0;
		for (Entry entry : updates) {
			grandTotal += entry.size();
		}
		long readTotal = 0;
		for (Entry entry : updates) {
			handler.startDownloadFile(entry);
			Path path = session.localDir().resolve(entry.path());
			Path tmpPath = checkFilesDir(path.getParent().resolve("." + path.getFileName().toString() + ".tmp"));
			try {
				URL url = AppManifest.concat(session.manifest().baseUri(), entry.uri()).toURL();
				try (InputStream in = url.openStream()) {
					try (OutputStream out = Files.newOutputStream(tmpPath)) {
						byte[] buf = new byte[65536];
						int r;
						long total = 0;
						while ((r = in.read(buf)) != -1) {
							out.write(buf, 0, r);
							total += r;
							readTotal += r;
							handler.updateDownloadFileProgress(entry, (float) ((double) total / (double) entry.size()));
							handler.updateDownloadProgress((float) ((double) readTotal / (double) grandTotal));
						}
					}
				}
				Files.move(tmpPath, path, StandardCopyOption.ATOMIC_MOVE);
				entry.checksum(AppManifest.checksum(path));
				entry.size(Files.size(path));
			} finally {
				handler.doneDownloadFile(entry);
			}
		}
		return handler.updatesComplete(task);
	}

	private Path checkFilesDir(Path file) throws IOException {
		checkDir(file.getParent());
		return file;
	}

	private Path checkDir(Path dir) throws IOException {
		if (!Files.exists(dir))
			Files.createDirectories(dir);
		return dir;
	}
}
