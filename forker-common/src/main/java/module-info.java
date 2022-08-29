/**
 * Common module
 * @uses com.sshtools.forker.common.IO
 */
module com.sshtools.forker.common {
	requires transitive com.sun.jna;
	requires transitive com.sun.jna.platform;
	exports com.sshtools.forker.common;
	uses com.sshtools.forker.common.IO;
}