package com.sshtools.forker.updater;

import java.nio.file.Path;

public interface UninstallHandler extends Handler<UninstallSession, Boolean> {

	void startUninstall() throws Exception;

	void uninstallProgress(float frac) throws Exception;

	void uninstallFile(Path file, Path d, int index) throws Exception;

	void uninstallFileProgress(Path file, float progress) throws Exception;

	void uninstallFileDone(Path file) throws Exception;

	void uninstallDone();

}
