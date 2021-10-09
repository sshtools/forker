package com.sshtools.forker.updater;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sshtools.forker.wrapper.AbstractWrapper;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "uninstaller", mixinStandardHelpOptions = true, description = "Uninstalls this application.")
public class Uninstaller extends AbstractWrapper implements Callable<Integer> {

	/** The logger. */
	protected Logger logger = Logger.getGlobal();

	@Option(names = { "--local-manifest" }, description = "Location of location manifest if not default.")
	private String localManifest;

	@Option(names = { "--level" }, description = "Log level.")
	private String level = "WARNING";

	protected UninstallHandler uninstallHandler;

	@Override
	public Integer call() throws Exception {
		reconfigureLogging(level);

		/*
		 * This is an attempt to prevent errors when deleting a JVM while its running.
		 * It may not be enough, more classes might get loaded. Or this might just be a
		 * bad idea in general :)
		 */
		TimeZone.getDefault();

		/* Start actual uninstaller */
		UninstallHandler handler = getUninstallHandler();
		UninstallSession session = new UninstallSession(cwd().resolve("uninstaller.properties"));
		session.tool(this);
		session.manifest(new AppManifest(cwd().resolve("manifest.xml")));
		session.base(cwd());

		handler.init(session);

		Boolean deleteAll = handler.prep(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				uninstallFrom(session, handler.value(), cwd());
				return null;
			}

		});
		if (deleteAll == null)
			/*
			 * Means the uninstall handler will prompt soon in its own way, call the
			 * callback when it has the destination folder
			 */
			return Integer.MIN_VALUE;

		uninstallFrom(session, deleteAll, cwd());

		return 0;
	}

	protected UninstallHandler getUninstallHandler() {

		if (uninstallHandler == null) {
			ServiceLoader<UninstallHandler> handlers = ServiceLoader.load(UninstallHandler.class);
			Iterator<UninstallHandler> it = handlers.iterator();
			if (it.hasNext()) {
				uninstallHandler = it.next();
			} else
				uninstallHandler = new DefaultConsoleUninstallHandler();
		}
		return uninstallHandler;
	}

	protected Path cwd() {
		return Paths.get(System.getProperty("user.dir"));
	}

	protected void uninstallFrom(UninstallSession session, boolean deleteAll, Path dest) throws Exception {
		
		logger.log(Level.FINE,
				String.format("Uninstalling from %s (%s).", cwd(), deleteAll ? "delete all" : "delete manifest files"));

		if (deleteAll) {
			Files.walk(cwd()).forEach(s -> {
				if (Files.isRegularFile(s) || Files.isSymbolicLink(s))
					session.addFile(s);
			});
			session.addDirectory(cwd());
		} else {
			for (Entry entry : session.manifest().entries()) {
				session.addFile(entry.resolve(cwd()));
			}
			session.addFile(cwd().resolve("manifest.xml"));
			session.addFile(cwd().resolve(session.manifest().id() + ".args"));
			session.addFile(cwd());
		}

		UninstallHandler handler = getUninstallHandler();
		handler.startUninstall();
		try {
			int file = 0;
			Set<Path> files = session.files();
			List<Exception> exceptions = new ArrayList<>();
			for (Path s : files) {
				Path d = checkFilesDir(dest.resolve(cwd().relativize(s)));
				logger.log(Level.FINE, String.format("Uninstalling %s from %s.", s, d));
				handler.uninstallFile(s, d, file);
				
				try {
					if(!Files.isDirectory(d) || d.toFile().list().length == 0)
						Files.deleteIfExists(d);
				}
				catch(IOException ioe) {
					if(logger.isLoggable(Level.FINE))
						logger.log(Level.WARNING, String.format("Failed to remove %s.", d), ioe);
					else
						logger.log(Level.WARNING, String.format("Failed to remove %s.", d));
					exceptions.add(new NotFatalException(ioe.getMessage()));
				}

				/*
				 * Check if any directory up to the installer directory is now empty, if so it
				 * can be removed
				 */
				d = d.getParent();
				while (d != null && !d.equals(dest)) {
					String[] list = d.toFile().list();
					if (list != null && list.length == 0) {
						logger.log(Level.FINE, String.format("Removing empty directory %s.", s, d));
						Files.delete(d);
					}
					d = d.getParent();
				}

				handler.uninstallProgress((float) ((double) file / (double) files.size()));
				handler.uninstallFileDone(s);
				file++;
			}
			if(exceptions.isEmpty()) {
				handler.uninstallDone();
	
				logger.log(Level.FINE, String.format("Installation complete."));
				handler.complete();
			}
			else if(exceptions.size() == 1) {
				throw exceptions.get(0);
			} else {
				throw new NotFatalException(String.format("%d files or directories could not be removed.", exceptions.size()));
			}

		} catch (Exception e) {
			handler.failed(e);
			throw e;
		}
	}

	public static void main(String... args) {
		int exitCode = new CommandLine(new Uninstaller()).execute(args);
		if(exitCode != Integer.MIN_VALUE)
			System.exit(exitCode);
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