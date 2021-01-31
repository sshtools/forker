package com.sshtools.forker.updater;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.SystemUtils;

import com.sshtools.forker.updater.DesktopShortcut.Type;

public abstract class AbstractSession implements Session {

	private Updater updater;
	private AppManifest manifest;

	public Updater updater() {
		return updater;
	}

	public AppManifest manifest() {
		return manifest;
	}

	public AbstractSession manifest(AppManifest manifest) {
		this.manifest = manifest;
		return this;
	}

	public AbstractSession updater(Updater updater) {
		this.updater = updater;
		return this;
	}

	public void installShortcut(DesktopShortcut shortcut) throws IOException {
		if (SystemUtils.IS_OS_LINUX) {
			File file = getShortcutFile(shortcut.id() + ".desktop");
			writeDesktopFile(file, shortcut);
		} else
			throw new UnsupportedOperationException();
	}

	public boolean shorcutInstalled(String id) {
		if (SystemUtils.IS_OS_LINUX)
			return getShortcutFile(id + ".desktop").exists();
		else
			throw new UnsupportedOperationException();
	}

	public void uninstallShortcut(String id) throws IOException {
		if (SystemUtils.IS_OS_LINUX)
			getShortcutFile(id + ".desktop").delete();
		else
			throw new UnsupportedOperationException();
	}

	private void writeDesktopFile(File file, DesktopShortcut shortcut) throws IOException, FileNotFoundException {
		checkFilesParent(file);
		try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
			pw.println("#!/usr/bin/env xdg-open");
			pw.println("[Desktop Entry]");
			pw.println("Version=1.0");
			pw.println("Terminal=false");
			if (shortcut.icon() != null)
				pw.println("Icon=" + addIcon(shortcut.icon(), shortcut.id()));
			for (Map.Entry<Locale, String> en : shortcut.icons().entrySet()) {
				pw.println("Icon[" + en.getKey().toLanguageTag() + "]="
						+ addIcon(en.getValue(), shortcut.id() + "_" + en.getKey().toLanguageTag()));
			}
			if (shortcut.name() != null)
				pw.println("Name=" + shortcut.name());
			for (Map.Entry<Locale, String> en : shortcut.names().entrySet()) {
				pw.println("Name[" + en.getKey().toLanguageTag() + "]=" + en.getValue());
			}
			if (shortcut.comment() != null)
				pw.println("Comment=" + shortcut.comment());
			for (Map.Entry<Locale, String> en : shortcut.comments().entrySet()) {
				pw.println("Comment[" + en.getKey().toLanguageTag() + "]=" + en.getValue());
			}
			if (shortcut.executable() != null) {

				pw.println("Exec=" + shortcut.executable()
						+ (shortcut.arguments().isEmpty() ? "" : " " + String.join(" ", shortcut.arguments())));

			}
			if (!shortcut.categories().isEmpty())
				pw.println("Categories=" + String.join(";", shortcut.categories()));
			pw.println("StartupNotify=false"); // TODO never in Java unless native/jna etc
			if (shortcut.type() != Type.AUTOSTART) {
				switch (shortcut.type()) {
				case APPLICATION:
					pw.println("Type=Application");
					break;
				case LINK:
					pw.println("Type=Link");
					break;
				case DIRECTORY:
					pw.println("Type=Directory");
					break;
				case AUTOSTART:
					pw.println("Type=Application");
					pw.println("X-GNOME-Autostart-enabled=" + shortcut.autoStart());
					break;
				default:
					break;
				}
			}
			pw.println("Keywords=razer;snake;mamba;chroma;deathadder");
		}
	}

	protected String addIcon(String icon, String id) {
		try {
			URL url = new URL(icon);
			/* It's a URL, copy it */

			File iconFile = checkFilesParent(new File(getShare(), "pixmaps" + File.separator + id + ".png"));
			try (FileOutputStream fos = new FileOutputStream(iconFile)) {
				try (InputStream in = url.openStream()) {
					in.transferTo(fos);
				}
			}

			return iconFile.getAbsolutePath();
		} catch (IOException ioe) {
			/* Give up / named icon */
			return icon;
		}
	}

	private File checkFilesParent(File file) throws IOException {
		if (!file.exists() && !file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
			throw new IOException(String.format("Failed to create parent folder for %s.", file));
		}
		return file;
	}

	File getShare() {
		return new File(System.getProperty("user.home") + File.separator + ".local" + File.separator + "share");
	}

	File getShortcutFile(String id) {
		return new File(getShare(), "applications" + File.separator + id);
	}

	@Override
	public abstract long size();

	@Override
	public abstract int updates();

}
