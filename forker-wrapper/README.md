# Forker Wrapper 

A 'wrapper' to execute services in Java. Similar to JSW (Java Service Wrapper) and YAJSW, Forker Wrapper can be used to launch processes in the background, track the process ID, capture output to log, automatically restart a hung or crashed JVM and more.

## Adding Forker Client To Your Project

To include the Forker Wrapper in your project, you will currently need the following repository and modules :-

### Maven

```xml
<repositories>
	<repository>
		<id>opensource-snapshots</id>
		<url>http://artifactory.javassh.com/opensource-snapshots</url>
		<name>SSHTOOLS Hosted Open Source Snapshots</name>
	</repository>
</repositories>
```

And your dependency configuration :-
    
```
<dependencies>
	<dependency>
		<groupId>com.sshtools</groupId>
		<artifactId>forker-wrapper</artifactId>
		<version>1.2.SNAPSHOT</version>
	</dependency>
</dependencies>
```

## Usage

## Quick Start

_The following requires that you have all of the Forker jars and their dependencies in the current directory. See Building Forker Libraries in the main [README.md](../README.md)  

```
java -jar forker-wrapper.jar com.nervepoint.forker.examples apparg1 apparg2 apparg3 
```

## Wrapper Configuration

The wrapper itself can be configured in any one of four ways. All four methods provide all the same configuration options, they just allow different ways of setting. 

 1. Command line options. To wrap your application, instead of supplying it's name as the classname when you invoke the java command, instead use **com.sshtools.forker.wrapper.ForkerWrapper**, then following this by any of the command line options. At least one option must provide the actual class name that contains the main method you wish to run.  
 1. Configuration files (see -c and -C command line options). You may supply (multiple) configuration files, each of which is a simple text file that contains one option, and optionally it's value, per line. By using -C, you can specify a configuration directory where all files in that directory will be loaded.
 1. Java system properties. The key of which is option name prefixed with forker.' and with - replaced with a dot (.)
 1. Environment variables. The key of which is the option name prefixed with
'FORKER_' (in upper case) with - replaced with _

By using --help, the following will be displayed detailing all possible options.

