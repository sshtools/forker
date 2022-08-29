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
package com.sshtools.forker.client.impl.nonblocking;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;

import com.sshtools.forker.client.EffectiveUser;
import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.client.IEventProcessor;
import com.sshtools.forker.client.NonBlockingProcess;
import com.sshtools.forker.client.NonBlockingProcessFactory;
import com.sshtools.forker.client.NonBlockingProcessListener;
import com.sshtools.forker.client.impl.jna.posix.LibC;
import com.sshtools.forker.client.impl.jna.posix.LibC.SyscallLibrary;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.StringArray;
import com.sun.jna.ptr.IntByReference;

/**
 * @author Brett Wooldridge
 */
public class NonBlockingOsxProcess extends NonBlockingBasePosixProcess {
	static {
		// Setup a private signal for waking up the kqueue processing threads
		LibC.signal(LibC.SIGUSR2, LibC.SIG_IGN);
	}

	/**
	 * @param builder
	 * @param factory
	 * @param listener
	 * @throws IOException 
	 */
	public NonBlockingOsxProcess(ForkerBuilder builder, NonBlockingProcessFactory factory, NonBlockingProcessListener listener)
			throws IOException {
		super(builder, factory, listener);

		EffectiveUser effectiveUser = builder.effectiveUser();
		if (effectiveUser != null) {
			effectiveUser.elevate(builder, null, builder.getCommand());
		}
		try {
			
			String[] commands = builder.command().toArray(new String[0]);
			Pointer posix_spawn_file_actions = createPosixPipes();
			Pointer posix_spawnattr = createPosixSpawnAttributes();
			try {
				int rc = LibC.posix_spawnattr_init(posix_spawnattr);
				checkReturnCode(rc, "Internal call to posix_spawnattr_init() failed");
				LibC.posix_spawnattr_setflags(posix_spawnattr,
						(short) (LibC.POSIX_SPAWN_START_SUSPENDED | LibC.POSIX_SPAWN_CLOEXEC_DEFAULT));
				IntByReference restrict_pid = new IntByReference();
				StringArray commandsArray = new StringArray(commands);
				StringArray environmentArray = new StringArray(mapToArray(builder.environment()));
				if (builder.directory() != null) {
					rc = spawnWithCwd(restrict_pid, commands[0], posix_spawn_file_actions, posix_spawnattr, commandsArray,
							environmentArray, builder.directory().toPath());
				} else {
					rc = LibC.posix_spawnp(restrict_pid, commands[0], posix_spawn_file_actions, posix_spawnattr, commandsArray,
							environmentArray);
				}
				pid = restrict_pid.getValue();
				initializeBuffers();
				if (!checkLaunch()) {
					throw new IOException("Process not started.");
				}
				checkReturnCode(rc, "Invocation of posix_spawn() failed");
				afterStart();
				registerProcess();
				singleProcessContinue();
			} finally {
				LibC.posix_spawnattr_destroy(posix_spawnattr);
				LibC.posix_spawn_file_actions_destroy(posix_spawn_file_actions);
				// After we've spawned, close the unused ends of our pipes (that
				// were dup'd into the child process space)
				LibC.close(stdinWidow);
				LibC.close(stdoutWidow);
				LibC.close(stderrWidow);
			}
		} finally {
			if (effectiveUser != null) {
				effectiveUser.descend(builder, null, builder.getCommand());
			}
		}
		

	}

	@Override
	protected IEventProcessor<? extends NonBlockingProcess> createProcessor() {
		return new ProcessKqueue(factory);
	}

	private int spawnWithCwd(final IntByReference restrict_pid, final String restrict_path, final Pointer file_actions,
			final Pointer /* const posix_spawnattr_t */ restrict_attrp,
			final StringArray /* String[] */ argv, final Pointer /* String[] */ envp, final Path cwd) {
		int cwdBufSize = 1024;
		long peer = Native.malloc(cwdBufSize);
		Pointer oldCwd = new Pointer(peer);
		LibC.getcwd(oldCwd, cwdBufSize);
		String newCwd = cwd.toAbsolutePath().toString();
		int rc = LibC.SYSCALL.syscall(SyscallLibrary.SYS___pthread_chdir, newCwd);
		checkReturnCode(rc, "syscall(SYS__pthread_chdir) failed to set current directory");
		try {
			return LibC.posix_spawnp(restrict_pid, restrict_path, file_actions, restrict_attrp, argv, envp);
		} finally {
			rc = LibC.SYSCALL.syscall(SyscallLibrary.SYS___pthread_chdir, oldCwd);
			Native.free(Pointer.nativeValue(oldCwd));
			checkReturnCode(rc, "syscall(SYS__pthread_chdir) failed to restore current directory");
		}
	}

	private void singleProcessContinue() {
		// Signal the spawned process to continue (unsuspend)
		LibC.kill(pid, LibC.SIGCONT);
	}

	private Pointer createPosixPipes() {
		int rc;
		Pointer posix_spawn_file_actions = createPosixSpawnFileActions();
		try {
			int[] fds = createPipes();
			// Create spawn file actions
			rc = LibC.posix_spawn_file_actions_init(posix_spawn_file_actions);
			checkReturnCode(rc, "Internal call to posix_spawn_file_actions_init() failed");
			// Dup the reading end of the pipe into the sub-process, and close
			// our end
			rc = LibC.posix_spawn_file_actions_adddup2(posix_spawn_file_actions, stdinWidow, 0);
			checkReturnCode(rc, "Internal call to posix_spawn_file_actions_adddup2() failed");
			rc = LibC.posix_spawn_file_actions_addclose(posix_spawn_file_actions, fds[0]);
			checkReturnCode(rc, "Internal call to posix_spawn_file_actions_addclose() failed");
			// Dup the writing end of the pipe into the sub-process, and close
			// our end
			rc = LibC.posix_spawn_file_actions_adddup2(posix_spawn_file_actions, stdoutWidow, 1);
			checkReturnCode(rc, "Internal call to posix_spawn_file_actions_adddup2() failed");
			rc = LibC.posix_spawn_file_actions_addclose(posix_spawn_file_actions, fds[1]);
			checkReturnCode(rc, "Internal call to posix_spawn_file_actions_addclose() failed");
			// Dup the writing end of the pipe into the sub-process, and close
			// our end
			rc = LibC.posix_spawn_file_actions_adddup2(posix_spawn_file_actions, stderrWidow, 2);
			checkReturnCode(rc, "Internal call to posix_spawn_file_actions_adddup2() failed");
			rc = LibC.posix_spawn_file_actions_addclose(posix_spawn_file_actions, fds[2]);
			checkReturnCode(rc, "Internal call to posix_spawn_file_actions_addclose() failed");
			return posix_spawn_file_actions;
		} catch (RuntimeException e) {
			LOGGER.log(Level.WARNING, "Exception creating posix pipe actions", e);
			LibC.posix_spawn_file_actions_destroy(posix_spawn_file_actions);
			throw e;
		}
	}

	private Pointer createPosixSpawnFileActions() {
		return new Memory(Native.POINTER_SIZE);
	}

	private Pointer createPosixSpawnAttributes() {
		return new Memory(Native.POINTER_SIZE);
	}
}
