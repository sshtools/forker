package com.sshtools.forker.client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.util.StringTokenizer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;

import com.sshtools.forker.common.Cookie;
import com.sshtools.forker.common.Cookie.Instance;
import com.sshtools.forker.common.IO;
import com.sshtools.forker.common.States;

/**
 * Replacement for {@link Runtime#exec(String)} and friends.
 * 
 * @see ForkerBuilder
 *
 */
public class Forker {

	private static final class ForkerDaemonThread extends Thread {
		private final EffectiveUser effectiveUser;
		private boolean isolated;
		private Process process;
		private boolean errored;

		private ForkerDaemonThread(EffectiveUser effectiveUser, boolean isolated) {
			this.effectiveUser = effectiveUser;
			this.isolated = isolated;
			setDaemon(true);
		}

		public void run() {
			String javaExe = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
			if (SystemUtils.IS_OS_WINDOWS)
				javaExe += ".exe";

			/*
			 * Build up a cut down classpath with only the jars forker daemon
			 * needs
			 */
			StringBuilder cp = new StringBuilder();
			for (String p : System.getProperty("java.class.path", "").split(File.pathSeparator)) {
				File f = new File(p);
				if (f.isDirectory()) {
					/*
					 * A directory, so this is likely in dev environment
					 */
					for (String regex : FORKER_DIRS) {
						if (f.getPath().matches(regex)) {
							if (cp.length() > 0)
								cp.append(File.pathSeparator);
							cp.append(p);
						}
					}
				} else {
					for (String regex : FORKER_JARS) {
						if (f.getName().matches(regex)) {
							if (cp.length() > 0)
								cp.append(File.pathSeparator);
							cp.append(p);
							break;
						}
					}
				}
			}

			ForkerBuilder fb = new ForkerBuilder(javaExe, "-Xmx" + System.getProperty("forker.daemon.maxMemory", "8m"),
					"-Djava.library.path=" + System.getProperty("java.library.path", ""), "-classpath", cp.toString(),
					"com.sshtools.forker.daemon.Forker");
			if (effectiveUser != null) {
				fb.effectiveUser(effectiveUser);
			}
			if (isolated) {
				fb.command().add("--isolated");
			}
			fb.io(IO.DEFAULT);
			// fb.background(true);
			fb.redirectErrorStream(true);
			try {
				process = fb.start();
				try {
					InputStream inputStream = process.getInputStream();

					if (isolated) {
						// Wait for cookie
						BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
						String line;
						while ((line = reader.readLine()) != null) {
							if (line.startsWith("FORKER-COOKIE: ")) {
								Cookie.get().set(new Instance(line.substring(15)));
								break;
							}
						}
					}

					// Now just read till it dies (or we die)
					IOUtils.copy(inputStream, System.err);
				} finally {
					process.waitFor();
				}
				if (process.exitValue() != 0)
					throw new IOException("Attempt to start forker daemon returned exit code " + process.exitValue());
			} catch (Exception e) {
				e.printStackTrace();
				errored = true;
			}
		}
	}

	/*
	 * The jars forker daemon needs. If it's dependencies ever change, this will
	 * have to updated too.
	 * 
	 * TODO Is there a better way to discover this? perhaps looking at maven
	 * meta-data
	 */
	final static String[] FORKER_JARS = { "^jna-.*", "^commons-lang-.*", "^commons-io.*", "^jna-platform-.*",
			"^purejavacomm-.*", "^guava-.*", "^log4j-.*", "^forker-common-.*", "^forker-daemon-.*", "^pty4j-.*" };
	final static String[] FORKER_DIRS = { ".*/forker-common/.*", ".*/forker-daemon/.*", ".*/pty4j/.*" };

	private final static Forker INSTANCE = new Forker();
	private static boolean daemonLoaded;
	private static boolean daemonRunning;
	private static boolean daemonAdministrator;
	private static Socket daemonMaintenanceSocket;

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

	/**
	 * Start the forker daemon (if it is not already started) using the current
	 * user.
	 */
	public static void loadDaemon() {
		loadDaemon(false);
	}

	/**
	 * Start the forker daemon (if it is not already started), optionally as an
	 * administrator. Note, great care should be taken in doing this, as it will
	 * allow subsequent processes to run as administrator with no further
	 * authentication. When <code>true</code>, the daemon started will be
	 * isolated and only usable by this runtime.
	 * 
	 * @param asAdministrator
	 */
	public static void loadDaemon(boolean asAdministrator) {
		loadDaemon(asAdministrator ? EffectiveUserFactory.getDefault().administrator() : null);
		daemonAdministrator = asAdministrator;
	}

