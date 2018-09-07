# Forker Client

## Introduction

Forker Client provides a set of utilities, *OSCommand*, and the ProcessBuilder replacement *ForkerBuilder*. 
   
## Adding Forker Client To Your Project

To include the Forker utilities in your project, you will need the module :-

### Maven

```
<dependencies>
	<dependency>
		<groupId>com.sshtools</groupId>
		<artifactId>forker-client</artifactId>
		<version>1.5</version>
	</dependency>
</dependencies>
```

## Usage

### OSCommand

Generally this is a simply case of a single call and Forker will deal with checking the exit code and redirecting or capturing standard output and error output. OS commands can be run either as the current user or as an administrator.

#### Running Commands

```java
/* Run a simple command, the first argument is command to run (on the PATH),
 * all subsequent arguments are passed to the command. If the exit value
 * is anything other than zero, an IOException will be thrown.
 */
OSCommand.run("cp", "/tmp/file1", "/tmp/file2");

/* Many methods also take a list .. */
List<String> args = new ArrayList<>();
args.add("cp");
args.addAll(filenameList);
args.add(targetDir);
OSCommand.run(args);

/* If you want to capture the exit code, use runCommand() */
if(OSCommand.runCommand("cp", "/tmp/file1", "/tmp/file") != 0) {
    System.err.println("File copy failed");
}

/* If you want to capture the output of a command (for short output) .. */
for(String userline : OSCommand.runCommandAndCaptureOutput("cat", "/etc/passwd")) {
    System.out.println(">>>> " + userline);  
}

/* If you want to capture the output of a command that is gong to output a lot,
   you are better off using the 'iterate' varient. These look similar, but
   the former is returning a List, the latter an Iterable. */
for(String userline : OSCommand.runCommandAndIterateOutput("find", "/")) {
    System.out.println(">>>> " + userline);  
}

/* If you want to capture the output of a command to a file .. */
OSCommand.runCommandAndOutputToFile(new File("/tmp/mysqld.dump"), "mysqldump", "--add-drop-tables", "mydatabase");

/* .. or to a stream, setting the working directory of the command */
ByteArrayOutputStream buffer = new ByteArrayOutputStream();
OSCommand.run(new File("/tmp", buffer, "cat", "temp1") throws IOException {

```
#### Running Commands As Administrator

```java
/* Simple run as administrator. User will be prompted for administrator password (their own, the administrators, or perhaps a UAC prompt depending on the operating system and configuration.
Most of the 'run' methods have 'admin' versions too */
OSCommand.admin("cat", "/etc/shadow");

/* You might want to sometimes run a command or group of commands as administrator. In this case you can elevate to admin
and then return to user level as required (remember to always do some, once elevated, your current thread stays
elevated for all future commands. */

OSCommand.elevate();
try {
    OSCommand.run("cp", "/tmp/file1", "/tmp/file2");
    OSCommand.run("cp", "-R", "/tmp/dir1", "/tmp/dir2");
    OSCommand.run("rm", "/tmp/file1");
}
finally {
    OSCommand.restrict();
}

/* If using Java8, you can use try-with-resource for a more compact form :- */
try(OSCommand.elevated()) {
    OSCommand.run("cp", "/tmp/file1", "/tmp/file2");
    OSCommand.run("cp", "-R", "/tmp/dir1", "/tmp/dir2");
    OSCommand.run("rm", "/tmp/file1");
}

/* Being prompted for the password every time a system command is need is probably 
   not what you want. You can use the Forker Daemon to help with this  .. */
   
Forker.loadDaemon(true); // Will prompt for admin password
OSCommand.run("id"); // Will run as normal user
OSCommand.admin("id"); // Will run as admin without a prompt  
System.out.println(OSCommand.adminCommandAndCaptureOutput("cat", "/etc/shadow")); // Will run as admin without a prompt
```

### ForkerBuilder 

The replacement to ProcessBuilder, ForkerBuilder uses different methods depending on the type of  I/O used, and also allows processes to be run as an administrator (or any other user). Depending on whether input, output, or I/O is needed (which should be provided as hint to the API), popen, system or a standard process will be used, as well as the ability to use non-blocking
I/O.

#### Running using non-blocking I/O

One major feature of ForkerBuilder is the ability to use non-blocking I/O. Depending on how many processes you will be launching
this can have major benefits, as it avoids the need to create one, two or even sometimes 3 threads that may be necessary
for an ordinary ProcessBuilder process.


```java
ForkerBuilder builder = new ForkerBuilder().io(IO.NON_BLOCKING).redirectErrorStream(true);
if (SystemUtils.IS_OS_UNIX) {
	// The unix example tries to list the root directory
	builder.command("ls", "-al", "/");
} else {
	builder.command("DIR", "C:\\");
}
Process process = builder.start(new DefaultNonBlockingProcessListener() {
	@Override
	public void onStdout(NonBlockingProcess process, ByteBuffer buffer, boolean closed) {
		if (!closed) {
			byte[] bytes = new byte[buffer.remaining()];
			/* Consume bytes from buffer (so position is updated) */
			buffer.get(bytes);
			System.out.println(new String(bytes));
		}
	}
});
```

The same listener can be used for getting notifications of process exit, errors, or stderr (if you 
are not using *redirectErrorStream(true)*).

```java

// ....
Process process = builder.start(new DefaultNonBlockingProcessListener() {
	@Override
	public void onError(Exception exception, NonBlockingProcess process, boolean existing) {
		// Got an error on the handler thread
	}

	@Override
	public void onExit(int exitCode, NonBlockingProcess process) {
		// Process has exited
	}

	@Override
	public void onStderr(NonBlockingProcess process, ByteBuffer buffer, boolean closed) {
		// Have received some stderr. React in same way as you would for onStdout()
	}
});
```

Again, this listener can be used for dealing with *stdin* in a non-blocking fashion too. For this, 
you override *onStdinReady*.

```java

// ....
Process process = builder.start(new DefaultNonBlockingProcessListener() {
	@Override
	public boolean onStdinReady(NonBlockingProcess process, ByteBuffer buffer) {
	
		// Put data to write in the buffer, making sure you flip the buffer before exiting
		
		buffer.put("Data to send to stdin...".getBytes());
		buffer.flip();
		
		/*
		 * Return false to indicate there is no more to write. Return
		 * true and this method will be called again when there is space
		 * available (and so on until false is returned).
		 */
		return false;
	}
});
```

Then when you want some stdin to be written ..

```java
process.wantWrite();
```

You can also send data to a non-blocking processes stdin using either the standard *Process.getOutputStream()* 
and write to that (you must *OutputStream.flush()* to flush the data to the process when doing it this way), or you can write directly using a *ByteBuffer* :-

```java
process.writeStdin(ByteBuffer.wrap("This is sent directly\n".getBytes()));
```

#### Running shells and scripts

If you want to run a shell (useful with the PTY extension), or a shell script, you might want to use [ShellBuilder](src/main/java/com/sshtools/forker/client/ShellBuilder.java) instead. This will automatically wrap your shell script path with the appropriate commands to use the native shell (bash, CMD.exe or whatever).

```java

		/* ShellBuilder is a specialisation of ForkerBuilder. The script path does not
		   need to be executable. */
		   
		ShellBuilder shell = new ShellBuilder("/home/myuser/ascript.sh");
		shell.redirectErrorStream(true);
		
		/* Demonstrate we are actually in a different shell by setting PS1 */
		shell.environment().put("MYENV", "An environment variable");
		
		/* Start the scription, giving it a window size listener */
		process = shell.start();
		
```