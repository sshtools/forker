package com.sshtools.forker.updater.swt;

import java.text.MessageFormat;
import java.util.concurrent.Callable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;

import com.sshtools.forker.updater.Entry;
import com.sshtools.forker.updater.UpdateHandler;
import com.sshtools.forker.updater.UpdateSession;
import com.sshtools.forker.updater.test.UpdateTest;

public class SWTUpdateHandler extends AbstractSWTHandler<UpdateSession, Void> implements UpdateHandler {

	public static void main(String[] args) throws Exception {
		System.setProperty("bannerImage", "../forker-updater-example/src/main/installer/left-banner.png");
		UpdateTest.main(args, new SWTUpdateHandler());
	}

	private ProgressBar progressBar;
	private Label progressText;
	private boolean cancelled;
	private Label updatingTextLabel;

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	protected void createMainPage(Callable<Void> updateCallback) {

		createContainer();

		GridLayout mainGridLayout = new GridLayout();
		mainGridLayout.numColumns = 1;
		container.setLayout(mainGridLayout);

		/* Text */
		updatingTextLabel = new Label(container, SWT.WRAP);
		GridData updatingLabelGridData = new GridData();
		updatingLabelGridData.horizontalSpan = 3;
		updatingLabelGridData.horizontalAlignment = SWT.FILL;
		updatingLabelGridData.verticalAlignment = SWT.BEGINNING;
		updatingLabelGridData.grabExcessHorizontalSpace = true;
		updatingLabelGridData.grabExcessVerticalSpace = true;
		updatingTextLabel.setLayoutData(updatingLabelGridData);
		updatingTextLabel.setText(
				MessageFormat.format(session.properties().getProperty("updatingText", "Updatiing {0} ..."), title));

		/* Decription */
		progressText = new Label(container, SWT.WRAP);
		GridData progressTextGridData = new GridData();
		progressTextGridData.horizontalSpan = 3;
		progressTextGridData.horizontalAlignment = SWT.FILL;
		progressTextGridData.grabExcessHorizontalSpace = true;
		progressTextGridData.grabExcessVerticalSpace = false;
		progressText.setLayoutData(progressTextGridData);
		progressText.setText("");

		/* Progress Bar */
		progressBar = new ProgressBar(container, SWT.NONE);
		progressBar.setMaximum(100);
		GridData progressBarGridData = new GridData();
		progressBarGridData.widthHint = 300;
		progressBarGridData.grabExcessHorizontalSpace = true;
		progressBarGridData.grabExcessVerticalSpace = false;
		progressBarGridData.horizontalAlignment = SWT.FILL;
		progressBar.setLayoutData(progressBarGridData);

		Composite actions = createActionBar(SWT.END, true);
		Button cancel = new Button(actions, SWT.PUSH);
		cancel.setText(session.properties().getProperty("cancelText", "Cancel"));
		cancel.addSelectionListener(SelectionListener.widgetSelectedAdapter((e) -> {
			cancel.setEnabled(false);
			cancelled = true;
		}));
		
		new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(3000);
					updateCallback.call();
				} catch (Exception e1) {
				}
			}
		}.start();

		stack.layout();
	}

	@Override
	public void updateDone(boolean upgradeError) {
		display.asyncExec(() -> {

			createContainer();

			GridLayout mainGridLayout = new GridLayout();
			mainGridLayout.numColumns = 1;
			container.setLayout(mainGridLayout);

			/* Decription */
			Label messageLabel = new Label(container, SWT.WRAP);
			GridData labelGridData = new GridData();
			labelGridData.horizontalSpan = 3;
			labelGridData.verticalAlignment = SWT.BEGINNING;
			labelGridData.horizontalAlignment = SWT.FILL;
			labelGridData.grabExcessHorizontalSpace = true;
			labelGridData.grabExcessVerticalSpace = true;
			messageLabel.setLayoutData(labelGridData);
			messageLabel.setText(MessageFormat.format(
					session.properties().getProperty("closeText", "Uninstallation of {0} is complete."), title));

			/* Actions */
			Composite actions = createActionBar(SWT.FILL, false);
			Button close = new Button(actions, SWT.PUSH);
			close.setText(session.properties().getProperty("closeText", "Close"));
			close.addSelectionListener(SelectionListener.widgetSelectedAdapter((e) -> {
				shell.dispose();
			}));

			stack.layout();
		});
	}

	@Override
	public void startDownloadFile(Entry file, int index) throws Exception {
		display.asyncExec(() -> {
			progressText.setText(file.target().toString());
		});
	}

	@Override
	public void doneDownloadFile(Entry file) throws Exception {
	}

	@Override
	public void updateDownloadFileProgress(Entry file, float progress) throws Exception {
	}

	@Override
	public void updateDownloadProgress(float frac) throws Exception {
		display.asyncExec(() -> {
			progressBar.setSelection((int) (frac * 100));
		});
	}

	@Override
	public void startDownloads() throws Exception {
		inProgress = true;
	}

	@Override
	public void startUpdateRollback() {
		display.asyncExec(() -> {
			progressText.setText("");
			updatingTextLabel.setText(MessageFormat.format(
					session.properties().getProperty("updateRollbackText", "Rolling back update of {0} ..."),
					title));
		});
	}

	@Override
	public void updateRollbackProgress(float progress) {
		display.asyncExec(() -> {
			progressBar.setSelection((int) (progress * 100));
		});
	}

	@Override
	protected String getFrameTitle(UpdateSession session) {
		return MessageFormat.format(session.properties().getProperty("frameTitle", "Updating {0}"), title);
	}

	@Override
	public Void value() {
		return null;
	}
}
