package com.sshtools.forker.updater.swt;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;

import com.sshtools.forker.updater.AbstractHandler;
import com.sshtools.forker.updater.InstallHandler;
import com.sshtools.forker.updater.InstallSession;

public class SWTInstallHandler extends AbstractHandler implements InstallHandler {

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
		Display display = new Display ();
		final Shell shell = new Shell (display);
		shell.setText("Snippet 65");
		Label label = new Label (shell, SWT.WRAP);
		label.setText ("This is a long text string that will wrap when the dialog is resized.");
		List list = new List (shell, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		list.setItems ("Item 1", "Item 2");
		Button button1 = new Button (shell, SWT.PUSH);
		button1.setText ("OK");
		Button button2 = new Button (shell, SWT.PUSH);
		button2.setText ("Cancel");

		final int insetX = 4, insetY = 4;
		FormLayout formLayout = new FormLayout ();
		formLayout.marginWidth = insetX;
		formLayout.marginHeight = insetY;
		shell.setLayout (formLayout);

		Point size = label.computeSize (SWT.DEFAULT, SWT.DEFAULT);
		final FormData labelData = new FormData (size.x, SWT.DEFAULT);
		labelData.left = new FormAttachment (0, 0);
		labelData.right = new FormAttachment (100, 0);
		label.setLayoutData (labelData);
		shell.addListener (SWT.Resize, e -> {
			Rectangle rect = shell.getClientArea ();
			labelData.width = rect.width - insetX * 2;
			shell.layout ();
		});

		FormData button2Data = new FormData ();
		button2Data.right = new FormAttachment (100, -insetX);
		button2Data.bottom = new FormAttachment (100, 0);
		button2.setLayoutData (button2Data);

		FormData button1Data = new FormData ();
		button1Data.right = new FormAttachment (button2, -insetX);
		button1Data.bottom = new FormAttachment (100, 0);
		button1.setLayoutData (button1Data);

		FormData listData = new FormData ();
		listData.left = new FormAttachment (0, 0);
		listData.right = new FormAttachment (100, 0);
		listData.top = new FormAttachment (label, insetY);
		listData.bottom = new FormAttachment (button2, -insetY);
		list.setLayoutData (listData);

		shell.pack ();
		shell.open ();
		while (!shell.isDisposed ()) {
			if (!display.readAndDispatch ()) display.sleep ();
		}
		display.dispose ();
		
//		chosenDestination = processPath(prompt(Ansi.ansi().a("Enter destination (").fg(Ansi.Color.RED).a("Enter")
//				.fgDefault().a(" for %s):").toString(), session.base()));
//		if (Files.exists(chosenDestination)) {
//			if (Files.isDirectory(chosenDestination)) {
//				if (Files.exists(chosenDestination.resolve("manifest.xml"))) {
//					println(Ansi.ansi().bold().a(
//							"The destination exists and appears to be an application. Are you sure you want to replace this?")
//							.boldOff().toString());
//					String answer = prompt(Ansi.ansi().a("Answer (").fg(Ansi.Color.RED).a("Y").fgDefault().a(")es or (")
//							.fg(Ansi.Color.RED).a("N").fgDefault().a(") (").fg(Ansi.Color.RED).a("Enter").fgDefault()
//							.a(" for Yes):").toString());
//					if (answer.equals("") || answer.toLowerCase().startsWith("y")) {
//						return chosenDestination;
//					} else
//						throw new IllegalStateException("Aborted.");
//				} else
//					throw new IllegalStateException("Destination exists.");
//			} else
//				throw new IllegalStateException("Destination exists and is not a directory.");
//		}
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