	/**
	 * Start the forker daemon (if it is not already started) as the current
	 * user making it <b>isolated</b>, i.e. usable only by this runtime.
	 */
	public static void loadIsolatedDaemon() {
		loadDaemon(null, null, true);
	}

	/**
	 * Connect to the forker daemon running in unauthenticated mode on a known
	 * port
	 */
	public static void connectUnauthenticatedDaemon(int port) {
		connectDaemon(new Instance("NOAUTH", port));
	}

	/**
	 * Connect to the forker daemon using a known cookie. This is useful for
	 * debugging, as you can run a separate Forker daemon (as any user) and
	 * connect to it.
	 */
	public static void connectDaemon(Instance cookie) {
		loadDaemon(cookie, null, true);
	}

	/**
	 * Start the forker daemon (if it is not already started), optionally as
	 * another user. Note, great care should be taken in doing this, as it will
	 * allow subsequent processes to run as this user with no further
	 * authentication. Also, external applications that have access to the
	 * cookie file may connect to the forker daemon and ask it to run processes
	 * too.
	 * 
	 * @param effectiveUser
	 *            effective user
	 */
	public static void loadDaemon(final EffectiveUser effectiveUser) {
		loadDaemon(null, effectiveUser, effectiveUser != null);
	}

	private static void loadDaemon(Instance fixedCookie, final EffectiveUser effectiveUser, boolean isolated) {
		if (!daemonLoaded) {

			/* Only attempt this if forker daemon is on the class path */
			try {
				Class.forName("com.sshtools.forker.daemon.Forker");

				try {
					/*
					 * If running as another user, always create a new isolated
					 * forker daemon
					 */
					Instance cookie = fixedCookie == null ? (isolated ? null : Cookie.get().load()) : fixedCookie;

					boolean isRunning = cookie != null && cookie.isRunning();
					if (!isRunning) {

						if (fixedCookie != null) {
							System.err.println("[WARNING] A fixed cookie of " + fixedCookie
									+ " was provided, but a daemon for this is not running.");
							return;
						}

						final ForkerDaemonThread fdt = new ForkerDaemonThread(effectiveUser, isolated);
						fdt.start();

						// Wait for a little bit for the daemon to start
						long now = System.currentTimeMillis();
						long expire = now
								+ (Integer.parseInt(System.getProperty("forker.daemon.launchTimeout", "180")) * 1000);
						while (now < expire && !fdt.errored) {
							try {
								cookie = Cookie.get().load();
								if (cookie != null && cookie.isRunning())
									break;
							} catch (Exception e) {
							}
							now = System.currentTimeMillis();
							Thread.sleep(500);
						}
						if (cookie == null || !cookie.isRunning())
							throw new RuntimeException("Failed to start forker daemon.");

						daemonRunning = true;
						
						if (isolated) {
							/*
							 * Open a connection to the forker daemon and keep
							 * it open. When forker see this connection go down,
							 * it will shut itself down
							 * 
							 * NOTE
							 * 
							 * VERY STRANGE. Unless this Socket object has a strong reference,
							 * it will close when we leave this scope!?!?!?! As far as I know
							 * the should not happen. The symptom is forker daemon will shutdown
							 * as this socket has been closed (so any any new commands to execute
							 * will get rejected). 
							 */
							daemonMaintenanceSocket = null;
							try {
								daemonMaintenanceSocket = new Socket(InetAddress.getLocalHost(), cookie.getPort());
								DataOutputStream dos = new DataOutputStream(daemonMaintenanceSocket.getOutputStream());
								dos.writeUTF(cookie.getCookie());
								dos.writeByte(1);
								dos.flush();
								DataInputStream din = new DataInputStream(daemonMaintenanceSocket.getInputStream());
								if (din.readInt() != States.OK)
									throw new Exception("Unexpected response.");

								// Now we leave this open
							} catch (Exception e) {
								if (daemonMaintenanceSocket != null) {
									daemonMaintenanceSocket.close();
								}
							}
						}
					} else {
						daemonRunning = true;
						if (fixedCookie != null) {
							Cookie.get().set(fixedCookie);
						}
					}
				} catch (InterruptedException e) {
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

	public static boolean isDaemonLoadedAsAdministrator() {
		return daemonRunning && daemonAdministrator;
	}

	public static boolean isDaemonRunning() {
		return daemonRunning;
	}

}
