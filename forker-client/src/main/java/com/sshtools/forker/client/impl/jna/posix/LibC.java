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
package com.sshtools.forker.client.impl.jna.posix;

import java.nio.ByteBuffer;

import org.apache.commons.lang3.SystemUtils;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.StringArray;
import com.sun.jna.ptr.IntByReference;

/**
 */
public class LibC {
	static {
		if (SystemUtils.IS_OS_MAC_OSX) {
			O_NONBLOCK = 0x0004; // MacOS X, Freebsd
		} else {
			O_NONBLOCK = 2048; // Linux
		}
		Native.register(NativeLibrary.getProcess());
	}

	/**
	 * @param fildes fildes
	 * @return status
	 */
	public static native int pipe(int[] fildes);

	/**
	 * @param posix_spawnattr_t
	 * @return status
	 */
	public static native int posix_spawnattr_init(Pointer posix_spawnattr_t);

	/**
	 * @param posix_spawnattr_t
	 * @return status
	 */
	public static native int posix_spawnattr_destroy(Pointer posix_spawnattr_t);

	/**
	 * @param posix_spawnattr_t
	 * @param flags
	 * @return status
	 */
	public static native int posix_spawnattr_setflags(Pointer posix_spawnattr_t, short flags);

	/**
	 * @param posix_spawn_file_actions_t
	 * @return status
	 */
	public static native int posix_spawn_file_actions_init(Pointer posix_spawn_file_actions_t);

	/**
	 * @param posix_spawn_file_actions_t
	 * @return status
	 */
	public static native int posix_spawn_file_actions_destroy(Pointer posix_spawn_file_actions_t);

	/**
	 * @param actions
	 * @param filedes
	 * @return status
	 */
	public static native int posix_spawn_file_actions_addclose(Pointer actions, int filedes);

	/**
	 * @param actions
	 * @param fildes
	 * @param newfildes
	 * @return status
	 */
	public static native int posix_spawn_file_actions_adddup2(Pointer actions, int fildes, int newfildes);

	/**
	 * @param restrict_pid
	 * @param restrict_path
	 * @param file_actions
	 * @param restrict_attrp
	 * @param argv
	 * @param envp
	 * @return status
	 */
	public static native int posix_spawnp(IntByReference restrict_pid, String restrict_path, Pointer file_actions,
			Pointer /* const posix_spawnattr_t */ restrict_attrp,
			StringArray /* String[] */ argv, Pointer /* String[] */ envp);

	/**
	 * @param fildes
	 * @param cmd
	 * @return status
	 */
	public static native int fcntl(int fildes, int cmd);

	/**
	 * @param fildes
	 * @param cmd
	 * @param argO
	 * @return status
	 */
	public static native int fcntl(int fildes, int cmd, long argO);

	/**
	 * @param fildes
	 * @return status
	 */
	public static native int close(int fildes);

	/**
	 * @param fildes
	 * @param buf
	 * @param nbyte
	 * @return status
	 */
	public static native int write(int fildes, ByteBuffer buf, int nbyte);

	/**
	 * @param fildes
	 * @param buf
	 * @param nbyte
	 * @return status
	 */
	public static native int read(int fildes, ByteBuffer buf, int nbyte);

	/**
	 * @return pid
	 */
	public static native int getpid();

	/**
	 * @param pid
	 * @param sig
	 * @return status
	 */
	public static native int kill(int pid, int sig);

	/**
	 * @param pid
	 * @param status
	 * @param options
	 * @return status
	 */
	public static native int waitpid(int pid, IntByReference status, int options);

	/**
	 * @param signal
	 * @param func
	 * @return pointer
	 */
	public static native Pointer signal(int signal, Pointer func);

	/**
	 * @param buf
	 * @param size
	 * @return cwd
	 */
	public static native String getcwd(Pointer buf, int size);

	/**
	 * from /usr/include/sys/syscall.h We can't use JNA direct mapping for
	 * syscall(), since it takes varargs.
	 */
	public interface SyscallLibrary extends Library {
		/**
		 * 
		 */
		public static final int SYS___pthread_chdir = 348;

		/**
		 * @param syscall_number
		 * @param args
		 * @return status
		 */
		int syscall(int syscall_number, Object... args);
	}

	/**
	 * 
	 */
	public static SyscallLibrary SYSCALL = Native.load(Platform.C_LIBRARY_NAME, SyscallLibrary.class);
	/**
	 * 
	 */
	public static final int F_GETFL = 3;
	/**
	 * 
	 */
	public static final int F_SETFL = 4;
	/**
	 * 
	 */
	public static final int O_NONBLOCK;
	/**
	 * from /usr/include/asm-generic/errno-base.h
	 */
	public static final int ECHILD = 10; /* No child processes */
	/**
	 * from /usr/include/sys/wait.h
	 */
	public static final int WNOHANG = 0x00000001;
	/**
	 * from /usr/include/sys/spawn.h
	 */
	public static final short POSIX_SPAWN_START_SUSPENDED = 0x0080;
	/**
	 * From /usr/include/sys/signal.h
	 */
	public static final short POSIX_SPAWN_CLOEXEC_DEFAULT = 0x4000;
	/**
	 * 
	 */
	public static final int SIGKILL = 9;
	/**
	 * 
	 */
	public static final int SIGTERM = 15;
	/**
	 * 
	 */
	public static final int SIGCONT = 19;
	/**
	 * 
	 */
	public static final int SIGUSR2 = 31;
	/**
	 * 
	 */
	public static final Pointer SIG_IGN = Pointer.createConstant(1);

	/**
	 * If WIFEXITED(STATUS), the low-order 8 bits of the status.
	 * 
	 * @param status
	 * @return status
	 */
	public static int WEXITSTATUS(int status) {
		return (((status) & 0xff00) >> 8);
	}

	/**
	 * If WIFSIGNALED(STATUS), the terminating signal.
	 * 
	 * @param status
	 * @return status
	 */
	public static int WTERMSIG(int status) {
		return ((status) & 0x7f);
	}

	/**
	 * If WIFSTOPPED(STATUS), the signal that stopped the child.
	 * @param status
	 * @return status
	 */
	public static int WSTOPSIG(int status) {
		return WEXITSTATUS(status);
	}

	/**
	 * Nonzero if STATUS indicates normal termination. 
	 * 
	 * @param status
	 * @return status
	 */
	public static boolean WIFEXITED(int status) {
		return ((status) & 0x7f) == 0;
	}

	/**
	 * Nonzero if STATUS indicates termination by a signal. 
	 * 
	 * @param status
	 * @return status
	 */
	public static boolean WIFSIGNALED(int status) {
		return (((byte) (((status) & 0x7f) + 1) >> 1) > 0);
	}

	/**
	 * Nonzero if STATUS indicates the child is stopped. 
	 * 
	 * @param status
	 * @return status
	 */
	public static boolean WIFSTOPPED(int status) {
		return WTERMSIG(status) != 0;
	}

	/**
	 * @param ret
	 * @param sig
	 * @return status
	 */
	public static int W_EXITCODE(int ret, int sig) {
		return ((ret) << 8 | (sig));
	}

	/**
	 * @param sig
	 * @return status
	 */
	public static int W_STOPCODE(int sig) {
		return ((sig) << 8 | 0x7f);
	}

	/**
	 *
	 */
	public interface SignalFunction extends Callback {
		/**
		 * @param signal
		 */
		void invoke(int signal);
	}
}
