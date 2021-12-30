package com.sshtools.forker.updater.swt;

import java.awt.Toolkit;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.sshtools.forker.updater.AbstractHandler;
import com.sshtools.forker.updater.Session;

public abstract class AbstractSWTHandler<S extends Session, V> extends AbstractHandler<S, V> {

	/** The logger. */
	protected Logger logger = Logger.getGlobal();

	protected static Display display;
	protected S session;
	protected Composite container;
	protected Shell shell;
	protected Composite stack;
	protected boolean inProgress;
	protected String title;

	private Composite root;
	private StackLayout stackLayout;
	private boolean cancelled;

	@Override
	public V prep(Callable<Void> callable) {

		/*
		 * Create a container for an optional banner image that may be placed at borrder
		 */
		root = new Composite(shell, SWT.NONE);
		GridLayout rootLayout = new GridLayout(3, false);
		root.setLayout(rootLayout);

		/* For Left/Top banners */
		String bannerPosition = session.properties().getProperty("bannerPosition", "left");
		if (session.properties().containsKey("bannerImage")) {
			switch (bannerPosition) {
			case "left":
				GridData leftGd = createBannerImage();
				leftGd.horizontalSpan = 1;
				leftGd.verticalSpan = 3;
				break;
			case "top":
				GridData topGd = createBannerImage();
				topGd.horizontalSpan = 3;
				topGd.verticalSpan = 1;
				break;
			}
		}

		/*
		 * Create a stack for the changing panels
		 */
		stack = new Composite(root, SWT.NONE);
		stack.setLayout(stackLayout = new StackLayout());

		/*
		 * Create the container for all of the wizard steps. All steps will be added,
		 * and hidden
		 */
		GridData stackGridData = new GridData();
		stackGridData.horizontalAlignment = GridData.FILL;
		stackGridData.verticalAlignment = SWT.FILL;
		stackGridData.grabExcessHorizontalSpace = true;
		stackGridData.grabExcessVerticalSpace = true;
		switch (bannerPosition) {
		case "left":
		case "right":
			stackGridData.horizontalSpan = 2;
			stackGridData.verticalSpan = 3;
			break;
		default:
			stackGridData.horizontalSpan = 3;
			stackGridData.verticalSpan = 2;
			break;
		}
		stack.setLayoutData(stackGridData);

		createContainer();

		/* For Right/Bottom banners */
		if (session.properties().containsKey("bannerImage")) {
			switch (bannerPosition) {
			case "right":
				GridData rightGd = createBannerImage();
				rightGd.horizontalSpan = 1;
				rightGd.verticalSpan = 3;
				break;
			case "bottom":
				GridData bottomGd = createBannerImage();
				bottomGd.horizontalSpan = 3;
				bottomGd.verticalSpan = 1;
				break;
			}
		}

		createMainPage(callable);

		String icon = session.properties().getProperty("icon");
		if(icon != null) {
			List<Image> l = new ArrayList<>();
			for(String i : icon.split(",")) {
				l.add(new Image(display, i));
			}
			shell.setImages(l.toArray(new Image[0]));
		}
		
		shell.pack();
		shell.open();

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();

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

		if (display == null) {
			display = new Display();
			setupShell();
		} else {
			display.asyncExec(() -> setupShell());
		}

	}

	protected void setupShell() {
		boolean resizable = "true".equals(session.properties().getProperty("resizable", "false"));
		shell = new Shell(display, resizable ? SWT.SHELL_TRIM : SWT.SHELL_TRIM & (~SWT.RESIZE));
		shell.setLayout(new FillLayout(SWT.VERTICAL | SWT.HORIZONTAL));
		shell.setText(getFrameTitle(session));
		shell.addListener(SWT.Close, event -> {
			event.doit = !inProgress;
		});
	}

	protected abstract String getFrameTitle(S session);

	@Override
	public void complete() {
		inProgress = false;
	}

	@Override
	public final void failed(Throwable error) {
		logger.log(Level.SEVERE, "Failed.", error);
		display.asyncExec(() -> {
			showError(error.getMessage() == null ? "No message supplied." : error.getMessage(), error);
		});
	}

	protected GridData createBannerImage() {
		String relpath = session.properties().getProperty("bannerImage");
		String bannerBackgroundColor = session.properties().getProperty("bannerBackgroundColor");
		Path abspath = resolveInstallerFile(relpath);
		Image bannerImage = new Image(display, abspath.toString());
		Canvas canvas = new Canvas(root, SWT.NONE);
		GridData bannerImageGridData = new GridData();
		bannerImageGridData.horizontalAlignment = GridData.FILL;
		bannerImageGridData.verticalAlignment = GridData.FILL;
		bannerImageGridData.widthHint = bannerImage.getBounds().width;
		bannerImageGridData.heightHint = bannerImage.getBounds().height;
		bannerImageGridData.verticalAlignment = GridData.FILL;
		canvas.setLayoutData(bannerImageGridData);
		Color bg = bannerBackgroundColor == null ? null : new Color(display, parseRgb(bannerBackgroundColor));
		canvas.addPaintListener(e -> {
			if (bg != null) {
				e.gc.setBackground(bg);
				e.gc.fillRectangle(0, 0, bannerImage.getBounds().width, bannerImage.getBounds().height);
			}
			e.gc.drawImage(bannerImage, 0, 0);
		});
		return bannerImageGridData;
	}

	private RGB parseRgb(String col) {
		if (col.startsWith("#"))
			col = col.substring(1);

		if (col.length() == 3)
			return new RGB(Integer.parseInt(col.substring(0, 1)) * 16, Integer.parseInt(col.substring(1, 2)) * 16,
					Integer.parseInt(col.substring(2, 3)) * 16);
		else
			return new RGB(Integer.parseInt(col.substring(0, 2)), Integer.parseInt(col.substring(2, 4)),
					Integer.parseInt(col.substring(4, 6)));
	}

	protected Path resolveInstallerFile(String relpath) {
		Path dir = Paths.get(System.getProperty("user.dir"));
		if (!Files.exists(dir)) {
			dir = Paths.get("src/main/installer");
		}
		return dir.resolve(relpath);
	}

	protected void createContainer() {

		container = new Composite(stack, SWT.NONE);
		stackLayout.topControl = container;
	}

	protected abstract void createMainPage(Callable<Void> installCallback);

	protected Composite createActionBar(int valign, boolean grabVertical) {
		/* Actions */
		Composite actions = new Composite(container, SWT.NONE);
		actions.setLayout(new RowLayout());
		GridData actionsGridData = new GridData();
		actionsGridData.horizontalSpan = 3;
		actionsGridData.verticalSpan = 1;
		actionsGridData.horizontalAlignment = SWT.END;
		actionsGridData.verticalAlignment = valign;
		actionsGridData.grabExcessHorizontalSpace = true;
		actionsGridData.grabExcessVerticalSpace = grabVertical;
		actions.setLayoutData(actionsGridData);
		return actions;
	}

	protected void showError(String message, Throwable trace) {
		inProgress = false;
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
		messageLabel.setText(message);

		/* Actions */
		Composite actions = createActionBar(SWT.FILL, false);
		Button close = new Button(actions, SWT.PUSH);
		close.setText(session.properties().getProperty("closeText", "Close"));
		close.addSelectionListener(SelectionListener.widgetSelectedAdapter((e) -> {
			shell.dispose();
		}));

		stack.layout();
	}

}