```
usage: com.sshtools.forker.wrapper.ForkerWrapper [-a] [-A <arg>] [-b <arg>] [-B <arg>] [-c <file>] [-C <directory>] [-cp <arg>] [-d]
       [-D <arg>] [-e <arg>] [-E <fd>] [-F] [-h <option>] [-I] [-j <arg>] [-J <arg>] [-l <arg>] [-L <arg>] [-m <arg>] [-M <arg>]
       [-n] [-N] [-o] [-O <fd>] [--on-application-stopped <command-or-classname>] [--on-exited-wrapper <command-or-classname>]
       [--on-exiting-wrapper <command-or-classname>] [--on-restarting-application <command-or-classname>] [--on-started-application
       <command-or-classname>] [--on-started-forker-daemon <command-or-classname>] [--on-starting-application
       <command-or-classname>] [-p <arg>] [-P <arg>] [-q <arg>] [-Q <arg>] [-r <arg>] [-R <arg>] [-S] [-s] [-t <arg>] [-u <arg>] [-w
       <arg>] [-W <arg>] [-x <arg>] [-X <arg>]
     <application.class.name> [<argument> [<argument> ..]]

Forker Wrapper is used to launch Java applications, optionally changing the user they are run as, providing automatic restarting,
signal handling and other facilities that will be useful running applications as a 'service'.

Configuration may be passed to Forker Wrapper in four different ways :-

1. Command line options.
2. Configuration files (see -c and -C options)
3. Java system properties. The key of which is option name prefixed with   'forker.' and with - replaced with a dot (.)
4. Environment variables. The key of which is the option name prefixed with   'FORKER_' (in upper case) with - replaced with _

  -a,--administrator                                        Run as administrator.
  -A,--apparg <arg>                                         Application arguments. How these are treated depends on argmode, but by
                                                            default the will be overridden by any command line arguments passed in.
  -b,--buffer-size <arg>                                    How big (in byte) to make the I/O buffer. By default this is 1 byte for
                                                            immediate output.
  -B,--cpu <arg>                                            Bind to a particular CPU, may be specified multiple times to bind to
                                                            multiple CPUs.
  -c,--configuration <file>                                 A file to read configuration. The file should contain name=value pairs,
                                                            where name is the same name as used for command line arguments (see
                                                            --help for a list of these)
  -C,--configuration-directory <directory>                  A directory to read configuration files from. Each file should contain
                                                            name=value pairs, where name is the same name as used for command line
                                                            arguments (see --help for a list of these)
  -cp,--classpath <arg>                                     The classpath to use to run the application. If not set, the current
                                                            runtime classpath is used (the java.class.path system property).
  -d,--daemon                                               Fork the process and exit, leaving it running in the background.
  -D,--log-write-delay <arg>                                In order to be compatible with external log rotation, log files are
                                                            closed as soon as they are written to. You can delay the closing of the
                                                            log file, so that any new log messages that are written within this time
                                                            will not need to open the file again. The time is in milliseconds with a
                                                            default of 50ms. A value of zero indicates to always immmediately reopen
                                                            the log.
  -e,--errors <arg>                                         Where to log stderr. If not specified, will be output on stderr of this
                                                            process or to 'log' if specified.
  -E,--fderr <fd>                                           File descriptor for stderr
  -F,--no-forker-classpath                                  When the forker daemon is being used, the wrappers own classpath will be
                                                            appened to to the application classpath. This option prevents that
                                                            behaviour for example if the application includes the modules itself.
  -h,--help <option>                                        Show command line help. When the optional argument is supplied, help
                                                            will be displayed for the option with that name
  -I,--no-info                                              Ordinary, forker will set some system properties in the wrapped
                                                            application. These communicate things such as the last exited code
                                                            (forker.info.lastExitCode), number of times start via
                                                            (forker.info.attempts) and more. This option prevents those being set.
  -j,--java <arg>                                           Alternative path to java runtime launcher.
  -J,--jvmarg <arg>                                         Additional VM argument. Specify multiple times for multiple arguments.
  -l,--log <arg>                                            Where to log stdout (and by default stderr) output. If not specified,
                                                            will be output on stdout (or stderr) of this process.
  -L,--level <arg>                                          Output level for information and debug output from wrapper itself (NOT
                                                            the application). By default this is WARNING, with other possible levels
                                                            being FINE, FINER, FINEST, SEVERE, INFO, ALL.
  -m,--main <arg>                                           The classname to run. If this is specified, then the first argument
                                                            passed to the command becomes the first app argument.
  -M,--argmode <arg>                                        Determines how apparg options are treated. May be one FORCE, APPEND,
                                                            PREPEND or DEFAULT. FORCE passed on only the appargs specified by
                                                            configuration. APPEND will append all appargs to any command line
                                                            arguments, PREPEND will prepend them. Finally DEFAULT is the default
                                                            behaviour and any command line arguments will override all appargs.
  -n,--no-forker-daemon                                     Do not enable the forker daemon. This will prevent the forked
                                                            application from executing elevated commands via the daemon and will
                                                            also disable JVM timeout detection.
  -N,--native                                               This option signals that main is not a Java classname, it is instead the
                                                            name of a native command. This option is incompatible with 'classpath'
                                                            and also means the forker daemon will not be used and so hang detection
                                                            and some other features will not be available.
  -o,--log-overwrite                                        Overwriite logfiles instead of appending.
  -O,--fdout <fd>                                           File descriptor for stdout
     --on-application-stopped <command-or-classname>        Executes a script or a Java class (that must be on wrappers own
                                                            classpath) when a particular event occurs. If a Java class is to be
                                                            execute, it must contain a main(String[] args) method. Each event may
                                                            pass a number of arguments.
     --on-exited-wrapper <command-or-classname>             Executes a script or a Java class (that must be on wrappers own
                                                            classpath) when a particular event occurs. If a Java class is to be
                                                            execute, it must contain a main(String[] args) method. Each event may
                                                            pass a number of arguments.
     --on-exiting-wrapper <command-or-classname>            Executes a script or a Java class (that must be on wrappers own
                                                            classpath) when a particular event occurs. If a Java class is to be
                                                            execute, it must contain a main(String[] args) method. Each event may
                                                            pass a number of arguments.
     --on-restarting-application <command-or-classname>     Executes a script or a Java class (that must be on wrappers own
                                                            classpath) when a particular event occurs. If a Java class is to be
                                                            execute, it must contain a main(String[] args) method. Each event may
                                                            pass a number of arguments.
     --on-started-application <command-or-classname>        Executes a script or a Java class (that must be on wrappers own
                                                            classpath) when a particular event occurs. If a Java class is to be
                                                            execute, it must contain a main(String[] args) method. Each event may
                                                            pass a number of arguments.
     --on-started-forker-daemon <command-or-classname>      Executes a script or a Java class (that must be on wrappers own
                                                            classpath) when a particular event occurs. If a Java class is to be
                                                            execute, it must contain a main(String[] args) method. Each event may
                                                            pass a number of arguments.
     --on-starting-application <command-or-classname>       Executes a script or a Java class (that must be on wrappers own
                                                            classpath) when a particular event occurs. If a Java class is to be
                                                            execute, it must contain a main(String[] args) method. Each event may
                                                            pass a number of arguments.
  -p,--pidfile <arg>                                        A filename to write the process ID to. May be used by external
                                                            application to obtain the PID to send signals to.
  -P,--priority <arg>                                       Scheduling priority, may be one of LOW, NORMAL, HIGH or REALTIME (where
                                                            supported).
  -q,--max-java <arg>                                       Maximum java version. If the selected JVM (default or otherwise) is
                                                            lower than this, an attempt will be made to locate an earlier version.
  -Q,--min-java <arg>                                       Minimum java version. If the selected JVM (default or otherwise) is
                                                            lower than this, an attempt will be made to locate a later version.
  -r,--restart-on <arg>                                     Which exit values from the spawned process will cause the wrapper to
                                                            attempt to restart it. When not specified, all exit values will cause a
                                                            restart except those that are configure not to (see dont-restart-on).
  -R,--dont-restart-on <arg>                                Which exit values from the spawned process will NOT cause the wrapper to
                                                            attempt to restart it. By default,this is set to 0, 1 and 2. See also
                                                            'restart-on'
  -S,--single-instance                                      Only allow one instance of the wrapped application to be active at any
                                                            one time. This is achieved through locked files.
  -s,--setenv                                               Set an environment on the wrapped process. This is in the format
                                                            NAME=VALUE. The option may be specified multiple times to specify
                                                            multiple environment variables.
  -t,--timeout <arg>                                        How long to wait since the last 'ping' from the launched application
                                                            before considering the process as hung. Requires forker daemon is
                                                            enabled.
  -u,--run-as <arg>                                         The user to run the application as.
  -w,--restart-wait <arg>                                   How long (in seconds) to wait before attempting a restart.
  -W,--cwd <arg>                                            Change working directory, the wrapped process will be run from this
                                                            location.
  -x,--allow-execute <arg>                                  The wrapped application can use it's wrapper to execute commands on it's
                                                            behalf. If the wrapper itself runs under an administrative user, and the
                                                            application as a non-privileged user,you may wish to restrict which
                                                            commands may be run. One or more of these options specifies the name of
                                                            the command that may be run. The value may be a regular expression, see
                                                            also 'prevent-execute'
  -X,--reject-execute <arg>                                 The wrapped application can use it's wrapper to execute commands on it's
                                                            behalf. If the wrapper itself runs under an administrative user, and the
                                                            application as a non-privileged user,you may wish to restrict which
                                                            commands may be run. One or more of these options specifies the name of
                                                            the commands that may NOT be run. The value may be a regular expression,
                                                            see also 'allow-execute'

Provided by SSHTOOLS Limited.

 
```
