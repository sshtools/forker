/**
 * Common module
 * @uses com.sshtools.forker.common.IO
 */
module com.sshtools.forker.common {
	requires transitive org.apache.commons.lang3;
	requires transitive com.sun.jna;
	requires transitive com.sun.jna.platform;
	requires transitive org.apache.commons.io;
	exports com.sshtools.forker.common;
	uses com.sshtools.forker.common.IO;
}