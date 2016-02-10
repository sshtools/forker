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

public class Cookie {

	private final static Cookie cookie = new Cookie();

	public static class Instance {
		private String cookie;
		private int port;

		public Instance(String cookie, int port) {
			this.cookie = cookie;
			this.port = port;
		}

		public Instance(String cookieText) {
			String[] a = cookieText.split(":");
			cookie = a[0];
			port = Integer.parseInt(a[1]);
		}

		public String getCookie() {
			return cookie;
		}

		public int getPort() {
			return port;
		}

		public String toString() {
			return String.format("%s:%d", cookie, port);
		}

		public boolean isRunning() {
			try {
				final Socket s = new Socket(InetAddress.getLocalHost(), port);
				try {
					s.setSoTimeout(1000);
					DataOutputStream dout = new DataOutputStream(s.getOutputStream());
					dout.writeUTF(cookie);
					DataInputStream din = new DataInputStream(s.getInputStream());
					if (din.readInt() == States.OK)
						return true;
				} finally {
					s.close();
				}
			} catch (IOException ioe) {
			}
			return false;
		}
	}

	public static Cookie get() {
		return cookie;
	}

	public Instance load() throws IOException {
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

	public File getCookieFile() {
		File dir = new File(new File(System.getProperty("user.home", ".")), ".forker");
		if (!dir.exists() && !dir.mkdirs())
			throw new RuntimeException("Could not create cookie directory " + dir);
		return new File(dir, "cookie");
	}
}
