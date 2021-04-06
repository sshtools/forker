/**
 * Common module
 * 
 * @uses com.sshtools.forker.client.ForkerProcessFactory
 */
module com.sshtools.forker.client {
	exports com.sshtools.forker.client;
	exports com.sshtools.forker.client.impl;
	exports com.sshtools.forker.client.impl.nonblocking;
	exports com.sshtools.forker.client.impl.jna.posix;
	exports com.sshtools.forker.client.impl.jna.win32;
	exports com.sshtools.forker.client.impl.jna.osx;
	exports com.sshtools.forker.client.ui;

	requires static transitive com.sshtools.forker.common;
	requires java.desktop;
	requires java.logging;

	uses com.sshtools.forker.client.ForkerProcessFactory;
}