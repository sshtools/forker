module com.sshtools.forker.updater.swt {
	requires java.desktop;
	requires java.prefs;
	requires transitive org.eclipse.swt;
//	requires transitive org.eclipse.swt.${osgi.platform}; 
	requires transitive com.sshtools.forker.updater;
	requires transitive com.sshtools.forker.wrapper;
    exports com.sshtools.forker.updater.swt;
    opens com.sshtools.forker.updater.swt; 
	provides com.sshtools.forker.updater.UpdateHandler with com.sshtools.forker.updater.swt.SWTUpdateHandler;
	provides com.sshtools.forker.updater.InstallHandler with com.sshtools.forker.updater.swt.SWTInstallHandler;
}