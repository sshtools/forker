package com.sshtools.forker.updater.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.concurrent.Callable;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.sshtools.forker.updater.InstallHandler;
import com.sshtools.forker.updater.InstallSession;
import com.sshtools.forker.updater.test.InstallTest;

public class SwingInstallHandler extends AbstractSwingHandler<InstallSession, Path> implements InstallHandler {

	public static void main(String[] args) throws Exception {
		System.setProperty("bannerImage", "../forker-updater-example/src/main/installer/left-banner.png");
		InstallTest.main(args, new SwingInstallHandler());
	}

	private Path chosenDestination;
	private JProgressBar progressBar;
	private JLabel progressText;
	private boolean cancelled;
	private JLabel installTextLabel;

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
		SwingUtilities.invokeLater(() -> {
			progressText.setText("");
			installTextLabel.setText(MessageFormat.format(
					session.properties().getProperty("installRollbackText", "Rolling back installation of {0} ..."),
					title));
		});
	}

	@Override
	public void installRollbackProgress(float progress) {
		SwingUtilities.invokeLater(() -> {
			progressBar.setValue((int) (progress * 100));
		});
	}

	@Override
	public void installDone() {
		SwingUtilities.invokeLater(() -> {

			createContainer("done");

			/* Decription */
			JLabel messageLabel = new JLabel(MessageFormat
					.format(session.properties().getProperty("closeText", "Installation of {0} is complete."), title));
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
	public void installFile(Path file, Path d, int index) throws Exception {
		SwingUtilities.invokeLater(() -> {
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
		SwingUtilities.invokeLater(() -> {
			progressBar.setValue((int) (frac * 100));
		});
	}

	@Override
	public void startInstall() throws Exception {
		inProgress = true;
		SwingUtilities.invokeLater(() -> {
			createContainer("start");

			/* Text */
			installTextLabel = new JLabel(MessageFormat
					.format(session.properties().getProperty("installingText", "Installing {0} ..."), title));
			container.add(installTextLabel, BorderLayout.NORTH);

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
		});
	}

	protected void createMainPage(Callable<Void> installCallback) {
		/* Create the main page, showing description and destination */

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
		//Box middle = new Box(BoxLayout.X_AXIS);
		JPanel middle = new JPanel(new FlowLayout());
		middle.add(new JLabel("Install Location:"));
		middle.setAlignmentY(JPanel.CENTER_ALIGNMENT);
		@SuppressWarnings("serial")
		JTextField installLocation = new JTextField() {
			@Override
			public Dimension getPreferredSize() {
				return new Dimension(300, super.getPreferredSize().height);
			}
		};
		installLocation.setText(session.base().toString());
		middle.add(installLocation);
		JButton browse = new JButton("Browse");
		browse.addActionListener((e) -> {
			JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.setSelectedFile(new File(installLocation.getText()));
			int returnVal = chooser.showOpenDialog(container);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				installLocation.setText(chooser.getSelectedFile().getPath());
			}
		});
		browse.setMnemonic('b');
		middle.add(browse);
		
		
		container.add(middle, BorderLayout.CENTER);

		/* Actions */
		JPanel actions = createActionBar();
		JButton uninstall = new JButton(session.properties().getProperty("installText", "Install"));
		actions.add(uninstall);
		uninstall.addActionListener((e) -> {
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
		});

	}

	@Override
	protected String getFrameTitle(InstallSession session) {
		return MessageFormat.format(session.properties().getProperty("frameTitle", "Installing {0}"), title);
	}

}
