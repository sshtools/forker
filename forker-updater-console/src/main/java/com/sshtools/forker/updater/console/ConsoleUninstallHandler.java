package com.sshtools.forker.updater.console;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Erase;

import com.sshtools.forker.updater.UninstallHandler;
import com.sshtools.forker.updater.UninstallSession;

public class ConsoleUninstallHandler extends AbstractConsoleHandler<UninstallSession, Boolean>
		implements UninstallHandler {

	private Boolean uninstallAll;

	@Override
	public Boolean prep(Callable<Void> callable) {
		String answer = prompt(Ansi.ansi().a("Uninstall all files? (").fg(Ansi.Color.RED).a("Y").fgDefault()
				.a(")es or (").fg(Ansi.Color.RED).a("N").fgDefault().a(") (").fg(Ansi.Color.RED).a("Enter").fgDefault()
				.a(" for Yes):").toString());
		if (answer.equals("") || answer.toLowerCase().startsWith("y")) {
			return uninstallAll = Boolean.TRUE;
		} else if (answer.equals("n") || answer.toLowerCase().startsWith("n")) {
			return uninstallAll = Boolean.TRUE;
		} else
			throw new IllegalStateException("Aborted.");
	}

	@Override
	public Boolean value() {
		return uninstallAll;
	}

	@Override
	public void uninstallDone() {
		Ansi ansi = Ansi.ansi();
		println(ansi.cursorToColumn(0).bold().a("Uninstallation complete.").eraseLine(Erase.FORWARD).toString());
	}

	@Override
	public void uninstallFile(Path file, Path dest, int index) throws Exception {
		this.currentIndex = index;
		this.currentDest = dest.getFileName().toString();
		this.currentFrac = 0;
		updateRow();

	}

	@Override
	public void uninstallFileDone(Path file) throws Exception {
	}

	@Override
	public void uninstallFileProgress(Path file, float progress) throws Exception {
	}

	@Override
	public void uninstallProgress(float frac) throws Exception {
		currentFrac = frac;
		updateRow();
	}

	@Override
	public void startUninstall() throws Exception {
		println("Uninstalling");
	}
}
