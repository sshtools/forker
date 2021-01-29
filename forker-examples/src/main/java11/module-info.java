module com.sshtools.forker.examples {
	requires transitive com.sshtools.forker.daemon;
	requires java.logging;
	requires org.apache.commons.io;
	requires com.sshtools.forker.client;
	requires com.sshtools.forker.pty;
	requires com.sshtools.forker.wrapper;

	exports com.sshtools.forker.examples;


}