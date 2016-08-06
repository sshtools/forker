package com.sshtools.forker.wrapper;

public interface ForkerWrapperMXBean {

	String getClassname();

	void restart() throws InterruptedException;

	void restart(boolean wait) throws InterruptedException;
	
	void stop() throws InterruptedException;

	void stop(boolean wait) throws InterruptedException;
}
