package com.sshtools.forker.updater;

import java.util.concurrent.Callable;

public interface Handler<S extends Session, V> {
	
	boolean isCancelled();
	
	void init(S session);

	void complete();

	void failed(Throwable error);
	
	V prep(Callable<Void> callback);
	
	V value();
}
