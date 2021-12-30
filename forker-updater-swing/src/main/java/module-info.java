module com.sshtools.forker.updater.swing {
	requires java.desktop;
	requires java.prefs;
	
	requires transitive com.sshtools.forker.updater;
	requires transitive com.sshtools.forker.wrapper;
    exports com.sshtools.forker.updater.swing;
    opens com.sshtools.forker.updater.swing; 
	provides com.sshtools.forker.updater.UpdateHandler with com.sshtools.forker.updater.swing.SwingUpdateHandler;
	provides com.sshtools.forker.updater.InstallHandler with com.sshtools.forker.updater.swing.SwingInstallHandler;
	provides com.sshtools.forker.updater.UninstallHandler with com.sshtools.forker.updater.swing.SwingUninstallHandler;
	provides com.sshtools.forker.updater.InstallerToolkit with com.sshtools.forker.updater.swing.SwingInstallerToolkit;
}