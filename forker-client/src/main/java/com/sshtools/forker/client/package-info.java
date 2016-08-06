/**
 * This packages contains the main classes that will be used in client code
 * making use of the <i>Forker</i> framework.
 * <p>
 * The three classes you are most likely to want to use include :-
 * <ul>
 * <li>{@link com.sshtools.forker.client.ForkerBuilder} - which is an API
 * compatible replacement for {@link java.lang.ProcessBuilder}.</li>
 * <li>{@link com.sshtools.forker.client.OSCommand} - which contains a set of
 * static helper methods to make running processes easy, taking care of checking
 * exit codes and redirecting I/O</li>
 * <li>{@link com.sshtools.forker.client.ShellBuilder} - which is a
 * specialisation of ForkerBuilder and can be used to create an interactive
 * shell (often used with the <i>Forker Pty</i> module).
 * </ul>
 */
package com.sshtools.forker.client;