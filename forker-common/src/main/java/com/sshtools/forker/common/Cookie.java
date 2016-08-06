package com.sshtools.forker.common;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Because <i>Forker Daemon</i> may run as a privileged process (or another
 * user), it is of course sensible to have some kind of security to prevent
 * malicious processes making use of its facilities.
 * <p>
 * This protection is provided by this class which represents a 'Cookie'. When
 * clients want to connect to the daemon, they must first present the cookie.
 * <p>
 * How the client gets the cookie in the first place depends on how the daemon
 * was started.
 * <h1>Isolated Mode</h1>
 * <p>
 * This is the safest method and recommended for most uses. In this case, the
 * client application is usually responsible for launching the daemon (possibly
 * requesting administrator credentials to do so). Once launched, the daemon
 * will return it's cookie on <b>stdout</b>, allowing the client to read it an
 * make a connection back to the daemons server socket. No other processes will
 * be able to see this, so the cookie remains secret between the daemon and it's
 * client. Once launched, stdout can be used as normal.
 * <p>
 * This is all completely transparent to your code, you just need to tell Forker
 * to use isolated mode for the daemon, make the following call early in your
 * applications lifecylce. Either
 * 
 * <pre>
 * <code>Forker.loadDaemon()</code>
 * </pre>
 * 
 * to run as the same user as you are currently running as, or
 * 
 * <pre>
 * <code>Forker.loadDaemon(true)</code>
 * </pre>
 * 
 * to run the daemon as an administrator.
 */
public class Cookie {

	private static Cookie cookie = new Cookie();

	private Instance isolatedCookieInstance;

	/**
	 * Represents a single running instance of the daemon, containing its cookie
	 * and the port it is available on
	 *
	 */
	public static class Instance {
		private String cookie;
		private int port;

		/**
		 * Create a new instance given the actual cookie and the port the daemon
		 * is running on
		 * 
		 * @param cookie
		 *            cookie
		 * @param port
		 *            port
		 */
		public Instance(String cookie, int port) {
			this.cookie = cookie;
			this.port = port;
		}

		/**
		 * Create a new instance given a cookie spec string consisten of
		 * [cookie]:[port].
		 * 
		 * @param cookieText
		 *            cookie text
		 */
		public Instance(String cookieText) {
			String[] a = cookieText.split(":");
			cookie = a[0];
			port = Integer.parseInt(a[1]);
		}

		/**
		 * Get the cookie
		 * 
		 * @return cookie
		 */
		public String getCookie() {
			return cookie;
		}

		/**
		 * Get the port the daemon is running on
		 * 
		 * @return port
		 */
		public int getPort() {
			return port;
		}

		@Override
		public String toString() {
			return String.format("%s:%d", cookie, port);
		}

		/**
		 * Test whether daemon represented by this cookie instance is actually
		 * running.
		 * 
		 * @return daemon running
		 */
		public boolean isRunning() {
			try {
				final Socket s = new Socket(InetAddress.getLocalHost(), port);
				try {
					s.setSoTimeout(1000);
					DataOutputStream dout = new DataOutputStream(s.getOutputStream());
					dout.writeUTF(cookie);
					dout.write(0);
					dout.flush();
					DataInputStream din = new DataInputStream(s.getInputStream());
					int state = din.readInt();
					if (state == States.OK)
						return true;
				} finally {
					s.close();
				}
			} catch (IOException ioe) {
			}
			return false;
		}
	}

	/**
	 * Set the cookie.
	 * 
	 * @param cookie
	 *            fixed cookie
	 */
	public static void set(Cookie cookie) {
		Cookie.cookie = cookie;
	}

	/**
	 * Get the cookie. This is NOT the actual cookie instance (see
	 * {@link #load()} for that).
	 * 
	 * @return cookie
	 */
	public static Cookie get() {
		return cookie;
	}

	/**
	 * Set the fixed cookie instance. Used when daemon has been loaded by the
	 * client runtime in isolated mode.
	 * 
	 * @param isolatedCookieInstance
	 *            cookie instance
	 */
	public void set(Instance isolatedCookieInstance) {
		this.isolatedCookieInstance = isolatedCookieInstance;
	}

	/**
	 * Load the cookie to use to connect to the daemon. If a fixed instance has
	 * been set using {@link #set(Instance)}, that will be returned, otherwise
	 * an attempt will be made to locate the cookie if possible. If no cookie
	 * can be found, <code>null</code> will be returned.
	 * 
	 * @return cookie instance
	 * @throws IOException
	 *             on any error
	 */
	public Instance load() throws IOException {
		if (isolatedCookieInstance != null)
			return isolatedCookieInstance;

		try {
			BufferedReader r = new BufferedReader(new FileReader(getCookieFile()));
			try {
				return new Instance(r.readLine());
			} finally {
				r.close();
			}
		} catch (FileNotFoundException fnfe) {
			return null;
		}
	}

	/**
	 * Get the file used to store the cookie. The daemon should write to the
	 * same file.
	 * 
	 * @return cookie file
	 */
	public File getCookieFile() {
		String cookieFile = System.getProperty("forker.cookie.file");
		if (cookieFile != null) {
			return new File(cookieFile);
		}
		File dir = new File(new File(System.getProperty("user.home", ".")), ".forker");
		if (!dir.exists() && !dir.mkdirs())
			throw new RuntimeException("Could not create cookie directory " + dir);
		return new File(dir, "cookie");
	}
}
