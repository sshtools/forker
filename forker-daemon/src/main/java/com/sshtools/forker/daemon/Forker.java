package com.sshtools.forker.daemon;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.pty4j.PtyProcess;
import com.sshtools.forker.common.Command;
import com.sshtools.forker.common.Defaults;
import com.sshtools.forker.common.IO;
import com.sshtools.forker.common.States;

public class Forker {

	private int port = Defaults.PORT;
	private int backlog = 10;
	private int threads = 5;
	private ExecutorService executor;

	public Forker() {

	}

	public void start() throws IOException {
		executor = Executors.newFixedThreadPool(threads);

		@SuppressWarnings("resource")
		ServerSocket s = new ServerSocket(port, backlog,
				InetAddress.getLocalHost());
		s.setReuseAddress(true);
		while (true) {
			Socket c = s.accept();
			executor.execute(new Client(c));
		}
	}

	public static void main(String[] args) throws Exception {
		Forker f = new Forker();
		f.start();
	}

	private static void handlePTYCommand(final DataInputStream din,
			final DataOutputStream dout, final Command cmd) throws IOException {

		try {

			final PtyProcess pty = PtyProcess.exec((String[]) cmd
					.getArguments().toArray(new String[0]), cmd
					.getEnvironment(), cmd.getDirectory().getAbsolutePath());

			InputStream in = pty.getInputStream();
			OutputStream out = pty.getOutputStream();
			final InputStream err = pty.getErrorStream();

			int width = pty.getWinSize().ws_col;
			int height = pty.getWinSize().ws_row;

			dout.writeInt(States.WINDOW_SIZE);
			dout.writeInt(width);
			dout.writeInt(height);
			new Thread() {
				public void run() {
					readStreamToOutput(dout, err,
							cmd.isRedirectError() ? States.OUT : States.ERR);
				}
			}.start();

			// Take any input coming the other way
			final Thread input = new InputThread(out, din) {

				@Override
				void kill() {
					pty.destroy();
				}
			};
			input.start();
			readStreamToOutput(dout, in, States.IN);
			synchronized (dout) {
				dout.writeInt(States.END);
				dout.writeInt(pty.exitValue());
				dout.flush();
			}

			// Wait for stream other end to close
			input.join();
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

	private static void readStreamToOutput(final DataOutputStream dout,
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

	private abstract static class InputThread extends Thread {
		private final DataInputStream din;
		private final OutputStream out;

		private InputThread(OutputStream out, DataInputStream din) {
			this.out = out;
			this.din = din;
		}

		abstract void kill();

		public void run() {
			try {
				boolean run = true;
				while (run) {
					int cmd = din.readInt();
					if (cmd == States.OUT) {
						int len = din.readInt();
						byte[] buf = new byte[len];
						din.readFully(buf);
						out.write(buf);
					} else if (cmd == States.KILL) {
						kill();
					} else if (cmd == States.CLOSE_OUT) {
						out.close();
						break;
					} else if (cmd == States.FLUSH_OUT) {
						out.flush();
						break;
					} else if (cmd == States.END) {
						run = false;
						break;
					} else {
						throw new IllegalStateException(
								"Unknown state code from client '" + cmd + "'");
					}
				}
			} catch (IOException ioe) {
			}
		}
	}

	private final static class Client implements Runnable {

		private Socket s;

		public Client(Socket s) {
			this.s = s;
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
					handlePTYCommand(din, dout, cmd);
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
