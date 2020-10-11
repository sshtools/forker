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

import java.io.Console;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Path;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

public abstract class AbstractHandler {

	// ------- Progress rendering, highly inspired by
	// https://github.com/ctongfei/progressbar

	protected PrintWriter out;
	protected Timer timer;

	protected int totalWidth;
	protected int msgWidth;
	protected int rateWidth;
	protected int percentWidth;
	protected int timeWidth;
	protected String clear;

	protected int ordinalWidth;
	protected int total;
	protected int index;

	protected long totalBytes;
	protected float lastFrac;
	protected float currentFrac;
	protected long start;
	protected boolean stopTimer;

	protected Console console;

	protected AbstractHandler() {
		console = System.console();
		out = console == null ? new PrintWriter(System.out) : console.writer();
	}

	protected void initProgress() {

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

	protected int consoleWidth() {
		return 80;
	}

	protected void clear() {
		out.print(clear);
		totalWidth = consoleWidth();
	}

	protected void clearln() {
		out.println(clear);
		totalWidth = consoleWidth();
	}

	protected void print(String str) {
		out.print("\r");
		out.print(padRight(totalWidth, str));
		totalWidth = consoleWidth();
	}

	protected void println(String str) {
		out.print("\r");
		out.println(padRight(totalWidth, str));
		totalWidth = consoleWidth();
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

	protected String renderFilename(Path file) {
		return padLeft(ordinalWidth, index + "/" + total) + " " + file;
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
}
