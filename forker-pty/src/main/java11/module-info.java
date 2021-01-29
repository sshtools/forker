/**
 * PTY module
 */
module com.sshtools.forker.pty {
	requires transitive com.sshtools.forker.common;
	requires transitive com.sshtools.forker.daemon;
	requires transitive pty4j;
	exports com.sshtools.forker.pty;

    provides com.sshtools.forker.common.IO
        with com.sshtools.forker.pty.PTYIO;

    provides com.sshtools.forker.daemon.CommandExecutor
        with com.sshtools.forker.pty.PTYExecutor;
}