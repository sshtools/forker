package com.sshtools.forker.update.swt;

import java.awt.Rectangle;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

import com.sshtools.forker.updater.InstallHandler;
import com.sshtools.forker.updater.InstallSession;

public class SWTInstallHandler implements InstallHandler {

	@Override
	public void init(InstallSession session) {
		final Display display = new Display();
		Shell shell = new Shell(display);
		shell.setText("Snippet 57");
		final ProgressBar bar = new ProgressBar(shell, SWT.SMOOTH);
		Rectangle clientArea = shell.getClientArea();
		bar.setBounds(clientArea.x, clientArea.y, 200, 32);
		shell.open();

		display.timerExec(100, new Runnable() {
			int i = 0;

			@Override
			public void run() {
				if (bar.isDisposed())
					return;
				bar.setSelection(i++);
				if (i <= bar.getMaximum())
					display.timerExec(100, this);
			}
		});

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}

	@Override
	public void startInstall() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void failed(Throwable error) {
		// TODO Auto-generated method stub

	}

	@Override
	public void complete() {
		// TODO Auto-generated method stub

	}

	@Override
	public void installProgress(float frac) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void installFile(Path file, Path d) throws Exception {
		// TODO Auto-generated method stub

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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Path chooseDestination(Callable<Void> callable) {
		// TODO Auto-generated method stub
		return null;
	}

}
