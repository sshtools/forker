package com.sshtools.forker.updater;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.logging.Level;

import com.sshtools.forker.common.OS;
import com.sshtools.forker.common.Util;
import com.sshtools.forker.updater.AppManifest.Section;
import com.sshtools.forker.wrapper.ForkerWrapper;
import com.sshtools.forker.wrapper.Replace;
import com.sun.jna.Platform;

import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;

public class Updater extends ForkerWrapper {

	public static void main(String[] args) {
		Updater wrapper = new Updater();
		wrapper.getWrappedApplication().setOriginalArgs(args);
		CommandSpec opts = CommandSpec.create();
		opts.mixinStandardHelpOptions(true);
		wrapper.addOptions(opts);
		opts.addOption(OptionSpec.builder("--update-on-exit").paramLabel("exitCode").type(int.class).description(
				"Update when hosted application returns this exit status. The hosted application can also detect if there is an update by examining the system property forker.updateAvailable or then environment variable FORKER_UPDATE_AVAILABLE.")
				.build());
		opts.addOption(OptionSpec.builder("--local-manifest").paramLabel("file").type(File.class)
				.description("The location of the local manifest.").build());
		opts.addOption(OptionSpec.builder("--offline").description("Do not check for updates at all.").build());
		opts.addOption(OptionSpec.builder("--default-remote-manifest").paramLabel("uri").type(String.class)
				.description("The default location of the remote manifest. Can be override by remote-manifest")
				.build());
		opts.addOption(OptionSpec.builder("--remote-manifest").paramLabel("uri").type(String.class)
				.description("The location of the remote manifest. Overrides default-remote-manifest").build());
		opts.addOption(OptionSpec.builder("--update-exit").paramLabel("exitCode").type(int.class).description(
				"This is the exit code update will exit with if the bootstrap itself needs updating. The files are downloaded to .bootstrap-updates in the cwd. If not present, the default value of '9' will be used.")
				.build());
		opts.addOption(OptionSpec.builder("--install")
				.description("Install the application (useful from self extracting scripts for example).").build());
		opts.addOption(OptionSpec.builder("--install-location").paramLabel("directory").type(File.class)
				.description("The default installation location.").build());
		opts.addOption(OptionSpec.builder("--run-on-install")
				.description("If specified, the installer will launch the application once installed.").build());
		opts.addOption(OptionSpec.builder("--throttle").paramLabel("bytesPerSecondOrTrue").type(String.class)
				.description(
						"If specified, the maximum number of bytes per second that may be read or written. Useful for testing.")
				.build());

		opts.usageMessage().header("Forker Updater", "Provided by JAdpative.");
		opts.usageMessage().description(
				"Based on Forker Wrapper, the Updater can create full installable and updateable working images of applications from minimal configuration.");

		wrapperMain(args, wrapper, opts);
	}

	protected UpdateHandler updateHandler;
	protected InstallHandler installHandler;
	private UpdateSession session;

	protected UpdateHandler getUpdateHandler() {

		if (updateHandler == null) {
			ServiceLoader<UpdateHandler> handlers = ServiceLoader.load(UpdateHandler.class);
			Iterator<UpdateHandler> it = handlers.iterator();
			if (it.hasNext()) {
				updateHandler = it.next();
			} else
				updateHandler = new DefaultConsoleUpdateHandler();
		}
		return updateHandler;
	}

	protected InstallHandler getInstallHandler() {

		if (installHandler == null) {
			ServiceLoader<InstallHandler> handlers = ServiceLoader.load(InstallHandler.class);
			Iterator<InstallHandler> it = handlers.iterator();
			if (it.hasNext()) {
				installHandler = it.next();
			} else
				installHandler = new DefaultConsoleInstallHandler();
		}
		return installHandler;
	}

	@Override
	protected Boolean onMaybeRestart(int retval, int lastRetVal) throws Exception {

		logger.log(Level.FINE, String.format("Deciding if  restart is needed. This exit value is %d", retval));
		String updateOnExitStr = getConfiguration().getOptionValue("update-on-exit", null);
		if (updateOnExitStr == null || session == null) {
			if (updateOnExitStr == null)
				logger.log(Level.FINE,
						String.format("Not update on exit, just continuing with standard exit or restart."));
			else
				logger.log(Level.FINE, String.format(
						"Update on exit set, but there is no update session, just continuing with standard exit or restart."));
			return null;
		} else {
			boolean doUpdate = Integer.parseInt(updateOnExitStr) == retval;
			if (doUpdate) {
				logger.log(Level.FINE, String.format("Updating because exit value %d is %s.", retval, updateOnExitStr));
				update(() -> {
					return null;
				}, session);
				getUpdateHandler().complete();
				logger.log(Level.FINE, String.format("Now forcing restart because update is done."));
				return true;
			}
			logger.log(Level.FINE,
					String.format("Not updating because exit value %d is not %s.", retval, updateOnExitStr));
			return null;
		}
	}

