module com.sshtools.forker.updater.swt {
	requires java.desktop;
	requires java.prefs;
	requires transitive org.eclipse.swt;
	
	/* Jenkins is linux so we must use this for now until a better solution
	 * is found. SWT (OSGi?) does not play well with JPMS
	 */
	requires transitive org.eclipse.swt.gtk.linux.x86_64;
//	requires static org.eclipse.swt.win32.win32.x86_64;
	
	requires transitive com.sshtools.forker.updater;
	requires transitive com.sshtools.forker.wrapper;
    exports com.sshtools.forker.updater.swt;
    opens com.sshtools.forker.updater.swt; 
	provides com.sshtools.forker.updater.UpdateHandler with com.sshtools.forker.updater.swt.SWTUpdateHandler;
	provides com.sshtools.forker.updater.InstallHandler with com.sshtools.forker.updater.swt.SWTInstallHandler;
}