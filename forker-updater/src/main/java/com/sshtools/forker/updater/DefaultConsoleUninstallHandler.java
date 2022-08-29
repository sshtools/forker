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
import java.nio.file.Path;
import java.util.concurrent.Callable;

public class DefaultConsoleUninstallHandler extends AbstractHandler<UninstallSession, Boolean> implements UninstallHandler {

	private boolean deleteAll;

	@Override
	public void init(UninstallSession context) {
	}

	@Override
	public void startUninstall() throws Exception {
	}

	@Override
	public void uninstallFile(Path file, Path dest, int index) throws Exception {
	}

	@Override
	public void uninstallFileProgress(Path file, float frac) throws Exception {
	}

	@Override
	public void uninstallProgress(float frac) throws Exception {
	}

	@Override
	public void uninstallFileDone(Path file) throws Exception {
	}

	@Override
	public void uninstallDone() {
	}

	@Override
	public Boolean prep(Callable<Void> callable) {
		String answer = prompt("Delete all files (Y)es/(No)?: ");
		return deleteAll = answer.equals("y") || answer.equals("yes");
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

	@Override
	public Boolean value() {
		return deleteAll;
	}
}
