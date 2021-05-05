package com.sshtools.forker.updater.swt;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.sshtools.forker.updater.AbstractHandler;
import com.sshtools.forker.updater.InstallHandler;
import com.sshtools.forker.updater.InstallSession;

public class SWTInstallHandler extends AbstractHandler<InstallSession> implements InstallHandler {

	private InstallSession session;

	@Override
	public void init(InstallSession session) {
		this.session = session;

//		final Display display = new Display();
//		Shell shell = new Shell(display);
//		shell.setText("Snippet 57");
//		final ProgressBar bar = new ProgressBar(shell, SWT.SMOOTH);
//		Rectangle clientArea = shell.getClientArea();
//		bar.setBounds(clientArea.x, clientArea.y, 200, 32);
//		shell.open();
//
//		display.timerExec(100, new Runnable() {
//			int i = 0;
//
//			@Override
//			public void run() {
//				if (bar.isDisposed())
//					return;
//				bar.setSelection(i++);
//				if (i <= bar.getMaximum())
//					display.timerExec(100, this);
//			}
//		});
//
//		while (!shell.isDisposed()) {
//			if (!display.readAndDispatch())
//				display.sleep();
//		}
//		display.dispose();
	}

	@Override
	public void startInstall() throws Exception {
	}

	@Override
	public void installProgress(float frac) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void installFile(Path file, Path d, int index) throws Exception {
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

	private Path chosenDestination;

	@Override
	public Path chooseDestination(Callable<Void> callable) {
		Display display = new Display();
		final Shell shell = new Shell(display);
		shell.setText(session.properties().getProperty("title", "Application"));
		Label label = new Label(shell, SWT.WRAP);
		label.setText(session.properties().getProperty("description", "Forker launched application"));

		Composite p = new Composite(shell, SWT.NONE);
		RowLayout layout = new RowLayout();
		layout.wrap = false;
		layout.center = true;
		p.setLayout(layout);
		Label l = new Label(p, SWT.NONE);
		l.setText("Install Location:");
		Text installLocation = new Text(p, SWT.BORDER);
		Button browse = new Button(p, SWT.PUSH);
		browse.setText("Browse");
		browse.addMouseListener(MouseListener.mouseUpAdapter((e) -> {
			DirectoryDialog dialog = new DirectoryDialog(shell);
			dialog.setFilterPath(null);
			String val = dialog.open();
			if (val != null)
				installLocation.setText(val);
		}));
		installLocation.setText(session.base().toString());

		Button button1 = new Button(shell, SWT.PUSH);
		button1.setText("Install");
		Button button2 = new Button(shell, SWT.PUSH);
		button2.setText("Close");

		final int insetX = 4, insetY = 4;
		FormLayout formLayout = new FormLayout();
		formLayout.spacing = 8;
		formLayout.marginWidth = insetX;
		formLayout.marginHeight = insetY;
		shell.setLayout(formLayout);

		Point size = label.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		final FormData labelData = new FormData(size.x, SWT.DEFAULT);
		labelData.left = new FormAttachment(0, 0);
		labelData.right = new FormAttachment(100, 0);
		label.setLayoutData(labelData);
		shell.addListener(SWT.Resize, e -> {
			Rectangle rect = shell.getClientArea();
			labelData.width = rect.width - insetX * 2;
			shell.layout();
		});

		FormData button2Data = new FormData();
		button2Data.right = new FormAttachment(100, -insetX);
		button2Data.bottom = new FormAttachment(100, 0);
		button2.setLayoutData(button2Data);

		FormData button1Data = new FormData();
		button1Data.right = new FormAttachment(button2, -insetX);
		button1Data.bottom = new FormAttachment(100, 0);
		button1.setLayoutData(button1Data);

		FormData listData = new FormData();
		listData.left = new FormAttachment(0, 0);
		listData.right = new FormAttachment(100, 0);
		listData.top = new FormAttachment(label, insetY);
		listData.bottom = new FormAttachment(button2, -insetY);
		p.setLayoutData(listData);

		shell.pack();
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
		return chosenDestination;
	}

	@Override
	public Path chosenDestination() {
		return chosenDestination;
	}

	@Override
	public void installDone() {

	}

}
