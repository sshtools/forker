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
	opens com.sshtools.forker.updater to info.picocli;
}