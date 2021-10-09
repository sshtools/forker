package com.sshtools.forker.updater;

@SuppressWarnings("serial")
public class NotFatalException extends Exception {

	public NotFatalException(String message, Throwable cause) {
		super(message, cause);
	}

	public NotFatalException(String message) {
		super(message);
	}

}
