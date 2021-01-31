package com.sshtools.forker.updater.swt;

import com.sshtools.forker.updater.AbstractHandler;
import com.sshtools.forker.updater.Entry;
import com.sshtools.forker.updater.UpdateHandler;
import com.sshtools.forker.updater.UpdateSession;

public class SWTUpdateHandler extends AbstractHandler implements UpdateHandler {

	@Override
	public void init(UpdateSession updater) {
	}

	@Override
	public void failed(Throwable error) {
	}

	@Override
	public void complete() {
	}

	@Override
	public void doneDownloadFile(Entry file) throws Exception {
	}

	@Override
	public void updateDownloadFileProgress(Entry file, float progress) throws Exception {
	}

	@Override
	public void updateDownloadProgress(float progress) throws Exception {
	}

	@Override
	public void startDownloadFile(Entry file, int index) throws Exception {
	}

	@Override
	public void startDownloads() throws Exception {
	}

	@Override
	public void updateDone(boolean upgradeError) {
	}

}
