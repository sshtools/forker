/**
 * Copyright © 2015 - 2018 SSHTOOLS Limited (support@sshtools.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sshtools.forker.wrapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class LazyLogStream extends OutputStream {

	private File outputFile;
	private OutputStream out;
	private Object lock = new Object();
	private ScheduledFuture<?> scheduled;
	private boolean append;
	private long opens;
	private long logDelay;
	private final static ScheduledExecutorService closer = Executors.newScheduledThreadPool(1);

	public LazyLogStream(long logDelay, File outputFile, boolean append) {
		this.outputFile = outputFile;
		this.logDelay = logDelay;
		this.append = append;
	}

	@Override
	public void write(int b) throws IOException {
		synchronized (lock) {
			OutputStream out = checkOpen();
			out.write(b);
			if (logDelay == 0) {
				closeStream();
			}
		}
	}

	@Override
	public void write(byte[] b) throws IOException {
		synchronized (lock) {
			OutputStream out = checkOpen();
			out.write(b);
			if (logDelay == 0) {
				closeStream();
			}
		}
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		synchronized (lock) {
			OutputStream out = checkOpen();
			out.write(b, off, len);
			if (logDelay == 0) {
				closeStream();
			}
		}
	}

	@Override
	public void flush() throws IOException {
		synchronized (lock) {
			if (out != null)
				out.flush();
		}
	}

	@Override
	public void close() throws IOException {
		synchronized (lock) {
			if (out != null)
				out.flush();
		}
	}

	private OutputStream checkOpen() throws IOException {
		synchronized (lock) {
			if (out == null) {
				out = new FileOutputStream(outputFile, opens > 0 || append);
				opens++;
			} else if (scheduled != null)
				scheduled.cancel(true);
			if (logDelay != 0) {
				scheduled = closer.schedule(new Runnable() {
					@Override
					public void run() {
						synchronized (lock) {
							closeStream();
						}
					}
				}, logDelay, TimeUnit.MILLISECONDS);
			}
			return out;
		}
	}

	private void closeStream() {
		try {
			out.close();
		} catch (IOException ioe) {
		} finally {
			out = null;
		}
	}
}
