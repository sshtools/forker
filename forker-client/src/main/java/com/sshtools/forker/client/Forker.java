package com.sshtools.forker.client;

import java.io.File;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;

import com.sshtools.forker.common.Cookie;
import com.sshtools.forker.common.Cookie.Instance;
import com.sshtools.forker.common.IO;

/**
 * Replacement for {@link Runtime#exec(String)} and friends.
 * 
 * @see ForkerBuilder
 *
 */
public class Forker {

	private final static Forker INSTANCE = new Forker();
	private static boolean daemonLoaded;

	public static Forker get() {
		return INSTANCE;
	}

	public Process exec(IO io, String command) throws IOException {
		return exec(io, command, null, null);
	}

	public Process exec(IO io, String command, String[] envp) throws IOException {
		return exec(io, command, envp, null);
	}

	public Process exec(IO io, String command, String[] envp, File dir) throws IOException {
		if (command.length() == 0)
			throw new IllegalArgumentException("Empty command");

		StringTokenizer st = new StringTokenizer(command);
		String[] cmdarray = new String[st.countTokens()];
		for (int i = 0; st.hasMoreTokens(); i++)
			cmdarray[i] = st.nextToken();
		return exec(io, cmdarray, envp, dir);
	}

	public Process exec(IO io, String... cmdarray) throws IOException {
		return exec(io, cmdarray, null, null);
	}

	public Process exec(IO io, String[] cmdarray, String[] envp) throws IOException {
		return exec(io, cmdarray, envp, null);
	}

	public Process exec(IO io, String[] cmdarray, String[] envp, File dir) throws IOException {
		return new ForkerBuilder(cmdarray).io(io).environment(envp).directory(dir).start();
	}

	public static void loadDaemon() {
		if (!daemonLoaded) {

			/* Only attempt this if forker daemon is on the class path */
			try {
				Class.forName("com.sshtools.forker.daemon.Forker");

				try {
					Instance cookie = Cookie.get().load();
					if (cookie == null || !cookie.isRunning()) {
						new Thread() {
							{
								setDaemon(true);
							}

							public void run() {
								String javaExe = System.getProperty("java.home") + File.separator + "bin"
										+ File.separator + "java";
								if (SystemUtils.IS_OS_WINDOWS)
									javaExe += ".exe";
								ForkerBuilder fb = new ForkerBuilder(javaExe,
										"-Djava.library.path=" + System.getProperty("java.library.path", ""),
										"-classpath", System.getProperty("java.class.path"),
										"com.sshtools.forker.daemon.Forker");
								fb.io(IO.INPUT);
								fb.redirectErrorStream(true);
								try {
									Process p = fb.start();
									try {
										IOUtils.copy(p.getInputStream(), System.out);
									} finally {
										p.waitFor();
									}
									if (p.exitValue() != 0)
										throw new IOException(
												"Attempt to start forker daemon returned exit code " + p.exitValue());
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}.start();

						// Wait for a little bit for the daemon to start
						long now = System.currentTimeMillis();
						long expire = now + 5000;
						while (now < expire) {
							try {
								cookie = Cookie.get().load();
								if (cookie != null && cookie.isRunning())
									break;
							} catch (Exception e) {
							}
							now = System.currentTimeMillis();
						}
						if (cookie == null || !cookie.isRunning())
							throw new RuntimeException("Failed to start forker daemon.");
					}
				} catch (IOException ioe) {
					System.err.println("[WARNING] Could not load cookie, and so could not start a daemon.");
				}
			} catch (ClassNotFoundException cnfe) {

			} finally {
				// Don't try again whatever
				daemonLoaded = true;
			}
		}
	}
}
