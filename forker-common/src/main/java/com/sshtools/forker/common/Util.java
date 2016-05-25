package com.sshtools.forker.common;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;

public class Util {

	public static String escapeSingleQuotes(String src) {
		return src.replace("'", "''");
	}

	public static String escapeDoubleQuotes(String src) {
		return src.replace("\"", "\"\"");
	}

	public static String getUsernameForID(String id) throws IOException {
		if(SystemUtils.IS_OS_LINUX) {
			// TODO what about NIS etc?
			for(String line : FileUtils.readLines(new File("/etc/passwd"))) {
				String[] arr = line.split(":");
				if(arr.length > 2 && arr[2].equals(id)) {
					return arr[0];
				}
			}			
		}
		else {
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
		if(SystemUtils.IS_OS_LINUX) {
			// TODO what about NIS etc?
			for(String line : FileUtils.readLines(new File("/etc/passwd"))) {
				String[] arr = line.split(":");
				if(arr.length > 2 && arr[0].equals(username)) {
					return arr[2];
				}
			}			
		}
		else {
			throw new UnsupportedOperationException();
		}
		return null;
	}
}
