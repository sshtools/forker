/**
 * PTY module
 */
module com.sshtools.forker.services {
	requires transitive com.sshtools.forker.common;
	requires transitive com.sshtools.forker.client;
	requires java.logging;
	requires org.freedesktop.dbus;
	exports com.sshtools.forker.services;
	exports com.sshtools.forker.services.impl;
	exports com.sshtools.forker.services.impl.systemd to org.freedesktop.dbus;

}