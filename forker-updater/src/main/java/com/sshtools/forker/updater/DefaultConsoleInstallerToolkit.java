package com.sshtools.forker.updater;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class DefaultConsoleInstallerToolkit implements InstallerToolkit {

	public DefaultConsoleInstallerToolkit() {
	}

	@Override
	public int getPriority() {
		return Integer.MIN_VALUE;
	}

	@Override
	public boolean isAvailable() {
		return true;
	}

	@Override
	public Set<Class<? extends Handler<?, ?>>> getHandlers() {
		return new HashSet<>(Arrays.asList(DefaultConsoleInstallHandler.class, DefaultConsoleUninstallHandler.class,
				DefaultConsoleUpdateHandler.class));
	}

	@Override
	public void init() {
	}

}
