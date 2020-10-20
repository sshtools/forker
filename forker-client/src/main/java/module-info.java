/**
 * Common module
 * 
 * @uses com.sshtools.forker.client.ForkerProcessFactory
 */
module com.sshtools.forker.client {
	exports com.sshtools.forker.client;

	requires transitive com.sshtools.forker.common;
	requires java.desktop;
	requires java.logging;

	uses com.sshtools.forker.client.ForkerProcessFactory;
}