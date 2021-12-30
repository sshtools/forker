package com.sshtools.forker.updater.maven.plugin;

public enum LauncherMode {
	WRAPPED, UPDATER, DIRECT, UNINSTALLER;

	public boolean isBootstrap() {
		switch(this) {
		case WRAPPED:
		case UPDATER:
		case UNINSTALLER:
			return true;
		default:
			return false;
		}	
	}
	
	public boolean isRequiresAppCfg() {
		switch(this) {
		case WRAPPED:
		case UPDATER:
			return true;
		default:
			return false;
		}
	}
}
