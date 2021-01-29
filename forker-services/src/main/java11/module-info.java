/**
 * PTY module
 */
module com.sshtools.forker.services {
	requires transitive com.sshtools.forker.common;
	requires transitive com.sshtools.forker.client;
	requires java.logging;
	requires transitive java.desktop;
	requires transitive dbus.java;
	requires transitive java.systemd;
	exports com.sshtools.forker.services;

}