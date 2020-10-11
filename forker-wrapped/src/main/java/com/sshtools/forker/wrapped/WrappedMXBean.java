package com.sshtools.forker.wrapped;

public interface WrappedMXBean {

	int launch(String[] args);

	int shutdown();
}
