package com.sshtools.forker.updater.console;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.sshtools.forker.updater.Handler;
import com.sshtools.forker.updater.InstallerToolkit;

public class ConsoleInstallerToolkit implements InstallerToolkit {

	public ConsoleInstallerToolkit() {
	}

	@Override
	public int getPriority() {
		return Integer.MIN_VALUE / 2;
	}

	@Override
	public boolean isAvailable() {
		return System.console() != null;
	}

	@Override
	public Set<Class<? extends Handler<?, ?>>> getHandlers() {
		return new HashSet<>(
				Arrays.asList(ConsoleInstallHandler.class, ConsoleUninstallHandler.class, ConsoleUpdateHandler.class));
	}

	@Override
	public void init() {
	}

}
