package com.sshtools.forker.updater;

public interface Handler<S extends Session> {
	
	void init(S session);

	void complete();

	void failed(Throwable error);
}
