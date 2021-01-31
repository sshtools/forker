package com.sshtools.forker.updater;

public interface Handler {

	void complete();

	void failed(Throwable error);
}
