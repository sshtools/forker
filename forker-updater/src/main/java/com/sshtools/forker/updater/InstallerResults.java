package com.sshtools.forker.updater;

public interface InstallerResults {
	
	void upgradeAndLaunch() throws Exception;
	
	void upgradeAndExit(int exit);

	void exit(int exit);
	
	void upgradeAndWait();
}
