
package com.sshtools.forker.updater.console;

import java.util.concurrent.Callable;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Erase;

import com.sshtools.forker.updater.Entry;
import com.sshtools.forker.updater.UpdateHandler;
import com.sshtools.forker.updater.UpdateSession;

public class ConsoleUpdateHandler extends AbstractConsoleHandler<UpdateSession, Void> implements UpdateHandler {

	@Override
	public void startDownloads() throws Exception {
		println("Updating");
	}

	@Override
	public void startDownloadFile(Entry file, int index) throws Exception {
		this.currentIndex = index;
		this.currentDest = file.path().getFileName().toString();
		this.currentFrac = 0;
		updateRow();
	}

	@Override
	public void updateDownloadFileProgress(Entry file, float frac) throws Exception {
	}

	@Override
	public void doneDownloadFile(Entry file) throws Exception {
	}

	@Override
	public void updateDone(boolean updateError) {
		Ansi ansi = Ansi.ansi();
		println(ansi.cursorToColumn(0).bold().a("Update complete.").eraseLine(Erase.FORWARD).toString());
	}

	@Override
	public void updateDownloadProgress(float frac) throws Exception {
		currentFrac = frac;
		updateRow();
	}

	@Override
	public void startUpdateRollback() {
		Ansi ansi = Ansi.ansi();
		println(ansi.cursorToColumn(0).bold().a("Starting rollback.").eraseLine(Erase.FORWARD).toString());
		this.currentIndex = 0;
		this.currentDest = "Rollback";
		this.currentFrac = 0;
		updateRow();
	}

	@Override
	public void updateRollbackProgress(float progress) {
		currentFrac = progress;
		updateRow();
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
