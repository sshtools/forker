/*
 * Copyright 2018 Mordechai Meisels
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.sshtools.forker.updater;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Path;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

import com.sshtools.forker.updater.AppManifest.Entry;

public class ConsoleUpdateHandler implements UpdateHandler {

	private UpdateSession context;

	@Override
	public void init(UpdateSession context) {
		this.context = context;
	}

	@Override
	public void startDownloads() throws Exception {
		total = context.getUpdates().size();
		ordinalWidth = String.valueOf(total).length() * 2 + 1;
		initProgress();
	}

	@Override
	public void startDownloadFile(Entry file) throws Exception {
		index++;
		println(renderFilename(file));
		resetProgress(file.size());
	}

	@Override
	public void updateDownloadFileProgress(Entry file, float frac) throws Exception {
		currentFrac = frac;
	}

	@Override
	public void doneDownloadFile(Entry file) throws Exception {
		clear();
	}

	@Override
	public void failed(Throwable t) {
		clearln();
		t.printStackTrace();
	}

	@Override
	public void complete() {
		stopTimer = true;
	}

	// ------- Progress rendering, highly inspired by
	// https://github.com/ctongfei/progressbar

	private PrintStream out;
	private Timer timer;

	private int totalWidth;
	private int msgWidth;
	private int rateWidth;
	private int percentWidth;
	private int timeWidth;
	private String clear;

	private int ordinalWidth;
	private int total;
	private int index;

	private long totalBytes;
	private float lastFrac;
	private float currentFrac;
	private long start;
	private boolean stopTimer;

	protected void initProgress() {
		out = out();
		totalWidth = consoleWidth();
		msgWidth = "Downloading".length();
		rateWidth = "@ 100.0 kB/s".length();
		percentWidth = "100%".length();
		timeWidth = "0:00:00".length();
		clear = "\r" + repeat(totalWidth, " ") + "\r";

		timer = new Timer("Progress Printer", true);
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				if (stopTimer) {
					timer.cancel();
					return;
				}

				print(renderProgress());
				lastFrac = currentFrac;
			}
		}, 0, 1000);
	}

	protected void resetProgress(long bytes) {
		currentFrac = 0;
		lastFrac = 0;
		totalBytes = bytes;
		start = System.currentTimeMillis();
	}

	protected PrintStream out() {
		return System.out;
	}

	protected int consoleWidth() {
		return 80;
	}

	private void clear() {
		out.print(clear);
	}

	private void clearln() {
		out.println(clear);
	}

	private void print(String str) {
		out.print("\r");
		out.print(padRight(totalWidth, str));
	}

	private void println(String str) {
		out.print("\r");
		out.println(padRight(totalWidth, str));
	}

	protected String renderProgress() {
		StringBuilder sb = new StringBuilder();
		sb.append("Downloading ");

		String humanReadableBytes = humanReadableByteCount(totalBytes);

		sb.append(humanReadableBytes);
		sb.append(" ");
		if (lastFrac == 0 && currentFrac == 0) {
			sb.append(repeat(rateWidth + 1, " "));
		} else {
			sb.append("@ ");
			sb.append(padRight(rateWidth - 2,
					humanReadableByteCount((long) ((currentFrac - lastFrac) * totalBytes)) + "/s"));
			sb.append(" ");
		}
		sb.append(padLeft(percentWidth, ((int) (currentFrac * 100)) + "%"));
		sb.append(" [");

		int progressWidth = totalWidth - msgWidth - humanReadableBytes.length() - rateWidth - percentWidth - timeWidth
				- 7; // spaces

		int pieces = (int) ((progressWidth - 2) * currentFrac);
		String line = repeat(pieces, "=");
		if (pieces < progressWidth - 2)
			line += ">";

		sb.append(padRight(progressWidth - 2, line));
		sb.append("]");

		long elapsed = System.currentTimeMillis() - start;
		if (currentFrac > 0) {
			sb.append(" (");
			sb.append(formatSeconds(((long) (elapsed / currentFrac) - elapsed) / 1000));
			sb.append(")");
		}

		return sb.toString();
	}

	protected String renderFilename(Entry file) {
		return padLeft(ordinalWidth, index + "/" + total) + " " + compactName(file.path());
	}

	private String compactName(Path name) {
		Path relative = relativize(context.manifest().path(), name);
		return relative.isAbsolute() ? relative.getFileName().toString() : relative.toString();
	}

	public static String repeat(int n, String str) {
		if (n < 0)
			throw new IllegalArgumentException("n < 0: " + n);
		// first lets try to use JDK 11's String::repeat
		try {
			Method repeat = String.class.getMethod("repeat", int.class);
			return (String) repeat.invoke(str, n);
		} catch (ReflectiveOperationException e) {
			return String.join("", Collections.nCopies(n, str));
		}
	}

	public static String padLeft(int width, String str) {
		if (str.length() >= width)
			return str;

		return repeat(width - str.length(), " ") + str;
	}

	public static String padRight(int width, String str) {
		if (str.length() >= width)
			return str;

		return str + repeat(width - str.length(), " ");
	}

	public static String formatSeconds(long m) {
		return String.format("%d:%02d:%02d", m / 3600, (m % 3600) / 60, m % 60);
	}

	// https://stackoverflow.com/a/3758880/1751640
	public static String humanReadableByteCount(long bytes) {
		if (-1000 < bytes && bytes < 1000) {
			return bytes + " B";
		}
		CharacterIterator ci = new StringCharacterIterator("kMGTPE");
		while (bytes <= -999_950 || bytes >= 999_950) {
			bytes /= 1000;
			ci.next();
		}
		return String.format("%.1f %cB", bytes / 1000.0, ci.current());
	}

	public static URI relativize(URI base, URI other) {
		if (base == null || other == null)
			return other;

		return base.relativize(other);
	}

	public static Path relativize(Path base, Path other) {
		if (base == null || other == null)
			return other;

		try {
			return base.relativize(other);
		} catch (IllegalArgumentException e) {
		}

		return other;
	}

	@Override
	public void updateDownloadProgress(float frac) throws Exception {
	}
}
