package com.sshtools.forker.updater.swt;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.SWT;

import com.sshtools.forker.updater.Handler;
import com.sshtools.forker.updater.InstallerToolkit;

public class SWTInstallerToolkit implements InstallerToolkit {

	public SWTInstallerToolkit() {
	}

	@Override
	public int getPriority() {
		return 1;
	}

	@Override
	public boolean isAvailable() {
		return SWT.isLoadable();
	}

	@Override
	public Set<Class<? extends Handler<?, ?>>> getHandlers() {
		return new HashSet<>(Arrays.asList(SWTInstallHandler.class, SWTUninstallHandler.class, SWTUpdateHandler.class));
	}

	@Override
	public void init() {
	}

}
