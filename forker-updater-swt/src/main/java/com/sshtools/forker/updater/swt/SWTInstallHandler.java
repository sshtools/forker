package com.sshtools.forker.updater.swt;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.concurrent.Callable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;

import com.sshtools.forker.updater.InstallHandler;
import com.sshtools.forker.updater.InstallSession;
import com.sshtools.forker.updater.test.InstallTest;

public class SWTInstallHandler extends AbstractSWTHandler<InstallSession, Path> implements InstallHandler {

	public static void main(String[] args) throws Exception {
		System.setProperty("bannerImage", "../forker-updater-example/src/main/installer/left-banner.png");
		InstallTest.main(args, new SWTInstallHandler());
	}
	
	private Path chosenDestination;
	private ProgressBar progressBar;
	private Label progressText;
	private boolean cancelled;
	private Label installTextLabel;

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public Path value() {
		return chosenDestination;
	}

	@Override
	public void startInstallRollback() throws Exception {
		display.asyncExec(() -> {
			progressText.setText("");
			installTextLabel.setText(MessageFormat.format(
					session.properties().getProperty("installRollbackText", "Rolling back installation of {0} ..."),
					title));
		});
	}

	@Override
	public void installRollbackProgress(float progress) {
		display.asyncExec(() -> {
			progressBar.setSelection((int) (progress * 100));
		});
	}

	@Override
	public void installDone() {
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
			messageLabel.setText(MessageFormat
					.format(session.properties().getProperty("closeText", "Installation of {0} is complete."), title));

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
	public void installFile(Path file, Path d, int index) throws Exception {
		display.asyncExec(() -> {
			progressText.setText(file.toString());
		});
	}

	@Override
	public void installFileDone(Path file) throws Exception {
	}

	@Override
	public void installFileProgress(Path file, float progress) throws Exception {
	}

	@Override
	public void installProgress(float frac) throws Exception {
		display.asyncExec(() -> {
			progressBar.setSelection((int) (frac * 100));
		});
	}

	@Override
	public void startInstall() throws Exception {
		inProgress = true;
		display.asyncExec(() -> {
			createContainer();

			GridLayout mainGridLayout = new GridLayout();
			mainGridLayout.numColumns = 1;
			container.setLayout(mainGridLayout);

			/* Text */
			installTextLabel = new Label(container, SWT.WRAP);
			GridData installTextLabelGridData = new GridData();
			installTextLabelGridData.horizontalSpan = 3;
			installTextLabelGridData.horizontalAlignment = SWT.FILL;
			installTextLabelGridData.verticalAlignment = SWT.BEGINNING;
			installTextLabelGridData.grabExcessHorizontalSpace = true;
			installTextLabelGridData.grabExcessVerticalSpace = true;
			installTextLabel.setLayoutData(installTextLabelGridData);
			installTextLabel.setText(MessageFormat
					.format(session.properties().getProperty("installingText", "Installing {0} ..."), title));

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

	protected void createMainPage(Callable<Void> installCallback) {
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

		/* Install Location */
		Label installLocationLabel = new Label(container, SWT.NONE);
		installLocationLabel.setText("Install Location:");
		GridData installLocationLabelGridData = new GridData();
		installLocationLabelGridData.grabExcessHorizontalSpace = false;
		installLocationLabelGridData.grabExcessVerticalSpace = true;
		installLocationLabel.setLayoutData(installLocationLabelGridData);

		Text installLocation = new Text(container, SWT.BORDER);
		GridData installLocationGridData = new GridData();
		installLocationGridData.grabExcessHorizontalSpace = true;
		installLocationGridData.grabExcessVerticalSpace = true;
		installLocationGridData.widthHint = 300;
		installLocationGridData.horizontalAlignment = SWT.FILL;
		installLocation.setLayoutData(installLocationGridData);

		Button browse = new Button(container, SWT.PUSH);
		browse.setText("Browse");
		browse.addMouseListener(MouseListener.mouseUpAdapter((e) -> {
			DirectoryDialog dialog = new DirectoryDialog(shell);
			dialog.setFilterPath(installLocation.getText());
			String val = dialog.open();
			if (val != null)
				installLocation.setText(val);
		}));
		GridData browseGridData = new GridData();
		browseGridData.grabExcessHorizontalSpace = false;
		browseGridData.grabExcessVerticalSpace = true;
		browse.setLayoutData(browseGridData);
		installLocation.setText(session.base().toString());

		Composite actions = createActionBar(SWT.FILL, false);

		Button install = new Button(actions, SWT.PUSH);
		install.setText(session.properties().getProperty("installText", "Install"));
		install.addSelectionListener(SelectionListener.widgetSelectedAdapter((e) -> {
			chosenDestination = Paths.get(installLocation.getText());
			new Thread() {
				@Override
				public void run() {
					try {
						installCallback.call();
					} catch (Exception e1) {
					}
				}
			}.start();
		}));

		stack.layout();
	}

	@Override
	protected String getFrameTitle(InstallSession session) {
		return MessageFormat.format(session.properties().getProperty("frameTitle", "Installing {0}"), title);
	}

}
