package com.sshtools.forker.updater.test;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import com.sshtools.forker.updater.InstallHandler;
import com.sshtools.forker.updater.InstallSession;


public class InstallTest {
	public static void main(String[] args, InstallHandler installHandler) throws Exception {
		InstallSession s = new InstallSession();
		s.properties().putAll(System.getProperties());
		s.base(Paths.get(System.getProperty("user.dir")));
		installHandler.init(s);
		Path destination = installHandler.prep(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				installTo(installHandler, s, installHandler.value());
				return null;
			}
	
		});
		if (destination == null)
			return;
		installTo(installHandler, s, destination);
	}
	
	protected static void installTo(InstallHandler handler, InstallSession session, Path dest) throws Exception {
		handler.startInstall();
		float iprg = 0;
		try {
			Path d = Paths.get(System.getProperty("java.io.tmpdir"));
			for (int i = 0; i < 100; i++) {
				Path f = Paths.get(String.format("file%d", i));
				checkCancel(handler);
				handler.installFile(f, d, i);
				for (float prg = 0; prg < 1; prg += Math.random()) {
					checkCancel(handler);
					handler.installFileProgress(f, prg);
					handler.installProgress(iprg = ((float) i / 100) + (0.01f * prg));
					Thread.sleep(50);
				}
				checkCancel(handler);
				handler.installFileDone(f);
				if(i > 50 && Boolean.getBoolean("installTest.testRollback")) {
					throw new InterruptedIOException("Failed to install a thing!");
				}
			}
			checkCancel(handler);
			handler.installDone();
			handler.complete();
		} catch (InterruptedIOException iioe) {
			handler.startInstallRollback();
			for (float prg = iprg; prg >= 0; prg -= 0.01) {
				handler.installRollbackProgress(prg);
				Thread.sleep(50);
			}
			handler.failed(iioe);
		} catch (Exception e) {
			handler.failed(e);
			throw e;
		}
	}
	
	protected static void checkCancel(InstallHandler handler) throws InterruptedIOException {
		if (handler.isCancelled())
			throw new InterruptedIOException("Cancelled.");
	}
}
