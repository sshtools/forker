package com.sshtools.forker.updater.swing;

import java.awt.BorderLayout;
import java.text.MessageFormat;
import java.util.concurrent.Callable;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import com.sshtools.forker.updater.Entry;
import com.sshtools.forker.updater.UpdateHandler;
import com.sshtools.forker.updater.UpdateSession;
import com.sshtools.forker.updater.test.UpdateTest;

public class SwingUpdateHandler extends AbstractSwingHandler<UpdateSession, Void> implements UpdateHandler {

	public static void main(String[] args) throws Exception {
		System.setProperty("bannerImage", "../forker-updater-example/src/main/installer/left-banner.png");
		UpdateTest.main(args, new SwingUpdateHandler());
	}

	private JProgressBar progressBar;
	private JLabel progressText;
	private boolean cancelled;
	private JLabel updatingTextLabel;

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	protected void createMainPage(Callable<Void> updateCallback) {

		createContainer("start");

		/* Text */
		updatingTextLabel = new JLabel(
				MessageFormat.format(session.properties().getProperty("updatingText", "Updating {0} ..."), title));
		container.add(updatingTextLabel, BorderLayout.NORTH);

		/* Decription */
		progressText = new JLabel();
		progressText.setAlignmentX(JComponent.LEFT_ALIGNMENT);

		/* Progress Bar */
		progressBar = new JProgressBar();
		progressBar.setMaximum(100);

		/* Middle */
		Box middle = new Box(BoxLayout.Y_AXIS);
		middle.add(Box.createVerticalGlue());
		middle.add(progressText);
		middle.add(progressBar);
		middle.add(Box.createVerticalGlue());
		middle.setAlignmentY(JComponent.CENTER_ALIGNMENT);
		container.add(middle, BorderLayout.CENTER);

		JPanel actions = createActionBar();
		JButton cancel = new JButton(session.properties().getProperty("cancelText", "Cancel"));
		actions.add(cancel);
		cancel.addActionListener((e) -> {
			cancel.setEnabled(false);
			cancelled = true;
		});

		shell.doLayout();

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
	}

	@Override
	public void updateDone(boolean upgradeError) {
		SwingUtilities.invokeLater(() -> {

			createContainer("done");

			/* Decription */
			JLabel messageLabel = new JLabel(MessageFormat
					.format(session.properties().getProperty("closeText", "Update of {0} is complete."), title));
			container.add(messageLabel, BorderLayout.NORTH);

			/* Actions */
			JPanel actions = createActionBar();
			JButton close = new JButton(session.properties().getProperty("closeText", "Close"));
			actions.add(close);
			close.addActionListener((e) -> {
				shell.dispose();
			});
		});
	}

	@Override
	public void startDownloadFile(Entry file, int index) throws Exception {
		SwingUtilities.invokeLater(() -> {
			progressText.setText(file.displayName());
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
		SwingUtilities.invokeLater(() -> {
			progressBar.setValue((int) (frac * 100));
		});
	}

	@Override
	public void startDownloads() throws Exception {
		inProgress = true;
	}

	@Override
	public void startUpdateRollback() {
		SwingUtilities.invokeLater(() -> {
			progressText.setText("");
			updatingTextLabel.setText(MessageFormat.format(
					session.properties().getProperty("updateRollbackText", "Rolling back update of {0} ..."), title));
		});
	}

	@Override
	public void updateRollbackProgress(float progress) {
		SwingUtilities.invokeLater(() -> {
			progressBar.setValue((int) (progress * 100));
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
