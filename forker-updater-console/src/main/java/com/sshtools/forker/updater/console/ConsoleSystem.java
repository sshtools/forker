package com.sshtools.forker.updater.console;

import org.fusesource.jansi.AnsiConsole;

public class ConsoleSystem {

	private static ConsoleSystem instance;

	public static ConsoleSystem get() {
		if (instance == null) {
			instance = new ConsoleSystem();
		}
		return instance;
	}

	private boolean enabled;

	private ConsoleSystem() {
	}

	public void activate() {
		if (!enabled) {
			AnsiConsole.systemInstall();
			enabled = true;
		}
	}

	public void deactivate() {
		if (enabled) {
			AnsiConsole.systemUninstall();
			enabled = false;
		}
	}

}
