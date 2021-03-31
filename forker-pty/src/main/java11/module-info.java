/**
 * PTY module
 */
module com.sshtools.forker.pty {
	requires transitive com.sshtools.forker.common;
	requires transitive pty4j;
	requires transitive com.sshtools.forker.client;
	exports com.sshtools.forker.pty;

    provides com.sshtools.forker.common.IO
        with com.sshtools.forker.pty.PTYIO;

    provides com.sshtools.forker.client.ForkerProcessFactory
        with com.sshtools.forker.pty.PTYProcessFactory;

}