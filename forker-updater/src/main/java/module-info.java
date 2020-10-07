/**
 * Updater module
 */
module com.sshtools.forker.updater {
	requires java.xml;
	requires transitive com.sshtools.forker.wrapper;
	requires transitive commons.cli;
	exports com.sshtools.forker.updater;
	uses com.sshtools.forker.updater.UpdateHandler;
}