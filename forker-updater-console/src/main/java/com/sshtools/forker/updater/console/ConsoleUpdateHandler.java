
package com.sshtools.forker.updater.console;

import com.sshtools.forker.updater.AbstractHandler;
import com.sshtools.forker.updater.Entry;
import com.sshtools.forker.updater.UpdateHandler;
import com.sshtools.forker.updater.UpdateSession;

public class ConsoleUpdateHandler extends AbstractHandler implements UpdateHandler {

	private UpdateSession context;

	@Override
	public void init(UpdateSession context) {
		this.context = context;
		ConsoleSystem.get().activate();
	}

	@Override
	public void startDownloads() throws Exception {
	}

	@Override
	public void startDownloadFile(Entry file) throws Exception {
	}

	@Override
	public void updateDownloadFileProgress(Entry file, float frac) throws Exception {
	}

	@Override
	public void doneDownloadFile(Entry file) throws Exception {
	}

	@Override
	public void failed(Throwable t) {
		t.printStackTrace();
	}

	@Override
	public void complete() {
	}

	@Override
	public void updateDownloadProgress(float frac) throws Exception {
	}

}
