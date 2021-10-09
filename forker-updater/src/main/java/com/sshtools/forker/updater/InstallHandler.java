package com.sshtools.forker.updater;

import java.nio.file.Path;

public interface InstallHandler extends Handler<InstallSession, Path> {

	void startInstall() throws Exception;

	void installProgress(float frac) throws Exception;

	void installFile(Path file, Path d, int index) throws Exception;

	void installFileProgress(Path file, float progress) throws Exception;

	void installFileDone(Path file) throws Exception;
	
	void startInstallRollback() throws Exception;
	
	void installRollbackProgress(float progress);

	void installDone();

}
