package com.sshtools.forker.updater.console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.fusesource.jansi.Ansi.Erase;

import com.sshtools.forker.updater.AbstractHandler;
import com.sshtools.forker.updater.AbstractSession;

public class AbstractConsoleHandler<S extends AbstractSession> extends AbstractHandler {
	protected int currentIndex;
	protected String currentDest;
	protected float currentFrac;
	protected S session;

	public void init(S session) {
		this.session = session;
		ConsoleSystem.get().activate();
	}
	
	protected String prompt(String text, Object... args) {
		if (console == null) {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
				print(String.format(text, args));
				return reader.readLine();
			} catch (IOException ioe) {
				throw new IllegalStateException("Failed to get prompt.", ioe);
			}
		} else {
			return console.readLine(text, args);
		}
	}

	@Override
	public void failed(Throwable error) {
		Ansi ansi = Ansi.ansi();
		println(ansi.eraseLine().toString());
		println(Ansi.ansi().fg(Color.RED)
				.a("Install failed. " + (error == null || error.getMessage() == null ? "" : error.getMessage()))
				.fgDefault().toString());
	}

	protected void updateRow() {
		Ansi ansi = Ansi.ansi();
		int progressCells = Math.max(1, (int) (40f * currentFrac));
		String sizeReceived = toHumanSize((long)((double)session.size() * currentFrac));
		String totalSize = toHumanSize(session.size());
		print(ansi.cursorToColumn(0)
				.a(String.format("%4d/%4d [%-40s] - %-20s (%02d:%02d:%02d) [%10s / %10s]", currentIndex + 1, session.updates(),
						repeat("▒", progressCells), trim(currentDest, 20), 0, 0, 0, sizeReceived, totalSize))
				.eraseLine(Erase.FORWARD).cursorToColumn(progressCells + 11).toString());
		out.flush();
	}

	protected String toHumanSize(long l) {
		if(l < 1024) {
			return String.format("%dB", l);
		}
		else if(l < 1048576)
			return String.format("%dKiB", l / 1024);
		else if(l < 1073741824)
			return String.format("%dMiB", l / 1024 / 1024);
		else if(l < 1073741824 * 1024)
			return String.format("%dGiB", l / 1024 / 1024 / 1024);
		else 
			return String.format("%dTiB", l / 1024 / 1024 / 1024 / 1024);
	}

	protected String trim(String string, int max) {
		if (string.length() > max)
			return string.substring(0, max);
		else
			return string;
	}

	protected String repeat(String string, int max) {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < max; i++)
			b.append(string);
		return b.toString();
	}
}
