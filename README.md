# Forker

Forker is a set of utilities and helpers for executing operating system commands. It can be used in a number of ways

 * As a 'wrapper' to execute services in Java. Similar to JSW (Java Service Wrapper) and YAJSW, Forker Wrapper can be used to launch processes in the background, track the process ID, capture output to log, automatically restart a hung or crashed JVM and more. 
   
 * As a set of static utilities to execute OS commands. Generally this is a simply case of a single call and Forker will deal with checking the exit code and redirecting or capturing standard output and error output. OS commands can be run either as the current user or as an administrator. 
   
 * A replacement to ProcessBuilder, ForkerBuilder that will uses different methods depending on the type of   I/O used, and also allows processes to be run as an administrator (or any other user). Depending on whether input, output, or I/O is needed (which should be provided as hint to the API), popen, system or a standard process will be used.
   
 * The Forker Daemon can be used to reduce forking costs on Linux, by starting a separate small JVM whose   job it is to just execute commands on behalf of another runtime. The daemon can be started as an administrator either on demand or up front (meaning the administrator password is only requested once), also allowing opening of administrator only files for reading and writing. The same daemon is also used by Forker Wrapper to provide communication between the wrapper and the wrapped application.
 
 * Execute commands and shells with a pseudo terminal (or 'pty'), providing command line editing and full interactive I/O. This is achieved using Pty4J. This could be used for example to create a Java based telnet or SSH terminal server.
 
  
## Forker Wrapper

To wrap your application, you need all 4 forker modules included in your project. 

### Maven Configuration

TODO

### Forker Wrapper Configuration

The wrapper itself can be configured in any one of four ways. All four methods provide all the same configuration options, they just allow different ways of setting. 

 1. Command line options. To wrap your application, instead of supplying it's name as the classname when you invoke the java command, instead use **com.sshtools.forker.wrapper.ForkerWrapper**, then following this by any of the command line options. At least one option must provide the actual class name that contains the main method you wish to run.  
 1. Configuration files (see -c and -C command line options). You may supply (multiple) configuration files, each of which is a simple text file that contains one option, and optionally it's value, per line. By using -C, you can specify a configuration directory where all files in that directory will be loaded.
 1. Java system properties. The key of which is option name prefixed with forker.' and with - replaced with a dot (.)
 1. Environment variables. The key of which is the option name prefixed with
'FORKER_' (in upper case) with - replaced with _

By using --help, the following will be displayed detailing all possible options.

```
usage: com.sshtools.forker.wrapper.ForkerWrapper [-a] [-A <arg>] [-b <arg>] [-c <file>] [-C <directory>] [-cp <arg>] [-d] [-e <arg>] [-E <fd>] [-F] [-h <option>] [-j <arg>] [-J <arg>] [-l <arg>] [-m <arg>] [-n] [-o] [-O <fd>] [-p <arg>] [-q] [-r <arg>] [-R <arg>] [-t <arg>] [-u <arg>] [-w <arg>] [-W <arg>] [-x <arg>] [-X <arg>] <application.class.name> [<argument> [<argument> ..]]

  -a,--administrator                           Run as administrator.
  -A,--apparg <arg>                            Application arguments. These are
                                               overridden by any application
                                               arguments provided on the command
                                               line.
  -b,--buffer-size <arg>                       How big (in byte) to make the I/O
                                               buffer. By default this is 1 byte
                                               for immediate output.
  -c,--configuration <file>                    A file to read configuration. The
                                               file should contain name=value
                                               pairs, where name is the same
                                               name as used for command line
                                               arguments (see --help for a list
                                               of these)
  -C,--configuration-directory <directory>     A directory to read configuration
                                               files from. Each file should
                                               contain name=value pairs, where
                                               name is the same name as used for
                                               command line arguments (see
                                               --help for a list of these)
  -cp,--classpath <arg>                        The classpath to use to run the
                                               application. If not set, the
                                               current runtime classpath is used
                                               (the java.class.path system
                                               property).
  -d,--daemon                                  Fork the process and exit,
                                               leaving it running in the
                                               background.
  -e,--errors <arg>                            Where to log stderr. If not
                                               specified, will be output on
                                               stderr of this process or to
                                               'log' if specified.
  -E,--fderr <fd>                              File descriptor for stderr
  -F,--no-forker-classpath                     When the forker daemon is being
                                               used, the wrappers own classpath
                                               will be appened to to the
                                               application classpath. This
                                               option prevents that behaviour
                                               for example if the application
                                               includes the modules itself.
  -h,--help <option>                           Show command line help. When the
                                               optional argument is supplied,
                                               help will be displayed for the
                                               option with that name
  -j,--java <arg>                              Alternative path to java runtime
                                               launcher.
  -J,--jvmarg <arg>                            Additional VM argument. Specify
                                               multiple times for multiple
                                               arguments.
  -l,--log <arg>                               Where to log stdout (and by
                                               default stderr) output. If not
                                               specified, will be output on
                                               stdout (or stderr) of this
                                               process.
  -m,--main <arg>                              The classname to run. If this is
                                               specified, then the first
                                               argument passed to the command
                                               becomes the first app argument.
  -n,--no-forker-daemon                        Do not enable the forker daemon.
                                               This will prevent the forked
                                               application from executing
                                               elevated commands via the daemon
                                               and will also disable JVM timeout
                                               detection.
  -o,--log-overwrite                           Overwriite logfiles instead of
                                               appending.
  -O,--fdout <fd>                              File descriptor for stdout
  -p,--pidfile <arg>                           A filename to write the process
                                               ID to. May be used by external
                                               application to obtain the PID to
                                               send signals to.
  -q,--quiet                                   Do not output anything on stderr
                                               or stdout from the wrapped
                                               process.
  -r,--restart-on <arg>                        Which exit values from the
                                               spawned process will cause the
                                               wrapper to attempt to restart it.
                                               When not specified, all exit
                                               values will cause a restart
                                               except those that are configure
                                               not to (see dont-restart-on).
  -R,--dont-restart-on <arg>                   Which exit values from the
                                               spawned process will NOT cause
                                               the wrapper to attempt to restart
                                               it. By default,this is set to 0,
                                               1 and 2. See also 'restart-on'
  -t,--timeout <arg>                           How long to wait since the last
                                               'ping' from the launched
                                               application before considering
                                               the process as hung. Requires
                                               forker daemon is enabled.
  -u,--run-as <arg>                            The user to run the application
                                               as.
  -w,--restart-wait <arg>                      How long (in seconds) to wait
                                               before attempting a restart.
  -W,--cwd <arg>                               Change working directory, the
                                               wrapped process will be run from
                                               this location.
  -x,--allow-execute <arg>                     The wrapped application can use
                                               it's wrapper to execute commands
                                               on it's behalf. If the wrapper
                                               itself runs under an
                                               administrative user, and the
                                               application as a non-privileged
                                               user,you may wish to restrict
                                               which commands may be run. One or
                                               more of these options specifies
                                               the name of the command that may
                                               be run. The value may be a
                                               regular expression, see also
                                               'prevent-execute'
  -X,--reject-execute <arg>                    The wrapped application can use
                                               it's wrapper to execute commands
                                               on it's behalf. If the wrapper
                                               itself runs under an
                                               administrative user, and the
                                               application as a non-privileged
                                               user,you may wish to restrict
                                               which commands may be run. One or
                                               more of these options specifies
                                               the name of the commands that may
                                               NOT be run. The value may be a
                                               regular expression, see also
                                               'allow-execute'
 
```