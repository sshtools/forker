package com.sshtools.forker.client.impl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import com.sshtools.forker.client.AbstractForkerProcess;
import com.sshtools.forker.common.Command;
import com.sshtools.forker.common.Cookie;
import com.sshtools.forker.common.Cookie.Instance;
import com.sshtools.forker.common.IO;
import com.sshtools.forker.common.States;

/**
 * Uses <i>Forker Daemon</i> to actually run the process in a remote JVM
 * (possibly as a different user). The {@link Command} will be serialised and
 * sent to the daemon.
 */
public class ForkerDaemonProcess extends AbstractForkerProcess {

	/**
	 * How output to stdin is flushed.
	 */
	public enum OutputFlushMode {
		/**
		 * Streams flush() method must be called manually
		 */
		MANUAL,
		/**
		 * Determined by {@link IO} mode.
		 */
		AUTO,
		/**
		 * Flushed to daemon on every write
		 */
		LOCAL,
		/**
		 * Flushed to daemon, then to process on every write
		 */
		BOTH
	}

	private DataOutputStream dout;
	private DataInputStream din;
	private OutputStream out;
	private PipedOutputStream inOut;
	private PipedOutputStream errOut;
	private PipedInputStream in;
	private PipedInputStream err;
	private Thread thread;
	private int exitValue = Integer.MIN_VALUE;
	private int ptyWidth;
	private int ptyHeight;
	private List<Listener> listeners = new ArrayList<Listener>();
	private Command command;
	private OutputFlushMode outputFlushMode = OutputFlushMode.AUTO;

	/**
	 * Listen to be implemented to be notified of pseudo terminal window size
	 * changes
	 *
	 */
	public interface Listener {
		/**
		 * Pseudo terminal window size has changed.
		 * 
		 * @param ptyWidth
		 *            width
		 * @param ptyHeight
		 *            height
		 */
		void windowSizeChanged(int ptyWidth, int ptyHeight);
	}

	/**
	 * Command
	 * 
	 * @param command
	 *            command
	 */
	public ForkerDaemonProcess(Command command) {
		this.command = command;
	}

	/**
	 * Start the process.
	 * 
	 * @throws IOException on any error
	 */
	public void start() throws IOException {

		Instance cookie = Cookie.get().load();
		if (cookie == null) {
			throw new ConnectException("The forker daemon is not running.");
		}

		final Socket s = new Socket(InetAddress.getLocalHost(), cookie.getPort());
		boolean ok = false;

		try {
			dout = new DataOutputStream(s.getOutputStream());
			din = new DataInputStream(s.getInputStream());

			// Coookie
			dout.writeUTF(cookie.getCookie());

			// Normal mode
			dout.write(0);
			dout.flush();

			int result = din.readInt();
			if (result == States.FAILED) {
				throw new IOException("Cookie rejected.");
			}

			// Command
			command.write(dout);
			dout.flush();
			result = din.readInt();
			if (result == States.FAILED) {
				String mesg = din.readUTF();
				System.out.println("FORK: Command failed " + mesg);
				try {
					dout.close();
				} catch (IOException ioe) {
				}
				try {
					din.close();
				} catch (IOException ioe) {
				}
				throw new IOException(mesg);
			}
			if (result == States.WINDOW_SIZE) {
				ptyWidth = din.readInt();
				ptyHeight = din.readInt();
			}

			inOut = new PipedOutputStream();
			errOut = new PipedOutputStream();

			in = new PipedInputStream(inOut) {

				@Override
				public void close() throws IOException {
					super.close();
					synchronized (this) {
						this.notifyAll();
					}
				}

			};
			err = new PipedInputStream(errOut);

			ok = true;
		} finally {
			if (!ok)
				s.close();
		}

		thread = new Thread("ForkerIn" + command.getArguments()) {

			public void run() {
				try {
					boolean run = true;
					while (run) {
						int cmd = din.readInt();
						switch (cmd) {
						case States.WINDOW_SIZE:
							ptyWidth = din.readInt();
							ptyHeight = din.readInt();
							windowSizeChange(ptyWidth, ptyHeight);
							break;
						case States.END:
							// Do not close ourselves when we break out, the
							// daemon end will close the socket
							run = false;
							exitValue = din.readInt();
							synchronized (dout) {
								try {
									dout.writeInt(States.END);
									dout.flush();
								} catch (IOException ioe) {
									// Other side may have closed
								}
								inOut.close();
								errOut.close();
							}
							break;
						case States.IN:
							int len = din.readInt();
							byte[] b = new byte[len];
							din.readFully(b);
							inOut.write(b);
							inOut.flush();
							break;
						case States.ERR:
							len = din.readInt();
							b = new byte[len];
							din.readFully(b);
							errOut.write(b);
							errOut.flush();
							break;
						case States.FAILED:
							String mesg = din.readUTF();
							throw new IOException("Remote error." + mesg);
						default:
							throw new IllegalStateException("Unknown forker command '" + cmd + "'");
						}
					}
				} catch (IOException ioe) {
					ioe.printStackTrace();
				} finally {
					try {
						s.close();
					} catch (IOException e) {
					}
				}
			}
		};
		thread.start();

	}

