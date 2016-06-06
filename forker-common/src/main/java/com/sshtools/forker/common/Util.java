package com.sshtools.forker.common;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;

public class Util {

	public static List<String> parseQuotedString(String command) {
		List<String> args = new ArrayList<>();
		boolean escaped = false;
		boolean quoted = false;
		StringBuilder word = new StringBuilder();
		for (int i = 0; i < command.length(); i++) {
			char c = command.charAt(i);
			if (c == '"' && !escaped) {
				if (quoted) {
					quoted = false;
				} else {
					quoted = true;
				}
			} else if (c == '\\' && !escaped) {
				escaped = true;
			} else if (c == ' ' && !escaped && !quoted) {
				if (word.length() > 0) {
					args.add(word.toString());
					word.setLength(0);
					;
				}
			} else {
				word.append(c);
			}
		}
		if (word.length() > 0)
			args.add(word.toString());
		return args;
	}

	public static String escapeSingleQuotes(String src) {
		return src.replace("'", "''");
	}

	public static String escapeDoubleQuotes(String src) {
		return src.replace("\"", "\"\"");
	}

	public static String getUsernameForID(String id) throws IOException {
		if (SystemUtils.IS_OS_LINUX) {
			// TODO what about NIS etc?
			for (String line : FileUtils.readLines(new File("/etc/passwd"))) {
				String[] arr = line.split(":");
				if (arr.length > 2 && arr[2].equals(id)) {
					return arr[0];
				}
			}
		} else {
			throw new UnsupportedOperationException();
		}
		return null;
	}

	public static StringBuilder getQuotedCommandString(List<String> cmd) {
		// Take existing command and turn it into one escaped command
		StringBuilder bui = new StringBuilder();
		for (int i = 0; i < cmd.size(); i++) {
			if (bui.length() > 0) {
				bui.append(' ');
			}
			if (i > 0)
				bui.append("'");
			bui.append(Util.escapeSingleQuotes(cmd.get(i)));
			if (i > 0)
				bui.append("'");
		}
		return bui;
	}

	public static String getIDForUsername(String username) throws IOException {
		if (SystemUtils.IS_OS_LINUX) {
			// TODO what about NIS etc?
			for (String line : FileUtils.readLines(new File("/etc/passwd"))) {
				String[] arr = line.split(":");
				if (arr.length > 2 && arr[0].equals(username)) {
					return arr[2];
				}
			}
		} else {
			throw new UnsupportedOperationException();
		}
		return null;
	}
}
