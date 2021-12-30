package com.sshtools.forker.updater.maven.plugin;

public enum LauncherType {
	APPLICATION, SERVICE, USER_SERVICE, USER_APPLICATION;
	
	public boolean isRequiresAdministrator() {
		switch(this) {
		case APPLICATION:
		case SERVICE:
			return true;
		default:
			return false;
		}
	}
}
