package com.sshtools.forker.updater.test;

import java.io.InterruptedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import com.sshtools.forker.updater.AppManifest;
import com.sshtools.forker.updater.Entry;
import com.sshtools.forker.updater.InstallHandler;
import com.sshtools.forker.updater.UpdateHandler;
import com.sshtools.forker.updater.UpdateSession;

public class UpdateTest {
	public static void main(String[] args, UpdateHandler updateHandler) throws Exception {
		UpdateSession s = new UpdateSession();
		s.properties().putAll(System.getProperties());
		updateHandler.init(s);
		updateHandler.prep(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				update(updateHandler, s);
				return null;
			}
		});
	}

	protected static void update(UpdateHandler handler, UpdateSession session) throws Exception {
		AppManifest mf = new AppManifest();
		handler.startDownloads();
		float iprg = 0;
		try {
			for (int i = 0; i < 100; i++) {
				Path pf = Files.createTempFile("abc", "def");
				pf.toFile().deleteOnExit();
				Entry f = new Entry(pf, mf);
				checkCancel(handler);
				handler.startDownloadFile(f, i);
				for (float prg = 0; prg < 1; prg += Math.random()) {
					checkCancel(handler);
					handler.updateDownloadFileProgress(f, prg);
					handler.updateDownloadProgress(iprg = (((float) i / 100) + (0.01f * prg)));
					Thread.sleep(50);
				}
				checkCancel(handler);
				handler.doneDownloadFile(f);
				Files.delete(pf);
				if (i > 50 && Boolean.getBoolean("updateTest.testRollback")) {
					throw new InterruptedIOException("Failed to update a thing!");
				}
			}
			checkCancel(handler);
			handler.updateDone(false);
			handler.complete();
		} catch (InterruptedIOException iioe) {
			handler.startUpdateRollback();
			for (float prg = iprg; prg >= 0; prg -= 0.01) {
				handler.updateRollbackProgress(prg);
				Thread.sleep(50);
			}
			handler.failed(iioe);
		} catch (Exception e) {
			e.printStackTrace();
			handler.failed(e);
			throw e;
		}
	}

	protected static void checkCancel(UpdateHandler handler) throws InterruptedIOException {
		if (handler.isCancelled())
			throw new InterruptedIOException("Cancelled.");
	}

	protected static void checkCancel(InstallHandler handler) throws InterruptedIOException {
		if (handler.isCancelled())
			throw new InterruptedIOException("Cancelled.");
	}
}
