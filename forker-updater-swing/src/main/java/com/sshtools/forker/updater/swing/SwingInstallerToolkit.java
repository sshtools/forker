package com.sshtools.forker.updater.swing;

import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.swing.UIManager;

import com.sshtools.forker.updater.Handler;
import com.sshtools.forker.updater.InstallerToolkit;

public class SwingInstallerToolkit implements InstallerToolkit {

	public SwingInstallerToolkit() {
	}

	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public boolean isAvailable() {
		return !GraphicsEnvironment.isHeadless();
	}

	@Override
	public Set<Class<? extends Handler<?, ?>>> getHandlers() {
		return new HashSet<>(
				Arrays.asList(SwingInstallHandler.class, SwingUninstallHandler.class, SwingUpdateHandler.class));
	}

	@Override
	public void init() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
		}
	}
}
