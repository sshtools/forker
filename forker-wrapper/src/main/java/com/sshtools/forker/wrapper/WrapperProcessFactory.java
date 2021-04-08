/**
 * Copyright © 2015 - 2021 SSHTOOLS Limited (support@sshtools.com)
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import com.sshtools.forker.client.ForkerProcess;
import com.sshtools.forker.client.EffectiveUser;
import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.client.ForkerProcessFactory;
import com.sshtools.forker.client.ForkerProcessListener;
import com.sshtools.forker.common.Util;
import com.sshtools.forker.wrapper.ForkerWrapper.KeyValuePair;

/**
 * {@link WrapperProcessFactory} implementation that allows the construction of
 * wrapped applications using {@link ForkerBuilder}. Simply set the
 * {@link ForkerBuilder#io(com.sshtools.forker.common.IO)} mode to
 * {@link WrapperIO#WRAPPER} and build the Java (or native) command to run. The
 * wrapper can be configured using
 * {@link WrapperProcessFactory#addOption(KeyValuePair)} (and the factory itself
 * may be obtained using {@link ForkerBuilder#processFactory(Class)}.
 * <p>
 * Note, if you want to run a native command, you will also need to set the
 * 'native' option.
 * 
 * <pre>
 * <code>fb.processFactory(WrapperProcessFactory.class).addOption(new KeyValuePair("native"));</code>
 * </pre>
 *
 */
public class WrapperProcessFactory implements ForkerProcessFactory {

	private final class WrapperProcessImpl extends ForkerProcess implements Runnable {
		private Thread thread;
		private int exitValue = 1;
		private InputStream inPipe;
		private InputStream errPipe;
		private OutputStream outPipe;
		private boolean redirectError;
		private ForkerWrapper wrapper;

		private WrapperProcessImpl(ForkerWrapper wrapper, boolean redirectError) {
			thread = new Thread(this, "ForkerWrapper");
			this.wrapper = wrapper;
			this.redirectError = redirectError;
		}

		@Override
		public int waitFor() throws InterruptedException {
			thread.join();
			return exitValue;
		}

		@Override
		public OutputStream getOutputStream() {
			return outPipe;
		}

		@Override
		public InputStream getInputStream() {
			return inPipe;
		}

		@Override
		public InputStream getErrorStream() {
			return errPipe;
		}

		@Override
		public int exitValue() {
			if (thread.isAlive())
				throw new IllegalThreadStateException("Process still running.");
			return exitValue;
		}

		@Override
		public void destroy() {
			try {
				wrapper.stop();
			} catch (InterruptedException e) {
			}
		}

		@Override
		public void run() {
			try {
				wrapper.start();
			} catch (IOException e) {
				e.printStackTrace();
				exitValue = 1;
			} catch (InterruptedException e) {
				e.printStackTrace();
				exitValue = 1;
			}

		}

		public void start() throws IOException {

			PipedOutputStream inPipeOut = new PipedOutputStream();
			wrapper.setDefaultOut(new PrintStream(inPipeOut));
			inPipe = new PipedInputStream(inPipeOut) {
				@Override
				public synchronized int read() throws IOException {
					try {
						return super.read();
					} catch (IOException ioe) {
						// TODO Blegh
						if (ioe.getMessage().indexOf("Write end dead") == -1
								&& ioe.getMessage().indexOf("Pipe broken") == -1)
							throw ioe;
						else
							return -1;
					}
				}
			};

			if (redirectError) {
				wrapper.setDefaultErr(wrapper.getDefaultOut());
			} else {
				PipedOutputStream errPipeOut = new PipedOutputStream();
				wrapper.setDefaultErr(new PrintStream(errPipeOut));
				errPipe = new PipedInputStream(errPipeOut);
			}

			PipedInputStream outPipeIn = new PipedInputStream();
			wrapper.setDefaultIn(outPipeIn);
			outPipe = new PipedOutputStream(outPipeIn);

			exitValue = 0;
			thread.start();
		}
	}

	private boolean separateProcess;
	private List<KeyValuePair> options = new ArrayList<KeyValuePair>();

	public WrapperProcessFactory() {
	}

	public void addOption(KeyValuePair option) {
		options.add(option);
	}

	public boolean isSeparateProcess() {
		return separateProcess;
	}

	public void setSeparateProcess(boolean separateProcess) {
		this.separateProcess = separateProcess;
	}

	@Override
	public ForkerProcess createProcess(ForkerBuilder builder, ForkerProcessListener listener) throws IOException {
		if (builder.io() == WrapperIO.WRAPPER) {
			List<String> allArgs = new ArrayList<String>(builder.command());
			EffectiveUser effectiveUser = builder.effectiveUser();
			if (separateProcess) {
				ForkerBuilder fb = new ForkerBuilder();
				fb.java();
				for (Integer i : builder.affinity()) {
					fb.command().add("--cpu=" + i);
				}
				if (builder.getCommand().getPriority() != null) {
					fb.command().add("--priority=" + builder.getCommand().getPriority().name());
				}
				fb.command().add(ForkerWrapper.class.getName());
				if (effectiveUser != null)
					fb.effectiveUser(effectiveUser);
				for (KeyValuePair opt : options) {
					String optv = "--" + opt.getName();
					String value = opt.getValue();
					if (value != null && !Boolean.TRUE.equals(value) && !"true".equals(value)) {
						if (value.contains(" "))
							optv += "'";
						optv += "=" + Util.escapeSingleQuotes(value);
						if (value.contains(" "))
							optv += "'";
					}
					fb.command().add(optv);
				}
				if (builder.background())
					fb.command().add("--daemon");
				fb.command().add("--");
				fb.command().addAll(allArgs);
				fb.environment().putAll(builder.environment());
				if (builder.redirectErrorStream())
					fb.redirectErrorStream(true);

				return (ForkerProcess) fb.start();
			} else {
				if (effectiveUser != null)
					throw new IOException("Cannot set effective user when running in same VM.");

				ForkerWrapper wrapper = new ForkerWrapper();
				for (KeyValuePair opt : options) {
					wrapper.getProperties().add(opt);
				}
				wrapper.setClassname(allArgs.remove(0));
				wrapper.setArguments(allArgs.toArray(new String[0]));
				WrapperProcessImpl wrapperProcessImpl = new WrapperProcessImpl(wrapper, builder.redirectErrorStream());
				wrapperProcessImpl.start();
				return wrapperProcessImpl;
			}
		}
		return null;
	}

}
