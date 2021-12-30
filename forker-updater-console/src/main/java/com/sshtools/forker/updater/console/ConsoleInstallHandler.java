package com.sshtools.forker.updater.console;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Erase;

import com.sshtools.forker.updater.InstallHandler;
import com.sshtools.forker.updater.InstallSession;

public class ConsoleInstallHandler extends AbstractConsoleHandler<InstallSession, Path> implements InstallHandler {

	private Path chosenDestination;

	@Override
	public Path prep(Callable<Void> callable) {
		chosenDestination = processPath(prompt(Ansi.ansi().a("Enter destination (").fg(Ansi.Color.RED).a("Enter")
				.fgDefault().a(" for %s):").toString(), session.base()));
		if (Files.exists(chosenDestination)) {
			if (Files.isDirectory(chosenDestination)) {
				if (Files.exists(chosenDestination.resolve("manifest.xml"))) {
					println(Ansi.ansi().bold().a(
							"The destination exists and appears to be an application. Are you sure you want to replace this?")
							.boldOff().toString());
					String answer = prompt(Ansi.ansi().a("Answer (").fg(Ansi.Color.RED).a("Y").fgDefault().a(")es or (")
							.fg(Ansi.Color.RED).a("N").fgDefault().a(") (").fg(Ansi.Color.RED).a("Enter").fgDefault()
							.a(" for Yes):").toString());
					if (answer.equals("") || answer.toLowerCase().startsWith("y")) {
						return chosenDestination;
					} else
						throw new IllegalStateException("Aborted.");
				} else {
					File[] files = chosenDestination.toFile().listFiles();
					if(files == null || files.length > 0)
						throw new IllegalStateException("Destination exists.");
				}
			} else
				throw new IllegalStateException("Destination exists and is not a directory.");
		}
		return chosenDestination;
	}

	@Override
	public Path value() {
		return chosenDestination;
	}

	@Override
	public void installDone() {
		Ansi ansi = Ansi.ansi();
		println(ansi.cursorToColumn(0).bold().a("Installation complete.").eraseLine(Erase.FORWARD).toString());
	}

	@Override
	public void installFile(Path file, Path dest, int index) throws Exception {
		this.currentIndex = index;
		this.currentDest = dest.getFileName().toString();
		this.currentFrac = 0;
		updateRow();

	}

	@Override
	public void installFileDone(Path file) throws Exception {
	}

	@Override
	public void installFileProgress(Path file, float progress) throws Exception {
	}

	@Override
	public void installProgress(float frac) throws Exception {
		currentFrac = frac;
		updateRow();
	}

	@Override
	public void startInstall() throws Exception {
		println("Installing");
	}

	private Path processPath(String path) {
		if (path == null)
			throw new IllegalStateException("Aborted.");
		if (path.equals(""))
			return session.base();
		return Paths.get(path);
	}

	@Override
	public void startInstallRollback() throws Exception {
		Ansi ansi = Ansi.ansi();
		println(ansi.cursorToColumn(0).bold().a("Starting rollback.").eraseLine(Erase.FORWARD).toString());
		this.currentIndex = 0;
		this.currentDest = "Rollback";
		this.currentFrac = 0;
		updateRow();
	}

	@Override
	public void installRollbackProgress(float progress) {
		currentFrac = progress;
		updateRow();
	}
}
