
package com.sshtools.forker.updater;

public class ConsoleUpdateHandler extends AbstractHandler implements UpdateHandler {

	private UpdateSession context;

	@Override
	public void init(UpdateSession context) {
		this.context = context;
	}

	@Override
	public void startDownloads() throws Exception {
		total = context.getUpdates().size();
		ordinalWidth = String.valueOf(total).length() * 2 + 1;
		initProgress();
	}

	@Override
	public void startDownloadFile(Entry file) throws Exception {
		index++;
		println(renderFilename(file.path()));
		resetProgress(file.size());
	}

	@Override
	public void updateDownloadFileProgress(Entry file, float frac) throws Exception {
		currentFrac = frac;
	}

	@Override
	public void doneDownloadFile(Entry file) throws Exception {
		clear();
	}

	@Override
	public void failed(Throwable t) {
		clearln();
		t.printStackTrace();
	}

	@Override
	public void complete() {
		stopTimer = true;
	}

	@Override
	public void updateDownloadProgress(float frac) throws Exception {
	}

}
