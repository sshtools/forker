package com.sshtools.forker.updater;

import java.nio.file.Path;
import java.util.concurrent.Callable;

public interface InstallHandler {

	void init(InstallSession session);

	void startInstall() throws Exception;

	void failed(Throwable error);

	void complete();

	void installProgress(float frac) throws Exception;

	void installFile(Path file, Path d) throws Exception;

	void installFileProgress(Path file, float progress) throws Exception;

	void installFileDone(Path file) throws Exception;

	Path chosenDestination();

	Path chooseDestination(Callable<Void> callable);

}
