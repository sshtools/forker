module com.sshtools.forker.examples {
	requires java.logging;
	requires com.sshtools.forker.client;
	requires com.sshtools.forker.pty;
	requires com.sshtools.forker.wrapped;
	requires transitive com.sshtools.forker.wrapper; 

	exports com.sshtools.forker.examples;


}