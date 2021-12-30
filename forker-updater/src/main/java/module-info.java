/**
 * Updater module
 */
module com.sshtools.forker.updater {
	requires transitive java.xml;
	requires transitive com.sshtools.forker.wrapper;
	exports com.sshtools.forker.updater; 
	exports com.sshtools.forker.updater.test;
	uses com.sshtools.forker.updater.UpdateHandler;
	uses com.sshtools.forker.updater.InstallHandler;
	uses com.sshtools.forker.updater.UninstallHandler;
	uses com.sshtools.forker.updater.InstallerToolkit;
	opens com.sshtools.forker.updater to info.picocli;
	provides com.sshtools.forker.updater.UpdateHandler with com.sshtools.forker.updater.DefaultConsoleUpdateHandler;
	provides com.sshtools.forker.updater.InstallHandler with com.sshtools.forker.updater.DefaultConsoleInstallHandler;
	provides com.sshtools.forker.updater.UninstallHandler with com.sshtools.forker.updater.DefaultConsoleUninstallHandler;
	provides com.sshtools.forker.updater.InstallerToolkit with com.sshtools.forker.updater.DefaultConsoleInstallerToolkit;
}