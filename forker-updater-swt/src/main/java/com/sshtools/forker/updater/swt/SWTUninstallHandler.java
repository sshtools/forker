package com.sshtools.forker.updater.swt;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.concurrent.Callable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;

import com.sshtools.forker.updater.UninstallHandler;
import com.sshtools.forker.updater.UninstallSession;
import com.sshtools.forker.updater.test.UninstallTest;

public class SWTUninstallHandler extends AbstractSWTHandler<UninstallSession, Boolean> implements UninstallHandler {

	public static void main(String[] args) throws Exception {
		System.setProperty("bannerImage", "../forker-updater-example/src/main/installer/left-banner.png");
		UninstallTest.main(args, new SWTUninstallHandler());
	}

	private boolean deleteAll;
	private ProgressBar progressBar;
	private Label progressText;
	private boolean cancelled;
	private Label uninstallTextLabel;

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public Boolean value() {
		return deleteAll;
	}

	@Override
	public void uninstallDone() {
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
	public void uninstallFile(Path file, Path d, int index) throws Exception {
		display.asyncExec(() -> {
			progressText.setText(file.toString());
		});
	}

	@Override
	public void uninstallFileDone(Path file) throws Exception {
	}

	@Override
	public void uninstallFileProgress(Path file, float progress) throws Exception {
	}

	@Override
	public void uninstallProgress(float frac) throws Exception {
		display.asyncExec(() -> {
			progressBar.setSelection((int) (frac * 100));
		});
	}

	@Override
	public void startUninstall() throws Exception {
		inProgress = true;
		display.asyncExec(() -> {
			createContainer();

			GridLayout mainGridLayout = new GridLayout();
			mainGridLayout.numColumns = 1;
			container.setLayout(mainGridLayout);

			/* Text */
			uninstallTextLabel = new Label(container, SWT.WRAP);
			GridData uninstallTextLabelGridData = new GridData();
			uninstallTextLabelGridData.horizontalSpan = 3;
			uninstallTextLabelGridData.horizontalAlignment = SWT.FILL;
			uninstallTextLabelGridData.verticalAlignment = SWT.BEGINNING;
			uninstallTextLabelGridData.grabExcessHorizontalSpace = true;
			uninstallTextLabelGridData.grabExcessVerticalSpace = true;
			uninstallTextLabel.setLayoutData(uninstallTextLabelGridData);
			uninstallTextLabel.setText(MessageFormat.format(
					session.properties().getProperty("uninstallingText", "Uninstalling {0} from this computer ..."),
					title));

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

			stack.layout();
		});
	}

	protected void createMainPage(Callable<Void> uninstallCallback) {
		/* Create the main page, showing description and destination */

		GridLayout mainGridLayout = new GridLayout();
		mainGridLayout.numColumns = 3;
		container.setLayout(mainGridLayout);

		/* Application Name */
		Label appNameLabel = new Label(container, SWT.WRAP);
		GridData appNameLabelGridData = new GridData();
		appNameLabelGridData.horizontalSpan = 3;
		appNameLabelGridData.horizontalAlignment = SWT.FILL;
		appNameLabelGridData.grabExcessHorizontalSpace = true;
		appNameLabelGridData.grabExcessVerticalSpace = false;
		appNameLabel.setLayoutData(appNameLabelGridData);
		appNameLabel.setText(session.properties().getProperty("titleText", title));

		/* Application Version */
		Label versionLabel = new Label(container, SWT.WRAP);
		GridData versionLabelGridData = new GridData();
		versionLabelGridData.horizontalSpan = 3;
		versionLabelGridData.horizontalAlignment = SWT.FILL;
		versionLabelGridData.grabExcessHorizontalSpace = true;
		versionLabelGridData.grabExcessVerticalSpace = false;
		versionLabel.setLayoutData(versionLabelGridData);
		versionLabel.setText(MessageFormat.format(session.properties().getProperty("versionText", "Version: {0}"),
				session.properties().getProperty("version", "Unknown")));

		/* Decription */
		Label label = new Label(container, SWT.WRAP);
		GridData labelGridData = new GridData();
		labelGridData.horizontalSpan = 3;
		labelGridData.horizontalAlignment = SWT.FILL;
		labelGridData.grabExcessHorizontalSpace = true;
		labelGridData.grabExcessVerticalSpace = false;
		label.setLayoutData(labelGridData);
		label.setText(session.properties().getProperty("description", "Forker launched application"));

		/* Delete All */

		Button deleteAll = new Button(container, SWT.CHECK);
		GridData deleteAllGridData = new GridData();
		deleteAllGridData.grabExcessHorizontalSpace = false;
		deleteAllGridData.grabExcessVerticalSpace = true;
		deleteAll.setLayoutData(deleteAllGridData);
		deleteAll.addSelectionListener(SelectionListener.widgetSelectedAdapter((e) -> {
			SWTUninstallHandler.this.deleteAll = deleteAll.getSelection();
		}));

		Label deleteAllLabel = new Label(container, SWT.NONE);
		deleteAllLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				SWTUninstallHandler.this.deleteAll = !deleteAll.getSelection();
				deleteAll.setSelection(SWTUninstallHandler.this.deleteAll);
			}
		});
		deleteAllLabel.setText("Delete All");
		GridData deleteAllLabelGridData = new GridData();
		deleteAllLabelGridData.grabExcessHorizontalSpace = true;
		deleteAllLabelGridData.grabExcessVerticalSpace = true;
		deleteAllLabelGridData.widthHint = 300;
		deleteAllLabelGridData.horizontalAlignment = SWT.FILL;
		deleteAllLabel.setLayoutData(deleteAllLabelGridData);

		/* Actions */
		Composite actions = createActionBar(SWT.FILL, false);
		Button uninstall = new Button(actions, SWT.PUSH);
		uninstall.setText(session.properties().getProperty("uninstallText", "Uninstall"));
		uninstall.addSelectionListener(SelectionListener.widgetSelectedAdapter((e) -> {
			new Thread() {
				@Override
				public void run() {
					try {
						uninstallCallback.call();
					} catch (Exception e1) {
					}
				}
			}.start();
		}));

		stack.layout();
	}

	@Override
	protected String getFrameTitle(UninstallSession session) {
		return MessageFormat.format(session.properties().getProperty("frameTitle", "Uninstalling {0}"), title);
	}

}
