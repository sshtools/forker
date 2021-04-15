/**
 * Updater module
 */
module com.sshtools.forker.updater {
	requires transitive java.xml;
	requires transitive com.sshtools.forker.wrapper;
	exports com.sshtools.forker.updater; 
	uses com.sshtools.forker.updater.UpdateHandler;
	uses com.sshtools.forker.updater.InstallHandler;
}