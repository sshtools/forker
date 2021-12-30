package com.sshtools.forker.updater.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.concurrent.Callable;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import com.sshtools.forker.updater.UninstallHandler;
import com.sshtools.forker.updater.UninstallSession;
import com.sshtools.forker.updater.test.UninstallTest;

public class SwingUninstallHandler extends AbstractSwingHandler<UninstallSession, Boolean> implements UninstallHandler {

	public static void main(String[] args) throws Exception {
		System.setProperty("bannerImage", "../forker-updater-example/src/main/installer/left-banner.png");
		UninstallTest.main(args, new SwingUninstallHandler());
	}

	private JCheckBox deleteAll;
	private JProgressBar progressBar;
	private JLabel progressText;
	private boolean cancelled;
	private JLabel uninstallTextLabel;

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public Boolean value() {
		return deleteAll.isSelected();
	}

	@Override
	public void uninstallDone() {
		SwingUtilities.invokeLater(() -> {

			createContainer("done");

			/* Decription */
			JLabel messageLabel = new JLabel(MessageFormat.format(
					session.properties().getProperty("closeText", "Uninstallation of {0} is complete."), title));
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
	public void uninstallFile(Path file, Path d, int index) throws Exception {
		SwingUtilities.invokeLater(() -> {
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
		SwingUtilities.invokeLater(() -> {
			progressBar.setValue((int) (frac * 100));
		});
	}

	@Override
	public void startUninstall() throws Exception {
		inProgress = true;
		SwingUtilities.invokeLater(() -> {
			createContainer("start");

			/* Text */
			uninstallTextLabel = new JLabel(MessageFormat.format(
					session.properties().getProperty("uninstallingText", "Uninstalling {0} from this computer ..."),
					title));
			container.add(uninstallTextLabel, BorderLayout.NORTH);

			/* Decription */
			progressText = new JLabel();
			progressText.setAlignmentX(JComponent.LEFT_ALIGNMENT);

			/* Progress Bar */
			progressBar = new JProgressBar();
			progressBar.setMaximum(100);

			/* Middle */
			Box middle = new Box(BoxLayout.Y_AXIS);
			middle.add( Box.createVerticalGlue() );
			middle.add(progressText);
			middle.add(progressBar);
			middle.add( Box.createVerticalGlue() );
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
		});
	}

	protected void createMainPage(Callable<Void> uninstallCallback) {
		/* Create the top panel */
		Box top = new Box(BoxLayout.Y_AXIS);
		container.add(top, BorderLayout.NORTH);

		/* Application Name */
		JLabel appNameLabel = new JLabel(session.properties().getProperty("titleText", title));
		top.add(appNameLabel);

		/* Application Version */
		JLabel versionLabel = new JLabel(
				MessageFormat.format(session.properties().getProperty("versionText", "Version: {0}"),
						session.properties().getProperty("version", "Unknown")));
		top.add(versionLabel);

		/* Decription */
		JLabel label = new JLabel(session.properties().getProperty("description", "Forker launched application"));
		top.add(label);

		/* Delete All */

		deleteAll = new JCheckBox("Delete All");
		deleteAll.setPreferredSize(new Dimension(300, 12));
		container.add(deleteAll, BorderLayout.CENTER);

		/* Actions */
		JPanel actions = createActionBar();
		JButton uninstall = new JButton(session.properties().getProperty("uninstallText", "Uninstall"));
		actions.add(uninstall);
		uninstall.addActionListener((e) -> {
			new Thread() {
				@Override
				public void run() {
					try {
						uninstallCallback.call();
					} catch (Exception e1) {
					}
				}
			}.start();
		});
	}

	@Override
	protected String getFrameTitle(UninstallSession session) {
		return MessageFormat.format(session.properties().getProperty("frameTitle", "Uninstalling {0}"), title);
	}

}
