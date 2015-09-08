package com.sshtools.forker.client;

public class Util {

	public static String escapeSingleQuotes(String src) {
		return src.replace("'", "''");
	}

	public static String escapeDoubleQuotes(String src) {
		return src.replace("\"", "\"\"");
	}

}
