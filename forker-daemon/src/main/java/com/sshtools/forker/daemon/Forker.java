package com.sshtools.forker.daemon;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;

import com.pty4j.PtyProcess;
import com.pty4j.WinSize;
import com.sshtools.forker.common.CSystem;
import com.sshtools.forker.common.Command;
import com.sshtools.forker.common.Cookie;
import com.sshtools.forker.common.Cookie.Instance;
import com.sshtools.forker.common.IO;
import com.sshtools.forker.common.States;
import com.sun.jna.Platform;

public class Forker {

	// private int port = Defaults.PORT;
	private int port = 0;
	private int backlog = 10;
	private int threads = 5;
	private ExecutorService executor;
	private ServerSocket socket;
	private boolean forked;

	public Forker() {

	}

	public void start() throws IOException {
		/*
		 * First look to see if there is an existing cookie, and if so, is the
		 * daemon still running. If it is, we don't need this one
		 */
		Instance cookie = Cookie.get().load();
		if (cookie != null && cookie.isRunning()) {
			throw new IOException("A Forker daemon is already on port " + cookie.getPort());
		}

		executor = Executors.newFixedThreadPool(threads);

		socket = new ServerSocket();
		socket.setReuseAddress(true);
		socket.bind(new InetSocketAddress(InetAddress.getLocalHost(), port), backlog);

		/*
		 * Create a new cookie for clients to be able to locate this daemon
		 */
		Instance thisCookie = new Instance(UUID.randomUUID().toString(), socket.getLocalPort());
		save(thisCookie);

		try {
			while (true) {
				Socket c = socket.accept();
				executor.execute(new Client(this, c, thisCookie));
			}
		} catch (IOException ioe) {
			// Ignore exception if this is forked JVM shutting down the socket,
			if (!forked) {
				throw ioe;
			}
		}
	}

	public static void main(String[] args) throws Exception {
		// BasicConfigurator.configure();
		Forker f = new Forker();
		f.start();
	}

	private void save(Instance cookie) throws IOException {
		File cookieFile = Cookie.get().getCookieFile();
		cookieFile.deleteOnExit();
		PrintWriter pw = new PrintWriter(new FileWriter(cookieFile), true);
		try {
			pw.println(cookie.toString());

			/* Try to set permissions so only the current user can access it */
			try {
				Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
				perms.add(PosixFilePermission.OWNER_READ);
				perms.add(PosixFilePermission.OWNER_WRITE);
				perms.add(PosixFilePermission.OWNER_EXECUTE);
				Files.setPosixFilePermissions(Paths.get(cookieFile.toURI()), perms);
			} catch (Exception e) {
				System.err.println(
						"[FATAL] Could not reduce file permission of cookie file %s. Other users may be able to execute processes under this user!");
				System.exit(3);
			}
		} finally {
			pw.close();
		}
	}

