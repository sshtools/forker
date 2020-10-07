
/**
 * Daemon module
 * 
 * @provides com.sshtools.forker.daemon.CommandExecutor
 * @provides com.sshtools.forker.daemon.Handler
 * @uses com.sshtools.forker.daemon.CommandExecutor
 * @uses com.sshtools.forker.daemon.Handler
 * @uses com.sshtools.forker.daemon.ControlHandler
 */
module com.sshtools.forker.daemon {
	requires transitive com.sshtools.forker.common;
	requires java.logging;

	exports com.sshtools.forker.daemon;

	uses com.sshtools.forker.daemon.CommandExecutor;
	uses com.sshtools.forker.daemon.Handler;

	provides com.sshtools.forker.daemon.CommandExecutor with com.sshtools.forker.daemon.StandardExecutor;
	provides com.sshtools.forker.daemon.Handler with com.sshtools.forker.daemon.CommandHandler,
			com.sshtools.forker.daemon.ControlHandler, com.sshtools.forker.daemon.OSInputStreamHandler, com.sshtools.forker.daemon.OSOutputStreamHandler;

}