	protected Path cwd() {
		return resolveCwd().toPath();
	}

	protected boolean isInstaller() {
		if (getConfiguration().getSwitch("install", false)) {
			return true;
		}
		File tmp = new File(System.getProperty("java.io.tmpdir"));
		try {
			if (cwd().toFile().getCanonicalPath().startsWith(tmp.getCanonicalPath())) {
				return true;
			}
		} catch (IOException ioe) {
		}

		String dirname = cwd().getFileName().toString();
		return dirname.equalsIgnoreCase("tmp") || dirname.equalsIgnoreCase("temp") || dirname.startsWith(".")
				|| dirname.equalsIgnoreCase("downloads");
	}

	protected boolean manifest(Callable<Void> task, AppManifest manifest, UpdateHandler handler, UpdateSession session,
			URL url, Reader in) throws IOException, Exception {
		session.manifest(manifest);
		if (manifest.getProperties().containsKey("main")) {
			getConfiguration().setProperty("main", manifest.getProperties().get("main"));
		}

		if (session.requiresUpdate()) {
			boolean updateOnExit = getConfiguration().getOptionValue("update-on-exit", null) != null;
			if (updateOnExit) {
				this.session = session;
				getConfiguration().addProperty("jvmarg", "-Dforker.updateAvailable=true");
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
		try {
			if (isInstaller()) {
				return install(task);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalStateException("Failed to install. Cannot continue.", e);
		}

		return update(task);
	}

	protected boolean update(Callable<Void> task) {
		Path userCwd = null;
		if (!Files.isWritable(cwd())) {
			userCwd = new File(System.getProperty("user.home") + File.separator + ".cache" + File.separator + "snake"
					+ File.separator + "app").toPath();
			try {
				logger.log(Level.FINE, String.format(
						"Current directory %s is unwriteable, so treating this is a system wide install and switching the actual installation directory to %s.",
						cwd(), userCwd));
				Files.createDirectories(userCwd);

				/*
				 * TODO: really we should copy the app system wide files to the cache rather
				 * than downloading them again. This will mean doing something less brutal than
				 * changing the CWD
				 */
				getConfiguration().setProperty("cwd", userCwd.toString());
			} catch (IOException ioe) {
				throw new IllegalStateException(String.format("Failed to create user cache directory %s", userCwd));
			}
		}

		String remoteManifestLocation = getConfiguration().getOptionValue("remote-manifest", null);
		if (remoteManifestLocation == null || remoteManifestLocation.equals("")) {
			remoteManifestLocation = getConfiguration().getOptionValue("default-remote-manifest", null);
			if (remoteManifestLocation == null || remoteManifestLocation.equals("")) {
				throw new IllegalStateException(
						"A remote-manifest option must be provided, with a URL pointing to remote XML manifest.");
			}
		}

		String localManifestLocation = getConfiguration().getOptionValue("local-manifest", null);
		if (localManifestLocation == null || remoteManifestLocation.equals(""))
			throw new IllegalStateException(
					"A local-manifest option must be provided, with a URL pointing to local XML manifest.");
		Path localManifestPath = cwd().resolve(localManifestLocation);

		/* Try get the remote manifest first */
		boolean continueProcessing = true;
		try {
			AppManifest manifest = new AppManifest();
			UpdateHandler handler = getUpdateHandler();
			UpdateSession session = new UpdateSession(this);
			if (userCwd != null)
				session.systemWideBootstrapInstall(true);

			session.localDir(cwd());
			session.appArgs(getConfiguration().getRemaining());
			handler.init(session);

			/*
			 * Load the local manifest well to get the current version first.
			 */
			URL url = localManifestPath.toUri().toURL();
			logger.log(Level.FINE, String.format("Look for local manifest from %s.", localManifestPath));
			AppManifest localManifest = null;
			handler.startingManifestLoad(url);
			try (Reader localIn = Files.newBufferedReader(localManifestPath)) {
				localManifest = new AppManifest();
				localManifest.load(localIn);
				if (localManifest.hasVersion()) {
					getConfiguration().addProperty("jvmarg", "-Dforker.installedVersion=" + localManifest.version());
					logger.log(Level.FINE, String.format("Version %s (timestamp %s) is installed for app %s.",
							localManifest.version(), localManifest.timestamp(), localManifest.id()));
				}
			} catch (IOException ioe) {
				logger.log(Level.FINE, String.format("No local manifest at %s.", localManifestPath));
			}
			handler.completedManifestLoad(url);

			url = new URL(remoteManifestLocation);
			logger.log(Level.FINE, String.format("Get remote manifest from %s.", remoteManifestLocation));
			handler.startingManifestLoad(url);
			boolean haveRemote = false;
			if (getConfiguration().getSwitch("offline", false)) {
				url = localManifestPath.toUri().toURL();
				try (Reader in = Files.newBufferedReader(localManifestPath)) {
					continueProcessing = manifest(task, localManifest, handler, session, url, in);
				}
			} else {
				try (Reader in = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8)) {
					manifest.load(in);
					haveRemote = true;
					if (manifest.hasVersion()) {
						logger.log(Level.FINE, String.format("Version %s (timestamp %s) is available for app %s.",
								manifest.version(), manifest.timestamp(), manifest.id()));
						getConfiguration().addProperty("jvmarg", "-Dforker.availableVersion=" + manifest.version());
					}
					continueProcessing = manifest(task, manifest, handler, session, url, in);

				} catch (IOException e) {
					// Could not load remote config, falling back to local.
					logger.log(Level.FINE, String.format(
							"Failed to get remote manifest, falling back to local from %s.", localManifestPath), e);

					url = localManifestPath.toUri().toURL();
					try (Reader in = Files.newBufferedReader(localManifestPath)) {
						continueProcessing = manifest(task, localManifest, handler, session, url, in);
					}
				}
			}
			handler.completedManifestLoad(url);

			/* Now update the local manifest */
			if (haveRemote) {
				logger.log(Level.FINE, String.format("Saving new local manifest to %s with version %s.",
						localManifestPath, manifest.version()));
				try (OutputStream localManifestOut = Files.newOutputStream(localManifestPath)) {
					manifest.save(localManifestOut);
				}
			}

			getUpdateHandler().complete();

		} catch (Exception e) {
			e.printStackTrace();
			getUpdateHandler().failed(e);
			continueProcessing = false;
		} finally {
		}
		return continueProcessing;
	}

	protected boolean install(Callable<Void> task) throws Exception {
		InstallHandler handler = getInstallHandler();
		InstallSession session = new InstallSession();
		Files.walk(cwd()).forEach(s -> {
			if (Files.isRegularFile(s))
				session.addFile(s);
		});
		String installLocation = getConfiguration().getOptionValue("install-location", null);
		if (installLocation == null || installLocation.equals("")) {
			throw new IllegalStateException("install-location must be provided.");
		}
		Map<String, String> props = new HashMap<>();
		if (OS.isAdministrator()) {
			if (Platform.isWindows()) {
				props.put("installer.home", "C:/Program Files");
			} else {
				props.put("installer.home", "/opt");
			}
		} else
			props.put("installer.home", System.getProperty("user.home"));
		installLocation = Replace.replaceProperties(installLocation, props, true);
		session.updater(this);
		session.manifest(new AppManifest(cwd().resolve("manifest.xml")));
		session.base(Paths.get(installLocation));
		handler.init(session);
		Path destination = handler.chooseDestination(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				installTo(session, handler.chosenDestination());

				/* Continue starting up the next bootphase, the updater */
				if (update(task)) {

					if (!getConfiguration().getSwitch("run-on-install", false))
						return null;
					else
						task.call();
				}

				return null;
			}

		});
		if (destination == null)
			/*
			 * No destination means the install handler will prompt soon in its own way,
			 * call the callback when it has the destination folder
			 */
			return false;
		installTo(session, destination);
		boolean ret = update(task);
		if (!getConfiguration().getSwitch("run-on-install", false))
			return true;
		return ret;
	}

