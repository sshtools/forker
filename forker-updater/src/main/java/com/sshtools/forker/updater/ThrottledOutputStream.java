package com.sshtools.forker.updater;

import java.io.IOException;
import java.io.OutputStream;

public class ThrottledOutputStream extends OutputStream {
	private OutputStream outputStream;
	private final long maxBytesPerSecond;
	private final long startTime = System.nanoTime();

	private long bytesWrite = 0;
	private long totalSleepTime = 0;
	private static final long SLEEP_DURATION_MS = 30;

	public ThrottledOutputStream(OutputStream outputStream) {
		this(outputStream, Long.MAX_VALUE);
	}

	public ThrottledOutputStream(OutputStream outputStream, long maxBytesPerSecond) {
		if (outputStream == null) {
			throw new IllegalArgumentException("outputStream shouldn't be null");
		}

		if (maxBytesPerSecond <= 0) {
			throw new IllegalArgumentException("maxBytesPerSecond should be greater than zero");
		}

		this.outputStream = outputStream;
		this.maxBytesPerSecond = maxBytesPerSecond;
	}

	@Override
	public void write(int arg0) throws IOException {
		throttle();
		outputStream.write(arg0);
		bytesWrite++;
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (len < maxBytesPerSecond) {
			throttle();
			bytesWrite = bytesWrite + len;
			outputStream.write(b, off, len);
			return;
		}

		long currentOffSet = off;
		long remainingBytesToWrite = len;

		do {
			throttle();
			remainingBytesToWrite = remainingBytesToWrite - maxBytesPerSecond;
			bytesWrite = bytesWrite + maxBytesPerSecond;
			outputStream.write(b, (int) currentOffSet, (int) maxBytesPerSecond);
			currentOffSet = currentOffSet + maxBytesPerSecond;

		} while (remainingBytesToWrite > maxBytesPerSecond);

		throttle();
		bytesWrite = bytesWrite + remainingBytesToWrite;
		outputStream.write(b, (int) currentOffSet, (int) remainingBytesToWrite);
	}

	@Override
	public void write(byte[] b) throws IOException {
		this.write(b, 0, b.length);
	}

	public void throttle() throws IOException {
		while (getBytesPerSec() > maxBytesPerSecond) {
			try {
				Thread.sleep(SLEEP_DURATION_MS);
				totalSleepTime += SLEEP_DURATION_MS;
			} catch (InterruptedException e) {
				System.out.println("Thread interrupted" + e.getMessage());
				throw new IOException("Thread interrupted", e);
			}
		}
	}

	/**
	 * Return the number of bytes read per second
	 */
	public long getBytesPerSec() {
		long elapsed = (System.nanoTime() - startTime) / 1000000000;
		if (elapsed == 0) {
			return bytesWrite;
		} else {
			return bytesWrite / elapsed;
		}
	}

	@Override
	public String toString() {
		return "ThrottledOutputStream{" + "bytesWrite=" + bytesWrite + ", maxBytesPerSecond=" + maxBytesPerSecond
				+ ", bytesPerSec=" + getBytesPerSec() + ", totalSleepTimeInSeconds=" + totalSleepTime / 1000 + '}';
	}

	public void close() throws IOException {
		outputStream.close();
	}
}