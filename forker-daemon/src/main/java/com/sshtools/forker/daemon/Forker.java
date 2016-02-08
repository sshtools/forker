package com.sshtools.forker.daemon;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.BasicConfigurator;

import com.pty4j.PtyProcess;
import com.sshtools.forker.common.CSystem;
import com.sshtools.forker.common.Command;
import com.sshtools.forker.common.Defaults;
import com.sshtools.forker.common.IO;
import com.sshtools.forker.common.States;

public class Forker {

	private int port = Defaults.PORT;
	private int backlog = 10;
	private int threads = 5;
	private ExecutorService executor;
	private ServerSocket socket;

	public Forker() {

	}

	public void start() throws IOException {
		executor = Executors.newFixedThreadPool(threads);

		socket = new ServerSocket();
		socket.setReuseAddress(true);
		socket.bind(new InetSocketAddress(InetAddress.getLocalHost(), port), backlog);
		while (true) {
			Socket c = socket.accept();
			executor.execute(new Client(this, c));
		}
	}

	public static void main(String[] args) throws Exception {
		BasicConfigurator.configure();
		Forker f = new Forker();
		f.start();
	}

	private static void handlePTYCommand(Forker forker,
			final DataInputStream din, final DataOutputStream dout,
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
					System.out.println("RUN AS " + euid);
				}
			}

			PtyProcess ptyorig = null;
			try {
				ptyorig = PtyProcess.exec((String[]) cmd.getArguments()
						.toArray(new String[0]), cmd.getEnvironment(), cmd
						.getDirectory().getAbsolutePath());
			} finally {
				// And return to previous ID
				if (euidWas != -1) {
					if (CSystem.INSTANCE.seteuid(euidWas) == -1) {
						// TODO get errono
						throw new RuntimeException("Failed to set EUID.");
					}
					System.out.println("NOW RUNNING AS " + euidWas);
				}
			}
			final PtyProcess pty = ptyorig;

			// The JVM is now forked, so free up some resources we won't
			// actually use
			forker.socket.close();

			InputStream in = pty.getInputStream();
			OutputStream out = pty.getOutputStream();
			final InputStream err = pty.getErrorStream();

			int width = pty.getWinSize().ws_col;
			int height = pty.getWinSize().ws_row;

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

	private static void handleStandardCommand(final DataInputStream din,
			final DataOutputStream dout, Command cmd) throws IOException {
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
						readStreamToOutput(dout, process.getErrorStream(),
								States.ERR);
					}
				}.start();
			}

			// Take any input coming the other way
			final Thread input = new InputThread(process.getOutputStream(), din) {
				@Override
				void kill() {
					process.destroy();
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

	static void readStreamToOutput(final DataOutputStream dout,
			final InputStream stream, final int outStream) {
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

		public Client(Forker forker, Socket s) {
			this.s = s;
			this.forker = forker;
		}

		@Override
		public void run() {

			try {
				final DataInputStream din = new DataInputStream(
						s.getInputStream());
				final DataOutputStream dout = new DataOutputStream(
						s.getOutputStream());
				Command cmd = new Command(din);
				System.out.println(cmd);
				if (cmd.getIO() == IO.PTY)
					handlePTYCommand(forker, din, dout, cmd);
				else
					handleStandardCommand(din, dout, cmd);
			} catch (IOException ioe) {
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