	protected void installTo(InstallSession session, Path dest) throws Exception {

		logger.log(Level.FINE, String.format("Installing to %s.", dest));

		InstallHandler handler = getInstallHandler();
		handler.startInstall();
		long grandTotal = session.size();
		try {
			long readTotal = 0;
			int file = 0;
			for (Path s : session.files()) {
				Path d = checkFilesDir(dest.resolve(cwd().relativize(s)));
				logger.log(Level.FINE, String.format("Installing %s to %s.", s, d));
				handler.installFile(s, d, file);
				Files.deleteIfExists(d);
				if (Files.isSymbolicLink(s)) {
					logger.log(Level.FINE, String.format("Creating link %s.", s));
					Files.createSymbolicLink(d, Files.readSymbolicLink(s));
				} else {
					long fileSize = Files.size(s);
					logger.log(Level.FINE, String.format("Creating %s.", s));
					try (InputStream in = throttleStream(Files.newInputStream(s))) {
						try (OutputStream out = throttleStream(Files.newOutputStream(d))) {
							byte[] buf = new byte[65536];
							int r;
							long total = 0;
							while ((r = in.read(buf)) != -1) {
								out.write(buf, 0, r);
								total += r;
								readTotal += r;
								handler.installFileProgress(s, (float) ((double) total / (double) fileSize));
								handler.installProgress((float) ((double) readTotal / (double) grandTotal));
							}
						}
					}
					Files.setPosixFilePermissions(d, Files.getPosixFilePermissions(s));
				}
				handler.installFileDone(s);
				file++;
			}
			handler.installDone();

			logger.log(Level.FINE, String.format("Installation complete."));
			handler.complete();

			getConfiguration().removeProperty("jvmarg", "-Dforker\\.installedVersion=.*");
			getConfiguration().addProperty("jvmarg", "-Dforker.installedVersion=" + session.manifest().version());
			getConfiguration().setProperty("cwd", dest.toString());
		} catch (Exception e) {
			handler.failed(e);
			throw e;
		}
	}

