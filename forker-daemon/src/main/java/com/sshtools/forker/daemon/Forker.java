package com.sshtools.forker.daemon;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
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
import com.sshtools.forker.common.States;

/**
 * The <i>Forker Daemon</i> itself. This is responsible for accepting requests
 * from clients and executing processes on their behalf (in a separate runtime).
 * <p>
 * The daemon is secured using a {@link Cookie}, which may be used in a varierty
 * of ways.
 * <p>
 * Further functionality may be added to the daemon using {@link Handler}
 * implementations and {@link CommandExecutor} implementations.
 */
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
	private List<Client> clients = Collections.synchronizedList(new ArrayList<Client>());
	private Map<Integer, Handler> handlers = new HashMap<Integer, Handler>();

	/**
	 * Constructor
	 */
	public Forker() {
		for (Handler handler : ServiceLoader.load(Handler.class)) {
			handlers.put(handler.getType(), handler);
		}
	}

	/**
	 * Get a {@link Handler} given it's class.
	 * 
	 * @param clazz
	 *            handler class
	 * @return handler
	 */
	@SuppressWarnings("unchecked")
	public <T extends Handler> T getHandler(Class<T> clazz) {
		for (Handler h : handlers.values()) {
			if (h.getClass().equals(clazz))
				return (T) h;
		}
		return null;
	}

	/**
	 * Indicate the JVM is is forked.
	 */
	public void setForked() {
		forked = true;
	}

	/**
	 * Get whether the daemon is running in isolated mode. When isolated, the
	 * cookie is only known by the client and the forker daemon and is the
	 * default and recommended mode.
	 * 
	 * @return isolated
	 */
	public boolean isIsolated() {
		return isolated;
	}

	/**
	 * Set whether the daemon is running in isolated mode. When isolated, the
	 * cookie is only known by the client and the forker daemon and is the
	 * default and recommended mode.
	 * 
	 * @param isolated
	 *            isolated
	 */
	public void setIsolated(boolean isolated) {
		this.isolated = isolated;
	}

	/**
	 * Get a list of the currently connected clients.
	 * 
	 * @return clients
	 */
	public List<Client> getClients() {
		return clients;
	}

	/**
	 * Start an instance of the forker daemon using the provided cookie instance
	 * to secure iit,
	 * 
	 * @param thisCookie
	 *            cookie
	 * @throws IOException
	 *             on any error
	 */
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

	/**
	 * Prepare the forker daemon for use, returning the cookie that should be
	 * used for authentication.
	 * 
	 * @return cookie instance
	 * @throws IOException on any error
	 */
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
		socket.bind(new InetSocketAddress("127.0.0.1", port), backlog);

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

	/**
	 * Entry point.
	 * 
	 * @param args
	 *            command line options
	 * @throws Exception
	 *             on any error
	 */
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

	/**
	 * Copy data from a raw input stream to a forker clients data stream.
	 * 
	 * @param dout
	 *            data output stream
	 * @param stream
	 *            stream to read
	 * @param outStream
	 *            the ID of the stream (either {@link States#ERR} or
	 *            {@link States#OUT}
	 */
	public static void readStreamToOutput(final DataOutputStream dout, final InputStream stream, final int outStream) {
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

	/**
	 * Represents a single client connection. This maybe a control connection,
	 * or a request to execute a command, or any other add-on supplied
	 * operation.
	 *
	 */
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

		/**
		 * Get the type of client.
		 * 
		 * @return client type
		 */
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

		/**
		 * Close the client.
		 * 
		 * @throws IOException
		 *             on any error
		 */
		public void close() throws IOException {
			s.close();
		}

	}

	/**
	 * Shutdown the daemon, waiting for tasks to finish and exit the runtime.
	 */
	public void exit() {
		shutdown(false);
		System.exit(0);
	}

	/**
	 * Shutdown the daemon, either waiting for tasks to finish or just shutting
	 * down immediately.
	 * 
	 * @param now
	 *            shutdown now
	 */
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

	/**
	 * Deactivate the daemon, preventing an new connections but don't actually
	 * exit.
	 */
	public void deactivate() {
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
			}
		}
	}

}