	/**
	 * Add a listener to the list of those to be notified when events such as
	 * window size changes occur.
	 * 
	 * @param listener
	 *            listener
	 */
	public void addListener(Listener listener) {
		listeners.add(listener);
	}

	/**
	 * Remove a listener from the list of those to be notified when events such
	 * as window size changes occur.
	 * 
	 * @param listener
	 *            listener
	 */
	public void removeListener(Listener listener) {
		listeners.remove(listener);
	}

	@Override
	public OutputStream getOutputStream() {
		if (out == null) {
			out = new OutputStream() {
				private boolean closed;

				@Override
				public void write(int b) throws IOException {
					if (closed) {
						throw new IOException("Closed.");
					}
					synchronized (dout) {
						dout.writeInt(States.OUT);
						dout.writeInt(1);
						dout.write((byte) b);
						maybeFlush();
					}
				}

				@Override
				public void write(byte[] b) throws IOException {
					if (closed) {
						throw new IOException("Closed.");
					}
					synchronized (dout) {
						dout.writeInt(States.OUT);
						dout.writeInt(b.length);
						dout.write(b);
						maybeFlush();
					}
				}

				@Override
				public void write(byte[] b, int off, int len) throws IOException {
					if (closed) {
						throw new IOException("Closed.");
					}
					synchronized (dout) {
						dout.writeInt(States.OUT);
						dout.writeInt(len);
						dout.write(b, off, len);
						maybeFlush();
					}
				}

				@Override
				public void flush() throws IOException {
					if (closed) {
						throw new IOException("Closed.");
					}
					synchronized (dout) {
						dout.writeInt(States.FLUSH_OUT);
						dout.flush();
					}
				}

				@Override
				public void close() throws IOException {
					if (closed) {
						throw new IOException("Already closed.");
					}
					closed = true;
					try {
						synchronized (dout) {
							dout.writeInt(States.CLOSE_OUT);
							flush();
						}
					} catch (IOException ioe) {
					}
				}

				private void maybeFlush() throws IOException {
					switch (outputFlushMode) {
					case AUTO:
						if (command.getIO().isAutoFlushStdIn()) {
							flush();
						}
						break;
					case LOCAL:
						dout.flush();
						break;
					case BOTH:
						flush();
						break;
					default:
						break;
					}
				}
			};
		}
		return out;
	}

	/**
	 * Get the current pseudo terminal width
	 * 
	 * @return pseudo terminal width
	 */
	public int getPtyWidth() {
		return ptyWidth;
	}

	/**
	 * Set the current pseudo terminal width
	 * 
	 * @param ptyWidth
	 *            pseudo terminal width
	 */
	public void setPtyWidth(int ptyWidth) {
		this.ptyWidth = ptyWidth;
		synchronized (dout) {
			try {
				dout.writeInt(States.WINDOW_SIZE);
				dout.writeInt(ptyWidth);
				dout.writeInt(ptyHeight);
				dout.flush();
			} catch (IOException ioe) {
				//
			}
		}
	}

	/**
	 * Get the current pseudo terminal height
	 * 
	 * @return pseudo terminal height
	 */
	public int getPtyHeight() {
		return ptyHeight;
	}

	/**
	 * Set the current pseudo terminal height
	 * 
	 * @param ptyHeight
	 *            pseudo terminal height
	 */
	public void setPtyHeight(int ptyHeight) {
		this.ptyHeight = ptyHeight;
		synchronized (dout) {
			try {
				dout.writeInt(States.WINDOW_SIZE);
				dout.writeInt(ptyWidth);
				dout.writeInt(ptyHeight);
				dout.flush();
			} catch (IOException ioe) {
				//
			}
		}
	}

	/**
	 * Set the current pseudo terminal size
	 * 
	 * @param ptyWidth
	 *            pseudo terminal width
	 * @param ptyHeight
	 *            pseudo terminal height
	 */
	public void setPtySize(int ptyWidth, int ptyHeight) {
		this.ptyWidth = ptyWidth;
		this.ptyHeight = ptyHeight;

		synchronized (dout) {
			try {
				dout.writeInt(States.WINDOW_SIZE);
				dout.writeInt(ptyWidth);
				dout.writeInt(ptyHeight);
				dout.flush();
			} catch (IOException ioe) {
				//
			}
		}
	}

	@Override
	public InputStream getInputStream() {
		return in;
	}

	@Override
	public InputStream getErrorStream() {
		return err;
	}

	@Override
	public int waitFor() throws InterruptedException {
		thread.join();
		return exitValue();
	}

	@Override
	public int exitValue() {
		return exitValue;
	}

	@Override
	public void destroy() {
		synchronized (dout) {
			try {
				dout.writeInt(States.KILL);
				dout.flush();
			} catch (IOException ioe) {
				//
			}
		}

	}

	protected void windowSizeChange(int ptyWidth, int ptyHeight) {
		for (int i = listeners.size() - 1; i >= 0; i--) {
			listeners.get(i).windowSizeChanged(ptyWidth, ptyHeight);
		}

	}
}