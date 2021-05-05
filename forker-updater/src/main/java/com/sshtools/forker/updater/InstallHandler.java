package com.sshtools.forker.updater;

import java.nio.file.Path;
import java.util.concurrent.Callable;

public interface InstallHandler extends Handler<InstallSession> {

	void startInstall() throws Exception;

	void installProgress(float frac) throws Exception;

	void installFile(Path file, Path d, int index) throws Exception;

	void installFileProgress(Path file, float progress) throws Exception;

	void installFileDone(Path file) throws Exception;

	Path chosenDestination();

	/**
	 * Get the location to install to. The {@link Callable} only need be
	 * invoked if this method do not block.
	 * 
	 * @param callable invoked when destination is obtained.
	 * @return the chosen path, or <code>null</code> if we will call the callable when the destination has been chosen
	 */
	Path chooseDestination(Callable<Void> callable);

	void installDone();

}