	protected InputStream throttleStream(InputStream in) throws IOException {
		long throttle = getThrottle();
		;
		if (throttle > 0) {
			try {
				return new ThrottledInputStream(in, throttle);
			} catch (NumberFormatException nfe) {
				return new ThrottledInputStream(in, 65536);
			}
		}
		return in;
	}

	protected long getThrottle() {
		String tv = getConfiguration().getOptionValue("throttle", "");
		return tv.equals("") ? 0 : (tv.equals("true") ? 10240 : Long.parseLong(tv));
	}

	protected OutputStream throttleStream(OutputStream out) throws IOException {
		long throttle = getThrottle();
		if (throttle > 0) {
			try {
				return new ThrottledOutputStream(out, throttle);
			} catch (NumberFormatException nfe) {
				return new ThrottledOutputStream(out, 65536);
			}
		}
		return out;
	}

	protected boolean update(Callable<Void> task, UpdateSession session) throws Exception {
		logger.log(Level.INFO, String.format("Updating app %s in %s.", session.manifest().id(), session.localDir()));
		UpdateHandler handler = getUpdateHandler();
		Collection<? extends Entry> updates = session.getUpdates();
		boolean requiredBootstrapUpdate = session.requiresBootstrapUpdate();
		handler.startDownloads();
		long readTotal = 0;
		Path tempBootstrapPath = null;
		boolean upgradeError = false;
		int idx = 0;
		for (Entry entry : updates) {
			handler.startDownloadFile(entry, idx++);
			Path resolvedPath = entry.resolve(session.localDir()); /*.relativize(Paths.get("/"));*/
			logger.log(Level.INFO, String.format("Installing %s to %s", entry.path(), resolvedPath));
			Path path;
			Path outPath = null;
			if (entry.section() == Section.APP) {
				path = resolvedPath;
				if (entry.isLink())
					outPath = path;
				else
					outPath = checkFilesDir(path.getParent().resolve("." + path.getFileName().toString() + ".tmp"));
			} else {
				if (tempBootstrapPath == null) {
					tempBootstrapPath = checkDir(session.localDir().resolve(".updates"));
				}
				path = entry.resolve(tempBootstrapPath);
				outPath = path;
			}
			checkPathBounds(path);
			checkPathBounds(outPath);
			try {
				if (entry.uri() == null) {
					Path target = entry.target();
					if (target == null)
						throw new IOException("Found an entry that has no uri, and it is not a symbolic link.");

					if (tempBootstrapPath != null) {
						/*
						 * If we are in the temporary bootstrap, then we must test for the existence of
						 * the link cdc jdk
						 */
					}

					Files.deleteIfExists(outPath);

					logger.log(Level.INFO, String.format("Creating link file %s targeting  %s from %s in %s section.",
							outPath, target, entry.path(), entry.section()));
					Files.createSymbolicLink(checkFilesDir(outPath), target);

				} else {
					URL url = AppManifest.concat(session.manifest().baseUri(), entry.uri()).toURL();
					logger.log(Level.INFO, String.format("Updating file %s from %s in %s section.", entry.path(), url,
							entry.section()));
					
					if(logger.isLoggable(Level.FINE)) {
						logger.log(Level.FINE, String.format("Writing to temporary file %s", outPath));
					}

					try (InputStream in = throttleStream(url.openStream())) {
						try (OutputStream out = throttleStream(Files.newOutputStream(checkFilesDir(outPath)))) {
							byte[] buf = new byte[65536];
							int r;
							long total = 0;
							while ((r = in.read(buf)) != -1) {
								out.write(buf, 0, r);
								total += r;
								readTotal += r;
								handler.updateDownloadFileProgress(entry,
										(float) ((double) total / (double) entry.size()));
								handler.updateDownloadProgress((float) ((double) readTotal / (double) session.size()));
							}
						}
					}

					if (!outPath.equals(path)) {
						if (Files.exists(path))
							logger.log(Level.INFO, String.format("Removing existing file %s", path));
						Files.deleteIfExists(path);
						Files.move(outPath, path, StandardCopyOption.ATOMIC_MOVE);
					}

					if (entry.permissions() != null) {
						if(logger.isLoggable(Level.FINE)) {
							logger.log(Level.FINE, String.format("Setting permissions %s on %s", entry.permissions(), path));
						}
						Files.setPosixFilePermissions(path, entry.permissions());
					} else {
						if (!entry.write()) {
							if(logger.isLoggable(Level.FINE)) {
								logger.log(Level.FINE, String.format("Setting read-only on %s", path));
							}
							path.toFile().setWritable(false, false);
						}
						if(!entry.read()) {
							if(logger.isLoggable(Level.FINE)) {
								logger.log(Level.FINE, String.format("Setting no-read on %s", path));
							}
							path.toFile().setReadable(false);
						}
						if(entry.execute()) {
							if(logger.isLoggable(Level.FINE)) {
								logger.log(Level.FINE, String.format("Setting execute on %s", path));
							}
							path.toFile().setExecutable(true);
						}
					}

					entry.checksum(AppManifest.checksum(path));
					entry.size(Files.size(path));
				}
			} finally {
				handler.doneDownloadFile(entry);
			}
		}

		handler.updateDone(upgradeError);
		if (upgradeError) {
			throw new IOException("This upgrade must be manually restarted to continue.");
		}

		/*
		 * If any bootstrap files were updated, signal to the script that calls the
		 * bootstrap that it should update from .updates and restart.
		 */
		if (requiredBootstrapUpdate) {
			int ret = Integer.parseInt(getConfiguration().getOptionValue("update-exit", "9"));
			logger.log(Level.INFO,
					String.format("Updates, so requesting a restart using exit code %d before proceeding.", ret));
			System.exit(ret);
		}

		/*
		 * Now check everything else in the base path (i.e. where app jars are) for
		 * anything that shouldn't be there.
		 */
		for(Path appBase : session.manifest().sectionPaths().values())  {
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(appBase)) {
				for (Path path : stream) {
					if (Files.isDirectory(path) && !appBase.equals(session.localDir())) {
						/*
						 * Unless it's the root of the install, the appBase should have no folders in it
						 */
						logger.log(Level.INFO,
								String.format("Removing directory with no manifest entry %s from app base path %s.", path,
										appBase));
						Util.deleteRecursiveIfExists(path.toFile());
					} else if (!Files.isDirectory(path) && !session.manifest().hasPath(path.getFileName())) {
						logger.log(Level.INFO,
								String.format("Removing file with no manifiest entry %s from app base path %s.", path,
										appBase));
						Files.delete(path);
					}
				}
			}
		}

		logger.log(Level.FINE, String.format("Updates complete."));
		getConfiguration().addProperty("jvmarg", "-Dforker.updated=true");
		getConfiguration().removeProperty("jvmarg", "-Dforker\\.installedVersion=.*");
		getConfiguration().removeProperty("jvmarg", "-Dforker\\.updateAvailable=.*");
		logger.log(Level.FINE, String.format("Adding new version %s", session.manifest().version()));
		getConfiguration().addProperty("jvmarg", "-Dforker.installedVersion=" + session.manifest().version());
		return handler.updatesComplete(task);
	}

	private void checkPathBounds(Path path) {
		Path parent = cwd().toAbsolutePath();
		if (!path.startsWith(parent))
			throw new IllegalArgumentException(
					String.format("Attempt to install file %s out of bounds of %s.", path, cwd()));
	}

	private Path checkFilesDir(Path file) throws IOException {
		checkDir(file.getParent());
		return file;
	}

	private Path checkDir(Path dir) throws IOException {
		if (!Files.exists(dir)) {
			logger.log(Level.INFO, String.format("Creating folder %s.", dir));
			Files.createDirectories(dir);
		}
		return dir;
	}
}
