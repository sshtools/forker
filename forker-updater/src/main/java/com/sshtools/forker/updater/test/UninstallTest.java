package com.sshtools.forker.updater.test;

import java.io.InterruptedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import com.sshtools.forker.updater.UninstallHandler;
import com.sshtools.forker.updater.UninstallSession;

public class UninstallTest {
	public static void main(String[] args, UninstallHandler uninstallHandler) throws Exception {
		UninstallSession s = new UninstallSession();
		s.properties().putAll(System.getProperties());
		s.base(Paths.get(System.getProperty("user.dir")));
		uninstallHandler.init(s);
		Boolean deleteAll = uninstallHandler.prep(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				uninstallFrom(uninstallHandler, s, uninstallHandler.value());
				return null;
			}

		});
		if (deleteAll == null)
			/*
			 * No destination means the install handler will prompt soon in its own way,
			 * call the callback when it has the destination folder
			 */
			return;
		uninstallFrom(uninstallHandler, s, deleteAll);

	}

	protected static void uninstallFrom(UninstallHandler handler, UninstallSession session, boolean deleteAll)
			throws Exception {
		handler.startUninstall();
		try {
			Path d = Paths.get(System.getProperty("java.io.tmpdir"));
			for (int i = 0; i < 100; i++) {
				Path f = Paths.get(String.format("file%d", i));
				checkCancel(handler);
				handler.uninstallFile(f, d, i);
				for (float prg = 0; prg < 1; prg += Math.random()) {
					checkCancel(handler);
					handler.uninstallFileProgress(f, prg);
					handler.uninstallProgress(((float) i / 100) + (0.01f * prg));
					Thread.sleep(50);
				}
				checkCancel(handler);
				handler.uninstallFileDone(f);
			}
			checkCancel(handler);
			handler.uninstallDone();
			handler.complete();
		} catch (Exception e) {
			handler.failed(e);
			throw e;
		}
	}

	protected static void checkCancel(UninstallHandler handler) throws InterruptedIOException {
		if (handler.isCancelled())
			throw new InterruptedIOException("Cancelled.");
	}
}
