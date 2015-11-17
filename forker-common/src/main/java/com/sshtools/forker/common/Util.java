package com.sshtools.forker.common;

public class Util {

	public static String escapeSingleQuotes(String src) {
		return src.replace("'", "''");
	}

	public static String escapeDoubleQuotes(String src) {
		return src.replace("\"", "\"\"");
	}

}
