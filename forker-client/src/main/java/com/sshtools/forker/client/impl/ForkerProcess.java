package com.sshtools.forker.client.impl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import com.sshtools.forker.client.Forker;
import com.sshtools.forker.common.Command;
import com.sshtools.forker.common.Cookie;
import com.sshtools.forker.common.Cookie.Instance;
import com.sshtools.forker.common.States;

public class ForkerProcess extends Process {
	
	static {
		Forker.loadDaemon();
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

	public interface Listener {
		void windowSizeChanged(int ptyWidth, int ptyHeight);
	}

	public ForkerProcess(Command command) {
		this.command = command;
	}

	public void start() throws NumberFormatException, UnknownHostException, IOException {

		Instance cookie = Cookie.get().load();
		if (cookie == null) {
			throw new IOException("The forker daemon is not running.");
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
	
			in = new PipedInputStream(inOut);
			err = new PipedInputStream(errOut);
			
			ok = true;
		}
		finally {
			if(!ok)
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
							run = false;
							exitValue = din.readInt();
							inOut.close();
							errOut.close();
							synchronized (dout) {
								dout.writeInt(States.END);
								dout.flush();
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

	public void addListener(Listener listener) {
		listeners.add(listener);
	}

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
					}
				}

				@Override
				public void flush() throws IOException {
					if (closed) {
						throw new IOException("Closed.");
					}
					synchronized (dout) {
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
						}
					} catch (IOException ioe) {
					}
				}

			};
		}
		return out;
	}

	public int getPtyWidth() {
		return ptyWidth;
	}

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

	public int getPtyHeight() {
		return ptyHeight;
	}

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
	
	public void setPtySize(int ptyWidth, int ptyHeight) {
		this.ptyWidth = ptyWidth;
		this.ptyHeight= ptyHeight;

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