	private static void handlePTYCommand(Forker forker, final DataInputStream din, final DataOutputStream dout,
			final Command cmd) throws IOException {

		try {
			/*
			 * HACK! Make sure the classes are loaded now so that there are not
			 * problems when it's forked
			 * 
			 * These don't actually get used
			 */
			OutputThread outThread = new OutputThread(null, null, null);
			InputThread inThread = new InputThread(null, null) {
				@Override
				void kill() {
				}

				@Override
				void setWindowSize(int width, int height) {
					new WinSize();
				}
			};

			// Change the EUID before we fork
			int euidWas = -1;
			if (!StringUtils.isBlank(cmd.getRunAs())) {
				if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC_OSX) {
					euidWas = CSystem.INSTANCE.geteuid();
					int euid = Integer.parseInt(cmd.getRunAs());
					if (CSystem.INSTANCE.seteuid(euid) == -1) {
						// TODO get errono
						throw new RuntimeException("Failed to set EUID.");
					}
				}
			}

			PtyProcess ptyorig = null;
			try {
				// If Windows, and we are starting a shell, strip this commands
				if (Platform.isWindows() && cmd.getArguments().size() > 2 && cmd.getArguments().get(0).equals("start")
						&& cmd.getArguments().get(1).equals("/c") && cmd.getArguments().get(2).equals("CMD.exe")) {
					cmd.getArguments().remove(0);
					cmd.getArguments().remove(0);
				}

				ptyorig = PtyProcess.exec((String[]) cmd.getArguments().toArray(new String[0]), cmd.getEnvironment(),
						cmd.getDirectory().getAbsolutePath());
			} finally {
				// And return to previous ID
				if (euidWas != -1) {
					if (CSystem.INSTANCE.seteuid(euidWas) == -1) {
						// TODO get errono
						throw new RuntimeException("Failed to set EUID.");
					}
				}
			}
			final PtyProcess pty = ptyorig;

			// The JVM is now forked, so free up some resources we won't
			// actually use
			forker.forked = true;
			// forker.socket.close();

			InputStream in = pty.getInputStream();
			OutputStream out = pty.getOutputStream();
			final InputStream err = pty.getErrorStream();
			
			WinSize winSize = pty.getWinSize();
			int width = winSize == null ? 80 : winSize.ws_col;
			int height = winSize == null ? 24 : winSize.ws_row;

			dout.writeInt(States.WINDOW_SIZE);
			dout.writeInt(width);
			dout.writeInt(height);
			outThread = new OutputThread(dout, cmd, err);
			outThread.start();

			// Take any input coming the other way
			inThread = new InputThread(out, din) {

				@Override
				void kill() {
					pty.destroy();
				}

				@Override
				void setWindowSize(int width, int height) {
					pty.setWinSize(new WinSize(width, height));
				}
			};
			inThread.start();
			readStreamToOutput(dout, in, States.IN);
			synchronized (dout) {
				dout.writeInt(States.END);
				dout.writeInt(pty.exitValue());
				dout.flush();
			}

			// Wait for stream other end to close
			inThread.join();
		} catch (Throwable t) {
			t.printStackTrace();
			synchronized (dout) {
				dout.writeInt(States.FAILED);
				dout.writeUTF(t.getMessage());
			}
		}
	}

	private static void handleStandardCommand(final DataInputStream din, final DataOutputStream dout, Command cmd)
			throws IOException {
		ProcessBuilder builder = new ProcessBuilder(cmd.getArguments());
		if (cmd.getEnvironment() != null) {
			builder.environment().putAll(cmd.getEnvironment());
		}
		builder.directory(cmd.getDirectory());
		if (cmd.isRedirectError()) {
			builder.redirectErrorStream(true);
		}

		try {
			final Process process = builder.start();
			dout.writeInt(States.OK);
			if (!cmd.isRedirectError()) {
				new Thread() {
					public void run() {
						readStreamToOutput(dout, process.getErrorStream(), States.ERR);
					}
				}.start();
			}

			// Take any input coming the other way
			final Thread input = new InputThread(process.getOutputStream(), din) {
				@Override
				void kill() {
					process.destroy();
				}

				@Override
				void setWindowSize(int width, int height) {
				}
			};
			input.start();
			readStreamToOutput(dout, process.getInputStream(), States.IN);
			synchronized (dout) {
				dout.writeInt(States.END);
				dout.writeInt(process.exitValue());
				dout.flush();
			}

			// Wait for stream other end to close
			input.join();
		} catch (Throwable t) {
			synchronized (dout) {
				dout.writeInt(States.FAILED);
				dout.writeUTF(t.getMessage());
			}
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

	private final static class Client implements Runnable {

		private Socket s;
		private Forker forker;
		private Instance cookie;

		public Client(Forker forker, Socket s, Cookie.Instance cookie) {
			this.s = s;
			this.forker = forker;
			this.cookie = cookie;
		}

		@Override
		public void run() {

			try {
				final DataInputStream din = new DataInputStream(s.getInputStream());
				String clientCookie = din.readUTF();
				if (!cookie.getCookie().equals(clientCookie)) {
					throw new Exception("Invalid cookie. (got " + clientCookie + ", expected " + cookie.getCookie());
				}
				final DataOutputStream dout = new DataOutputStream(s.getOutputStream());
				dout.writeInt(States.OK);
				dout.flush();
				Command cmd = new Command(din);
				if (cmd.getIO() == IO.PTY)
					handlePTYCommand(forker, din, dout, cmd);
				else
					handleStandardCommand(din, dout, cmd);
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
			}
		}

	}
}
