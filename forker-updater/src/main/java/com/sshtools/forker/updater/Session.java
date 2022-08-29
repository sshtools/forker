package com.sshtools.forker.updater;

import java.util.Properties;

public interface Session {
	
	int updates();
	
	long size();

	Properties properties();
}
