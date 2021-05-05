package com.sshtools.forker.updater;

import java.net.URL;
import java.util.concurrent.Callable;

public interface UpdateHandler extends Handler<UpdateSession> {

	default void startingManifestLoad(URL location) {
	}

	default void completedManifestLoad(URL location) {
	}

	void doneDownloadFile(Entry file) throws Exception;

	void updateDownloadFileProgress(Entry file, float progress) throws Exception;

	void updateDownloadProgress(float progress) throws Exception;

	void startDownloadFile(Entry file, int index) throws Exception;

	void startDownloads() throws Exception;

	default boolean noUpdates(Callable<Void> task) {
		return true;
	}

	default boolean updatesComplete(Callable<Void> task) throws Exception {
		return true;
	}

	void updateDone(boolean upgradeError);
}
