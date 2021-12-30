module com.sshtools.forker.updater.console {
	requires transitive com.sshtools.forker.updater;
	requires transitive org.fusesource.jansi;
    exports com.sshtools.forker.updater.console; 
	provides com.sshtools.forker.updater.UpdateHandler with com.sshtools.forker.updater.console.ConsoleUpdateHandler;
	provides com.sshtools.forker.updater.InstallHandler with com.sshtools.forker.updater.console.ConsoleInstallHandler;
	provides com.sshtools.forker.updater.UninstallHandler with com.sshtools.forker.updater.console.ConsoleUninstallHandler;
	provides com.sshtools.forker.updater.InstallerToolkit with com.sshtools.forker.updater.console.ConsoleInstallerToolkit;
}