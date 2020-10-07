/**
 * Wrapper module
 */
module com.sshtools.forker.wrapper {
	requires transitive com.sshtools.forker.common;
	requires transitive com.sshtools.forker.client;
	requires transitive java.logging;
	requires java.scripting;
	requires java.management;
	requires transitive com.sshtools.forker.daemon;
	requires transitive commons.cli;

	exports com.sshtools.forker.wrapper;

	provides com.sshtools.forker.client.ForkerProcessFactory with com.sshtools.forker.wrapper.WrapperProcessFactory;

	provides com.sshtools.forker.common.IO with com.sshtools.forker.wrapper.WrapperIO;

	provides com.sshtools.forker.daemon.CommandExecutor with com.sshtools.forker.wrapper.CheckCommandPermission;

	provides com.sshtools.forker.daemon.Handler with com.sshtools.forker.wrapper.WrapperHandler;
}