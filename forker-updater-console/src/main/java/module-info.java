module com.sshtools.forker.updater.console {
	requires transitive com.sshtools.forker.updater;
	requires transitive org.fusesource.jansi;
    exports com.sshtools.forker.updater.console; 
	provides com.sshtools.forker.updater.UpdateHandler with com.sshtools.forker.updater.console.ConsoleUpdateHandler;
	provides com.sshtools.forker.updater.InstallHandler with com.sshtools.forker.updater.console.ConsoleInstallHandler;
}