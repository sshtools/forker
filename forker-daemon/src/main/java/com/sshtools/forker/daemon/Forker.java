package com.sshtools.forker.daemon;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.SystemUtils;

import com.sshtools.forker.common.Cookie;
import com.sshtools.forker.common.Cookie.Instance;

public class Forker {

	// private int port = Defaults.PORT;
	private int port = 0;
	private int backlog = 10;
	private int threads = 5;
	private ExecutorService executor;
	private ServerSocket socket;
	private boolean forked;
	private boolean isolated;
	private boolean unauthenticated;
	private List<Client> clients = new ArrayList<>();
	private Map<Integer, Handler> handlers = new HashMap<>();

	public Forker() {
		clients = Collections.synchronizedList(clients);
		for (Handler handler : ServiceLoader.load(Handler.class)) {
			handlers.put(handler.getType(), handler);
		}
	}

	@SuppressWarnings("unchecked")
	public <T extends Handler> T getHandler(Class<T> handler) {
		for (Handler h : handlers.values()) {
			if (h.getClass().equals(handler))
				return (T) h;
		}
		return null;
	}

	void setForked() {
		forked = true;
	}

	public boolean isIsolated() {
		return isolated;
	}

	public void setIsolated(boolean isolated) {
		this.isolated = isolated;
	}

	public List<Client> getClients() {
		return clients;
	}

	public void start(Instance thisCookie) throws IOException {
		try {
			while (true) {
				Socket c = socket.accept();
				Client client = new Client(this, c, thisCookie);
				clients.add(client);
				executor.execute(client);
			}
		} catch (IOException ioe) {
			// Ignore exception if this is forked JVM shutting down the socket,
			if (!forked) {
				throw ioe;
			}
		}
	}

	public Instance prepare() throws IOException {
		/*
		 * First look to see if there is an existing cookie, and if so, is the
		 * daemon still running. If it is, we don't need this one
		 */
		Instance cookie = isolated ? null : Cookie.get().load();
		if (cookie != null && cookie.isRunning()) {
			throw new IOException("A Forker daemon is already on port " + cookie.getPort());
		}

		executor = Executors.newFixedThreadPool(threads);

		socket = new ServerSocket();
		socket.setReuseAddress(true);
		socket.bind(new InetSocketAddress(InetAddress.getLocalHost(), port), backlog);

		return createCookie();
	}

	protected Instance createCookie() throws IOException {
		/*
		 * Create a new cookie for clients to be able to locate this daemon
		 */
		Instance thisCookie = new Instance(UUID.randomUUID().toString(), socket.getLocalPort());
		if (!unauthenticated) {
			save(thisCookie);
		} else if (!isolated) {
			System.out.println(
					"[WARNING] Forker daemon is running in unauthenticated mode. This should not be used for production use. Use the cookie NOAUTH:"
							+ socket.getLocalPort());
		}
		return thisCookie;
	}

	public static void main(String[] args) throws Exception {
		// BasicConfigurator.configure();
		Forker f = new Forker();
		for (String a : args) {
			if (a.equals("--isolated")) {
				f.isolated = true;
			} else if (a.equals("--unauthenticated")) {
				f.unauthenticated = true;
			} else if (a.startsWith("--port=")) {
				f.port = Integer.parseInt(a.substring(7));
			}
		}
		Instance cookie = f.prepare();
		if (f.isolated) {
			System.out.println("FORKER-COOKIE: " + cookie.toString());
		}
		f.start(cookie);
	}

	private void save(Instance cookie) throws IOException {
		File cookieFile = Cookie.get().getCookieFile();
		cookieFile.deleteOnExit();
		PrintWriter pw = new PrintWriter(new FileWriter(cookieFile), true);
		try {
			pw.println(cookie.toString());

			/* Try to set permissions so only the current user can access it */
			if (SystemUtils.IS_OS_UNIX) {
				try {
					Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
					perms.add(PosixFilePermission.OWNER_READ);
					perms.add(PosixFilePermission.OWNER_WRITE);
					perms.add(PosixFilePermission.OWNER_EXECUTE);
					Files.setPosixFilePermissions(Paths.get(cookieFile.toURI()), perms);
				} catch (Exception e) {
					System.err.println(String.format(
							"[FATAL] Could not reduce file permission of cookie file %s. Other users may be able to execute processes under this user!",
							cookieFile));
					System.exit(3);
				}
			} else {
				System.err.println(String.format(
						"[SERIOUS] Could not reduce file permission of cookie file %s. Other users may be able to execute processes under this user!",
						cookieFile));
			}
		} finally {
			pw.close();
		}
	}

	static void readStreamToOutput(final DataOutputStream dout, final InputStream stream, final int outStream) {
		// Capture stdout if not already doing so via
		// ProcessBuilder
		try {
			int r;
			byte[] buf = new byte[65536];
			while ((r = stream.read(buf)) != -1) {
				synchronized (dout) {
					dout.writeInt(outStream);
					dout.writeInt(r);
					dout.write(buf, 0, r);
				}
			}
		} catch (IOException ioe) {
			//
		}
	}

	public final static class Client implements Runnable {

		private Socket s;
		private Forker forker;
		private Instance cookie;
		private int type;

		private Client(Forker forker, Socket s, Cookie.Instance cookie) {
			this.s = s;
			this.forker = forker;
			this.cookie = cookie;
		}

		public int getType() {
			return type;
		}

		@Override
		public void run() {

			try {
				final DataInputStream din = new DataInputStream(s.getInputStream());
				String clientCookie = din.readUTF();
				if (!forker.unauthenticated && !cookie.getCookie().equals(clientCookie)) {
					System.out.println(
							"[WARNING] Invalid cookie. (got " + clientCookie + ", expected " + cookie.getCookie());
					throw new Exception("Invalid cookie. (got " + clientCookie + ", expected " + cookie.getCookie());
				}
				final DataOutputStream dout = new DataOutputStream(s.getOutputStream());

				//
				type = din.readByte();
				Handler handler = forker.handlers.get(type);
				if (handler == null)
					throw new IOException("No handler for type " + type);

				handler.handle(forker, din, dout);
			} catch (EOFException eof) {
				// Normal
			} catch (Exception ioe) {
				System.err.println("Forker client I/O failed.");
				ioe.printStackTrace();
			} finally {
				try {
					s.close();
				} catch (IOException e) {
				}
				forker.clients.remove(this);
			}
		}

		public void close() throws IOException {
			s.close();
		}

	}

	public void exit() {
		shutdown(false);
		System.exit(0);
	}

	public void shutdown(boolean now) {
		for (Handler h : handlers.values())
			h.stop();
		if (now)
			executor.shutdownNow();
		else
			executor.shutdown();
		try {
			executor.awaitTermination(60, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		}
	}

	public void deactivate() {
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
			}
		}
	}

}
