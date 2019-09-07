/*
 * Copyright (C) 2013 Brett Wooldridge
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sshtools.forker.client.impl.nonblocking;

import static com.sshtools.forker.client.impl.jna.posix.LibC.WEXITSTATUS;
import static com.sshtools.forker.client.impl.jna.posix.LibC.WIFEXITED;
import static com.sshtools.forker.client.impl.jna.posix.LibC.WIFSIGNALED;
import static com.sshtools.forker.client.impl.jna.posix.LibC.WTERMSIG;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

import org.apache.commons.lang3.SystemUtils;

import com.sshtools.forker.client.EffectiveUser;
import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.client.NonBlockingProcessListener;
import com.sshtools.forker.client.impl.jna.posix.LibC;
import com.sshtools.forker.client.impl.jna.posix.LibEpoll;
import com.sshtools.forker.client.impl.jna.posix.LibJava10;
import com.sshtools.forker.client.impl.jna.posix.LibJava8;
import com.sun.jna.JNIEnv;
import com.sun.jna.ptr.IntByReference;

/**
 * @author Brett Wooldridge
 */
public class NonBlockingLinuxProcess extends NonBlockingBasePosixProcess {
	private static final boolean isAzul;
	static {
		isAzul = System.getProperty("java.vm.vendor", "").contains("Azul");
		LibEpoll.sigignore(LibEpoll.SIGPIPE);
	}

	@SuppressWarnings("unused")
	private enum LaunchMechanism {
		// order IS important!
		FORK, POSIX_SPAWN, VFORK
	}

	/**
	 * @param builder builder
	 * @param factory factory
	 * @param listener listener
	 * @throws IOException
	 */
	public NonBlockingLinuxProcess(ForkerBuilder builder, NonBlockingProcessFactory factory, NonBlockingProcessListener listener)
			throws IOException {
		super(builder, factory, listener);
		EffectiveUser effectiveUser = builder.effectiveUser();
		if (effectiveUser != null) {
			effectiveUser.elevate(builder, null, builder.getCommand());
		}
		try {
			String[] cmdarray = builder.getCommand().getArguments().toArray(new String[0]);
			// See
			// https://github.com/JetBrains/jdk8u_jdk/blob/master/src/solaris/classes/java/lang/ProcessImpl.java#L71-L83
			byte[][] args = new byte[cmdarray.length - 1][];
			int size = args.length; // For added NUL bytes
			for (int i = 0; i < args.length; i++) {
				args[i] = cmdarray[i + 1].getBytes();
				size += args[i].length;
			}
			byte[] argBlock = new byte[size];
			int i = 0;
			for (byte[] arg : args) {
				System.arraycopy(arg, 0, argBlock, i, arg.length);
				i += arg.length + 1;
				// No need to write NUL bytes explicitly
			}
			// See
			// https://github.com/JetBrains/jdk8u_jdk/blob/master/src/solaris/classes/java/lang/ProcessImpl.java#L86
			byte[] envBlock = toEnvironmentBlock(builder.environment());
			try {
				// See
				// https://github.com/JetBrains/jdk8u_jdk/blob/master/src/solaris/classes/java/lang/ProcessImpl.java#L96
				createPipes();
				int[] child_fds = { stdinWidow, stdoutWidow, stderrWidow };
				if (!isAzul && SystemUtils.IS_JAVA_1_8) {
					pid = LibJava8.Java_java_lang_UNIXProcess_forkAndExec(JNIEnv.CURRENT, this,
							LaunchMechanism.VFORK.ordinal() + 1, toCString(System.getProperty("java.home") + "/lib/jspawnhelper"), // used
																																	// on
																																	// Linux
							toCString(cmdarray[0]), argBlock, args.length, envBlock, builder.environment().size(),
							(builder.directory() != null ? toCString(builder.directory().toString()) : null), child_fds,
							(byte) 0 /* redirectErrorStream */);
				} else {// See
					// https://github.com/JetBrains/jdk8u_jdk/blob/master/src/solaris/classes/java/lang/UNIXProcess.java#L247
					// Native source code:
					// https://github.com/JetBrains/jdk8u_jdk/blob/master/src/solaris/native/java/lang/UNIXProcess_md.c#L566
					pid = LibJava10.Java_java_lang_ProcessImpl_forkAndExec(JNIEnv.CURRENT, this,
							LaunchMechanism.VFORK.ordinal() + 1, toCString(System.getProperty("java.home") + "/lib/jspawnhelper"), // used
																																	// on
																																	// Linux
							toCString(cmdarray[0]), argBlock, args.length, envBlock, builder.environment().size(),
							(builder.directory() != null ? toCString(builder.directory().toString()) : null), child_fds,
							(byte) 0 /* redirectErrorStream */);
				}
				if (pid == -1) {
					throw new IOException("Process not started, no PID could be determined.");
				}
				// Close the child end of the pipes in our process
				LibC.close(stdinWidow);
				LibC.close(stdoutWidow);
				LibC.close(stderrWidow);
				initializeBuffers();
				afterStart();
				registerProcess();
			} catch (Exception e) {
				if (listener != null)
					listener.onError(e, this, true);
				else
					LOGGER.log(Level.WARNING, "Failed to start process", e);
				onExit(Integer.MIN_VALUE);
				throw new IOException("Process not started.", e);
			}
		} finally {
			if (effectiveUser != null) {
				effectiveUser.descend(builder, null, builder.getCommand());
			}
		}
	}

	@Override
	protected IEventProcessor<? extends NonBlockingProcess> createProcessor() {
		return new ProcessEpoll(factory);
	}

	@Override
	protected boolean checkLaunch() {
		// This is necessary on Linux because spawn failures are not reflected
		// in the rc, and this will reap
		// any zombies due to launch failure
		IntByReference ret = new IntByReference();
		int waitpidRc = LibC.waitpid(pid, ret, LibC.WNOHANG);
		int status = ret.getValue();
		boolean cleanExit = waitpidRc == pid && WIFEXITED(status) && WEXITSTATUS(status) == 0;
		if (cleanExit) {
			// If the process already exited cleanly, make sure we run epoll to
			// dispatch any stdout/stderr sent
			// before we tear everything down.
			cleanlyExitedBeforeProcess.set(true);
		} else if (waitpidRc != 0) {
			if (WIFEXITED(status)) {
				status = WEXITSTATUS(status);
				if (status == 127) {
					onExit(Integer.MIN_VALUE);
				} else {
					onExit(status);
				}
			} else if (WIFSIGNALED(status)) {
				onExit(WTERMSIG(status));
			}
			return false;
		}
		return true;
	}

	private static byte[] toCString(String s) {
		if (s == null)
			return null;
		byte[] bytes = s.getBytes();
		byte[] result = new byte[bytes.length + 1];
		System.arraycopy(bytes, 0, result, 0, bytes.length);
		result[result.length - 1] = (byte) 0;
		return result;
	}

	private static byte[] toEnvironmentBlock(Map<String, String> environmentMap) {
		String[] environment = mapToArray(environmentMap);
		int count = environment.length;
		for (String entry : environment) {
			count += entry.getBytes().length;
		}
		byte[] block = new byte[count];
		int i = 0;
		for (String entry : environment) {
			byte[] bytes = entry.getBytes();
			System.arraycopy(bytes, 0, block, i, bytes.length);
			i += bytes.length + 1;
			// No need to write NUL byte explicitly
			// block[i++] = (byte) '\u0000';
		}
		return block;
	}
}
