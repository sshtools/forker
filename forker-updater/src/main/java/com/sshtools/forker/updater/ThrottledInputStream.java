package com.sshtools.forker.updater;

import java.io.IOException;
import java.io.InputStream;

public class ThrottledInputStream extends InputStream {

	private final InputStream inputStream;
	private final long maxBytesPerSec;
	private final long startTime = System.nanoTime();

	private long bytesRead = 0;
	private long totalSleepTime = 0;

	private static final long SLEEP_DURATION_MS = 30;

	public ThrottledInputStream(InputStream inputStream) {
		this(inputStream, Long.MAX_VALUE);
	}

	public ThrottledInputStream(InputStream inputStream, long maxBytesPerSec) {
		if (maxBytesPerSec < 0) {
			throw new IllegalArgumentException("maxBytesPerSec shouldn't be negative");
		}
		if (inputStream == null) {
			throw new IllegalArgumentException("inputStream shouldn't be null");
		}

		this.inputStream = inputStream;
		this.maxBytesPerSec = maxBytesPerSec;
	}

	@Override
	public void close() throws IOException {
		inputStream.close();
	}

	@Override
	public int read() throws IOException {
		throttle();
		int data = inputStream.read();
		if (data != -1) {
			bytesRead++;
		}
		return data;
	}

	@Override
	public int read(byte[] b) throws IOException {
		throttle();
		int readLen = inputStream.read(b);
		if (readLen != -1) {
			bytesRead += readLen;
		}
		return readLen;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		throttle();
		int readLen = inputStream.read(b, off, len);
		if (readLen != -1) {
			bytesRead += readLen;
		}
		return readLen;
	}

	private void throttle() throws IOException {
		while (getBytesPerSec() > maxBytesPerSec) {
			try {
				Thread.sleep(SLEEP_DURATION_MS);
				totalSleepTime += SLEEP_DURATION_MS;
			} catch (InterruptedException e) {
				System.out.println("Thread interrupted" + e.getMessage());
				throw new IOException("Thread interrupted", e);
			}
		}
	}

	public long getTotalBytesRead() {
		return bytesRead;
	}

	/**
	 * Return the number of bytes read per second
	 */
	public long getBytesPerSec() {
		long elapsed = (System.nanoTime() - startTime) / 1000000000;
		if (elapsed == 0) {
			return bytesRead;
		} else {
			return bytesRead / elapsed;
		}
	}

	public long getTotalSleepTime() {
		return totalSleepTime;
	}

	@Override
	public String toString() {
		return "ThrottledInputStream{" + "bytesRead=" + bytesRead + ", maxBytesPerSec=" + maxBytesPerSec
				+ ", bytesPerSec=" + getBytesPerSec() + ", totalSleepTimeInSeconds=" + totalSleepTime / 1000 + '}';
	}
}