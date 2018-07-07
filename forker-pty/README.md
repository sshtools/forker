# Forker PTY support

This module adds support for a new I/O mode, [PTYExecutor.PTY](src/main/java/com/sshtools/forker/pty/PTYExecutor.java).
This mode will use [Forker Daemon](../forker-daemon/README.md) to maintain one or more pseudo terminals.

## Maven

This module is not available on Maven Central due to it's dependencies. Instead it can be found at our own Artifactory server :-


```xml
<repositories>
	<repository>
		<id>opensource-releases</id>
		<url>http://artifactory.javassh.com/opensource-snapshots</url>
		<name>SSHTOOLS Open Source Releases</name>
	</repository>
</repositories>
```

And your dependency configuration :-
    
```
<dependencies>
	<dependency>
		<groupId>com.sshtools</groupId>
		<artifactId>forker-pty</artifactId>
		<version>1.5</version>
	</dependency>
</dependencies>
```

## Example

```java
		/*
		 * This example reads from stdin (i.e. the console), so stdin needs to be unbuffered with
		 * no local echoing at this end of the pipe, the following function
		 * attempts to do this. 
		 */
		OS.unbufferedStdin();
		
		/* PTY requires the daemon, so load it now (or connect to an existing one if you
		 * have started it yourself). */
		Forker.loadDaemon();
		// Forker.connectDaemon(new Instance("NOAUTH:57872"));
		
		/* ShellBuilder is a specialisation of ForkerBuilder */
		ShellBuilder shell = new ShellBuilder();
		shell.loginShell(true);
		shell.io(PTYExecutor.PTY);
		shell.redirectErrorStream(true);
		
		/* Demonstrate we are actually in a different shell by setting PS1 */
		shell.environment().put("PS1", ">>>");
		
		/* Start the shell, giving it a window size listener */
		final Process p = shell.start(new Listener() {
			@Override
			public void windowSizeChanged(int ptyWidth, int ptyHeight) {
				System.out.println("Window size changed to " + ptyWidth + " x " + ptyHeight);
			}
		});

		
		new Thread() {
			public void run() {
				try {
					IOUtils.copy(System.in, p.getOutputStream());
				} catch (IOException e) {
				} finally {
					// Close the process input stream when stdin closes, this
					// will end the process
					try {
						p.getOutputStream().close();
					} catch (IOException e) {
					}
				}
			}
		}.start();
		IOUtils.copy(p.getInputStream(), System.out);
		int ret = p.waitFor();
		System.err.println("Exited with code: " + ret);
		System.exit(ret);
```
 