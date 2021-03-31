/**
 * Wrapper module
 */
module com.sshtools.forker.wrapper {
	requires transitive com.sshtools.forker.common;
	requires transitive com.sshtools.forker.client;
	requires transitive java.logging;
	requires java.management;
	requires transitive jdk.attach;
	requires transitive info.picocli;

	exports com.sshtools.forker.wrapper;

	provides com.sshtools.forker.client.ForkerProcessFactory with com.sshtools.forker.wrapper.WrapperProcessFactory;
	provides com.sshtools.forker.common.IO with com.sshtools.forker.wrapper.WrapperIO;

	uses com.sshtools.forker.wrapper.WrapperPlugin;
}