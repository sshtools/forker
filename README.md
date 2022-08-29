# Forker

Forker is a set of utilities and helpers for executing operating system commands from Java. It can be used in a number of ways :-
 
## Forker Client
  
Forker Client provides a set of utilities, *OSCommand*, and the ProcessBuilder replacement *ForkerBuilder*. See [forker-client/README.md](forker-client/README.md) for more information.

### OSCommand

Generally this is a simply case of a single call and Forker will deal with checking the exit code and redirecting or capturing standard output and error output. OS commands can be run either as the current user or as an administrator. 
   
### ForkerBuilder 
The replacement to ProcessBuilder, ForkerBuilder uses different methods depending on the type of  I/O used, and also allows processes to be run as an administrator (or any other user). Depending on whether input, output, or I/O is needed (which should be provided as hint to the API), popen, system or a standard process will be used. 

A very useful feature is the ability to use non-blocking
I/O on supported platforms, which can make for cleaner code and much better memory usage when launching lots of processes. 
This feature also uses *vfork* on Linux, which doesn't have the high fork cost associated with standard Java.

### Pseudo Terminal Support

Execute commands and shells with a pseudo terminal (or 'pty'), providing command line editing and full interactive I/O. This is achieved using Pty4J. This could be used for example to create a Java based telnet or SSH terminal server. 

See [forker-pty/README.md](forker-pty/README.md) 

## Forker Wrapper

A 'wrapper' to execute services in Java. Similar to JSW (Java Service Wrapper) and YAJSW, Forker Wrapper can be used to launch processes in the background, track the process ID, capture output to log, automatically restart a hung or crashed JVM and more.
Forker Wrapper is lightweight and powerful.

See [forker-wrapper/README.md](forker-wrapper/README.md)

## Forker Updater

Builds on Forker Wrapper to provide an Install / Update system. Currently Linux only for all features, Windows support in progress.

See [forker-updater/README.md](forker-updater/README.md)

There are several modules that cover Updater's functionality.

 * forker-updater - The core
 * forker-updater-console - Plugin for console install / updates
 * forker-updater-swt - Plugin for GUI  install / updates (WIP)
 * forker-updater-example - An example installable and updateable application.
 * forker-updater-maven-plugin - Build updater images and bootstrap installers.

## Forker Services

This allows you to control local system services in a cross platform way. Support is provided for Linux and Windows
currently, and allows enumerating of services and their states, as well as control services and configuring their start on boot setting.

See [forker-services/README.md](forker-services/README.md).

## Forker Pipes

Cross platform API to pipe-like OS specific streams (i.e. Unix Sockets on Linux, Named Pipes on
Windows).

See [forker-pipes/README.md](forker-pipes/README.md).
