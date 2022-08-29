
package com.sshtools.forker.updater;

import java.util.concurrent.Callable;

public class DefaultConsoleUpdateHandler extends AbstractHandler<UpdateSession, Void> implements UpdateHandler {

	@Override
	public void init(UpdateSession context) { 
	}

	@Override
	public void startDownloads() throws Exception {
	}

	@Override
	public void startDownloadFile(Entry file, int index) throws Exception {
	}

	@Override
	public void updateDownloadFileProgress(Entry file, float frac) throws Exception {
	}

	@Override
	public void doneDownloadFile(Entry file) throws Exception {
	}

	@Override
	public void updateDone(boolean upgradeError) {
	}

	@Override
	public void updateDownloadProgress(float frac) throws Exception {
	}

	@Override
	public void startUpdateRollback() {
		System.out.println("Rolling back.");
	}

	@Override
	public void updateRollbackProgress(float progress) {
		if(progress == 1)
			System.out.println("Rollback complete.");
		
	}

	@Override
	public Void prep(Callable<Void> callback) {
		return null;
	}

	@Override
	public Void value() {
		return null;
	}

}
