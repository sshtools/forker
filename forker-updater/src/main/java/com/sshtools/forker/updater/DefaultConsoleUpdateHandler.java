
package com.sshtools.forker.updater;

public class DefaultConsoleUpdateHandler extends AbstractHandler implements UpdateHandler {

	private UpdateSession context;

	@Override
	public void init(UpdateSession context) {
		this.context = context;
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
	}

	@Override
	public void complete() {
	}

	@Override
	public void updateDownloadProgress(float frac) throws Exception {
	}

}
