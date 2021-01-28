/*
 * Copyright 2018 Mordechai Meisels
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.sshtools.forker.updater.console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Erase;

import com.sshtools.forker.updater.AbstractHandler;
import com.sshtools.forker.updater.InstallHandler;
import com.sshtools.forker.updater.InstallSession;

public class ConsoleInstallHandler extends AbstractHandler implements InstallHandler {

	private InstallSession session;
	private Path dest;
	private Path chosenDestination;

	@Override
	public void init(InstallSession context) {
		this.session = context;
		ConsoleSystem.get().activate();
	}

	@Override
	public Path chooseDestination(Callable<Void> callable) {
		chosenDestination = getDestination();
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
				} else
					throw new IllegalStateException("Destination exists.");
			} else
				throw new IllegalStateException("Destination exists and is not a directory.");
		}
		return chosenDestination;
	}

	private Path getDestination() {
		return processPath(prompt(Ansi.ansi().a("Enter destination (").fg(Ansi.Color.RED).a("Enter").fgDefault()
				.a(" for %s):").toString(), session.base()));
	}

	private int getConsoleWidth() {
		return 80;
	}

	private String prompt(String text, Object... args) {
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

	private Path processPath(String path) {
		if (path == null)
			throw new IllegalStateException("Aborted.");
		if (path.equals(""))
			return dest = session.base();
		return dest = Paths.get(path);
	}

	@Override
	public void startInstall() throws Exception {
	}

	@Override
	public void failed(Throwable error) {
		// TODO Auto-generated method stub

	}

	@Override
	public void complete() {
		println("");
	}

	@Override
	public void installProgress(float frac) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void installFile(Path file, Path d, int index) throws Exception {
		Ansi ansi = Ansi.ansi();
		print(ansi.cursorToColumn(0).toString());
		print(String.format("%4d/%4d [%s40] - %20s (%02d:%02d:%02d)", index + 1, session.files().size(), "XXX",
				d.getFileName().toString(), 0, 0, 0));
//		print(ansi.eraseLine(Erase.FORWARD).toString());
		print(ansi.cursorToColumn(0).toString());

	}

	@Override
	public void installFileProgress(Path file, float progress) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void installFileDone(Path file) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public Path chosenDestination() {
		return chosenDestination;
	}
}
