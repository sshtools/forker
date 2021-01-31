
package com.sshtools.forker.updater.console;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Erase;

import com.sshtools.forker.updater.Entry;
import com.sshtools.forker.updater.UpdateHandler;
import com.sshtools.forker.updater.UpdateSession;

public class ConsoleUpdateHandler extends AbstractConsoleHandler<UpdateSession> implements UpdateHandler {

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

}
