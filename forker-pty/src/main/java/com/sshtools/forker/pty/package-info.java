/**
 * Provides the classes required to support creation of Pseudo Terminals for
 * interactive shells and other commands. This currently is only available when
 * <i>Forker Daemon</i> is installed, and requires client code set the I/O mode
 * of {@link com.sshtools.forker.pty.PTYExecutor#PTY} when using ForkerBuilder.
 * <p>
 * For example, this in conjunction with other Forker suite facilities (such as
 * as privilege escalation and run as user) could be used to create an SSH or
 * Telnet server in Java.
 */
package com.sshtools.forker.pty;