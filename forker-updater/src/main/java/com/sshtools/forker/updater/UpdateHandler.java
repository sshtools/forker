package com.sshtools.forker.updater;

import java.net.URL;
import java.util.concurrent.Callable;

import com.sshtools.forker.updater.AppManifest.Entry;

public interface UpdateHandler {

	void init(UpdateSession updater);

	default void startingManifestLoad(URL location) {
	}

	default void completedManifestLoad(URL location) {
	}

	void failed(Throwable error);

	void complete();

	void doneDownloadFile(Entry file) throws Exception;

	void updateDownloadFileProgress(Entry file, float progress) throws Exception;

	void updateDownloadProgress(float progress) throws Exception;

	void startDownloadFile(Entry file) throws Exception;

	void startDownloads() throws Exception;

	default boolean noUpdates(Callable<Void> task) {
		return true;
	}

	default boolean updatesComplete(Callable<Void> task) throws Exception {
		return true;
	}
}
