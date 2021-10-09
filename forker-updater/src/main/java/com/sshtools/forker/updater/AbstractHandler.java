package com.sshtools.forker.updater;

import java.io.Console;
import java.io.PrintWriter;

public abstract class AbstractHandler<S extends Session, V> implements Handler<S, V> {

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

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public void complete() {
	}

	@Override
	public void failed(Throwable error) {
		error.printStackTrace(out);
	}
}
