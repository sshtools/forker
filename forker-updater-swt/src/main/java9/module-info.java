module com.sshtools.forker.updater.swt {
	requires java.desktop;
	requires java.prefs;
	requires transitive org.eclipse.swt;
	requires transitive org.eclipse.swt.gtk.linux.x86.64;
	requires transitive com.sshtools.forker.updater;
	requires transitive com.sshtools.forker.wrapper;
    exports com.sshtools.forker.update.swt;
    opens com.sshtools.forker.update.swt; 
	provides com.sshtools.forker.updater.UpdateHandler with com.sshtools.forker.update.swt.SWTUpdateHandler;
	provides com.sshtools.forker.updater.InstallHandler with com.sshtools.forker.update.swt.SWTInstallHandler;
}