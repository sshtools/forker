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
package com.sshtools.forker.updater;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

public class DefaultConsoleInstallHandler extends AbstractHandler<InstallSession, Path> implements InstallHandler {

	private InstallSession session;
	private Path dest;

	@Override
	public void init(InstallSession context) {
		this.session = context; 
	}

	@Override
	public void startInstall() throws Exception {
		System.out.println("Installing ....");
	}

	@Override
	public void installFile(Path file, Path dest, int index) throws Exception {
		System.out.println(file.toString() + " -> " + dest);
	}

	@Override
	public void installFileProgress(Path file, float frac) throws Exception {
	}

	@Override
	public void installProgress(float frac) throws Exception {
	}

	@Override
	public void installFileDone(Path file) throws Exception {
	}

	@Override
	public void installDone() {
		System.out.println("Install complete!");
	}

	@Override
	public void startInstallRollback() throws Exception {
	}

	@Override
	public void installRollbackProgress(float progress) {
	}

	@Override
	public Path value() {
		return dest;
	}

	@Override
	public void failed(Throwable error) {
		System.out.println("Install failed. " + error.getMessage());
	}

	@Override
	public Path prep(Callable<Void> callable) {
		Path destination = getDestination();
		if (Files.exists(destination)) {
			if (Files.isDirectory(destination)) {
				if (Files.exists(destination.resolve("manifest.xml"))) {
					println(
							"The destination exists and appears to an application. Are you sure you want to replace this?");
					String answer = prompt("Answer (Y)es or (N)o (Enter for yes):");
					if (answer.equals("") || answer.toLowerCase().startsWith("y")) {
						return destination;
					} else
						throw new IllegalStateException("Aborted.");
				} else
					throw new IllegalStateException("Destination exists.");
			} else
				throw new IllegalStateException("Destination exists and is not a directory.");
		}
		return destination;
	}

	private Path getDestination() {
		return processPath(prompt("Enter destination (Enter for %s): ", session.base()));
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
}
