package com.sshtools.forker.updater;

import java.io.Console;
import java.io.PrintWriter;

public abstract class AbstractHandler {

	protected Console console;
	protected PrintWriter out;

	protected AbstractHandler() {
		console = System.console();
		out = console == null ? new PrintWriter(System.out) : console.writer();
	}

	protected void println(String str) {
		out.println(str);
	}

	protected void print(String str) {
		out.print(str);
	}
}
