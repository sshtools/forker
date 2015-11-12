package com.sshtools.forker.client.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.sshtools.forker.client.AbstractOSProcess;
import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.common.IO;
import com.sun.jna.Memory;

public class POpenProcess extends AbstractOSProcess {

	private CSystem.FILE fd;
	private int exitValue = Integer.MIN_VALUE;
	private ForkerBuilder builder;
	private InputStream in;
	private OutputStream out;

	public POpenProcess(final ForkerBuilder builder) throws IOException {
		this.builder = builder;
		if(builder.effectiveUser() == null)
			doBuildCommand(builder);
		else {
			builder.effectiveUser().elevate(builder, this);
			try {
				doBuildCommand(builder);
			}
			finally {
				builder.effectiveUser().descend();				
			}
		}
		if (fd == null) {
			throw new IOException("Failed popen.");
		}
	}

	@Override
	public OutputStream getOutputStream() {
		if (builder.io() != IO.OUTPUT) {
			throw new IllegalStateException("Not in " + IO.OUTPUT + " mode.");
		}
		if (out == null) {
			out = new OutputStream() {

				private boolean closed;

				@Override
				public void write(int b) throws IOException {
					if (closed) {
						throw new IOException("Closed.");
					}
					CSystem.INSTANCE.fputs(String.valueOf((char) b), fd);
				}

				@Override
				public void write(byte[] b) throws IOException {
					if (closed) {
						throw new IOException("Closed.");
					}
					CSystem.INSTANCE.fputs(new String(b), fd);
				}

				@Override
				public void write(byte[] b, int off, int len)
						throws IOException {
					if (closed) {
						throw new IOException("Closed.");
					}
					CSystem.INSTANCE.fputs(new String(b, off, len), fd);
				}

				@Override
				public void close() throws IOException {
					if (closed) {
						throw new IOException("Already closed.");
					}
					closed = true;
				}
			};
		}
		return out;
	}

	@Override
	public InputStream getInputStream() {
		if (builder.io() != IO.INPUT) {
			throw new IllegalStateException("Not in " + IO.INPUT + " mode.");
		}
		if (in == null) {
			in = new InputStream() {

				private boolean closed;

				@Override
				public int read() throws IOException {
					if (closed) {
						throw new IOException("Closed.");
					}
					Memory buffer = new Memory(1);
					String res = CSystem.INSTANCE.fgets(buffer, 1, fd);
					if (res == null || res.length() == 0) {
						exitValue = CSystem.INSTANCE.pclose(fd);
						return -1;
					}
					return res.getBytes()[0];
				}

				@Override
				public int read(byte[] b) throws IOException {
					if (closed) {
						throw new IOException("Closed.");
					}
					Memory buffer = new Memory(b.length);
					String res = CSystem.INSTANCE.fgets(buffer, b.length, fd);
					if (res == null) {
						exitValue = CSystem.INSTANCE.pclose(fd);
						return -1;
					}
					System.arraycopy(res.getBytes(), 0, b, 0, res.length());
					return res.length();
				}

				@Override
				public int read(byte[] b, int off, int len) throws IOException {
					if (closed) {
						throw new IOException("Closed.");
					}
					Memory buffer = new Memory(len);
					String res = CSystem.INSTANCE.fgets(buffer, len, fd);
					if (res == null) {
						exitValue = CSystem.INSTANCE.pclose(fd);
						return -1;
					}
					System.arraycopy(res.getBytes(), 0, b, off, res.length());
					return res.length();
				}

				@Override
				public void close() throws IOException {
					if (closed) {
						throw new IOException("Already closed.");
					}
					closed = true;
				}
			};
		}
		return in;
	}

	@Override
	public InputStream getErrorStream() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int waitFor() throws InterruptedException {
		if (exitValue == Integer.MIN_VALUE) {
			exitValue = CSystem.INSTANCE.pclose(fd);
			if (exitValue == -1) {
				throw new RuntimeException("Failed to close process.");
			}
		}
		return exitValue;
	}

	@Override
	public int exitValue() {
		if (exitValue == Integer.MIN_VALUE) {
			throw new IllegalStateException("Process not finished.");
		}
		return exitValue;
	}

	@Override
	public void destroy() {
		throw new UnsupportedOperationException();
	}

	private void doBuildCommand(final ForkerBuilder builder) {
		fd = CSystem.INSTANCE.popen(buildCommand(builder),
				builder.io() == IO.INPUT ? "r" : "w");
	}
}