package com.sshtools.forker.updater.swing;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.sshtools.forker.updater.AbstractHandler;
import com.sshtools.forker.updater.Session;

public abstract class AbstractSwingHandler<S extends Session, V> extends AbstractHandler<S, V> {

	/** The logger. */
	protected Logger logger = Logger.getGlobal();

	protected S session;
	protected JPanel container;
	protected JFrame shell;
	protected JPanel stack;
	protected boolean inProgress;
	protected String title;

	private JPanel root;
	private CardLayout stackLayout;
	private boolean cancelled;

	@Override
	public V prep(Callable<Void> callable) {

		/*
		 * Create a container for an optional banner image that may be placed at borrder
		 */
		root = new JPanel();
		shell.add(root);
		BorderLayout rootLayout = new BorderLayout();
		root.setLayout(rootLayout);

		/* For Left/Top banners */
		String bannerPosition = session.properties().getProperty("bannerPosition", "left");
		if (session.properties().containsKey("bannerImage")) {
			switch (bannerPosition) {
			case "left":
				root.add(createBannerImage(), BorderLayout.WEST);
				break;
			case "top":
				root.add(createBannerImage(), BorderLayout.NORTH);
				break;
			case "right":
				root.add(createBannerImage(), BorderLayout.EAST);
				break;
			case "bottom":
				root.add(createBannerImage(), BorderLayout.SOUTH);
				break;
			}
		}

		/*
		 * Create a stack for the changing panels
		 */
		stack = new JPanel();
		root.add(stack);
		stack.setLayout(stackLayout = new CardLayout());

		createContainer("main");
		createMainPage(callable);

		shell.pack();
		shell.setLocationRelativeTo(null);
		shell.setVisible(true);

		return null;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public final void init(S session) {
		this.session = session;

		title = session.properties().getProperty("title", "Application");

		if (shell == null) {
			setupShell();
		} else {
			SwingUtilities.invokeLater(() -> setupShell());
		}

	}

	protected void setupShell() {
		boolean resizable = "true".equals(session.properties().getProperty("resizable", "false"));
		String icon = session.properties().getProperty("icon");
		shell = new JFrame();
		if (icon != null) {
			List<Image> l = new ArrayList<>();
			for (String i : icon.split(",")) {
				l.add(Toolkit.getDefaultToolkit().getImage(i));
			}
			shell.setIconImages(l);
		}
		shell.setResizable(resizable);
		shell.setLayout(new BorderLayout());
		shell.setTitle(getFrameTitle(session));
		shell.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (!inProgress)
					shell.dispose();
			}
		});
		shell.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
	}

	protected abstract String getFrameTitle(S session);

	@Override
	public void complete() {
		inProgress = false;
	}

	@Override
	public final void failed(Throwable error) {
		logger.log(Level.SEVERE, "Failed.", error);
		SwingUtilities.invokeLater(() -> {
			showError(error.getMessage() == null ? "No message supplied." : error.getMessage(), error);
		});
	}

	protected JLabel createBannerImage() {
		String relpath = session.properties().getProperty("bannerImage");
		Path abspath = resolveInstallerFile(relpath);
		try {
			JLabel l = new JLabel(new ImageIcon(abspath.toUri().toURL()));
			String bannerBackgroundColor = session.properties().getProperty("bannerBackgroundColor");
			if (bannerBackgroundColor != null) {
				l.setBackground(Color.decode(bannerBackgroundColor));
				l.setOpaque(true);
			}
			return l;
		} catch (MalformedURLException e) {
			throw new IllegalStateException("Failed to load banner image.", e);
		}
	}

	protected Path resolveInstallerFile(String relpath) {
		Path dir = Paths.get(System.getProperty("user.dir"));
		if (!Files.exists(dir)) {
			dir = Paths.get("src/main/installer");
		}
		return dir.resolve(relpath);
	}

	protected void createContainer(String name) {

		container = new JPanel();
		container.setName(name);
		container.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		container.setLayout(new BorderLayout());
		stack.add(container, container.getName());
		stackLayout.show(stack, container.getName());
	}

	protected abstract void createMainPage(Callable<Void> installCallback);

	protected JPanel createActionBar() {
		/* Actions */
		JPanel actions = new JPanel();
		actions.setLayout(new FlowLayout(FlowLayout.RIGHT));
		container.add(actions, BorderLayout.SOUTH);
		return actions;
	}

	protected void showError(String message, Throwable trace) {
		inProgress = false;
		createContainer("error");

		container.setLayout(new BorderLayout());

		/* Decription */
		JLabel messageLabel = new JLabel(message);
		container.add(messageLabel, BorderLayout.CENTER);

		/* Actions */
		JPanel actions = createActionBar();
		container.add(actions, BorderLayout.SOUTH);
		JButton close = new JButton(session.properties().getProperty("closeText", "Close"));
		close.addActionListener((e) -> shell.dispose());
	}

}
