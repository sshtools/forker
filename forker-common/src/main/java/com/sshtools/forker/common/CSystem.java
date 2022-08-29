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
package com.sshtools.forker.common;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.LastErrorException;
import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

/**
 * Interface to the native C library.
 */
public interface CSystem extends Library {

	/**
	 * The Class SockAddr.
	 */
	@FieldOrder({"sun_family", "sun_path"})
	public static class SockAddr extends Structure {

		/** The sun family. */
		public short sun_family;
		
		/** The sun path. */
		public byte[] sun_path;

		/**
		 * Instantiates a new sock addr.
		 *
		 * @param sunPath the sun path
		 */
		public SockAddr(String sunPath) {
			sun_family = AF_UNIX;
			byte[] arr = sunPath.getBytes();
			sun_path = new byte[arr.length + 1];
			System.arraycopy(arr, 0, sun_path, 0, Math.min(sun_path.length - 1, arr.length));
			allocateMemory();
		}
	}
	
	/**
	 * Instance of library.
	 */
	CSystem INSTANCE = (CSystem) Native.load((Platform.isWindows() ? "msvcrt" : "c"), CSystem.class);
	
	/** The Constant AF_UNIX. */
	public static final int AF_UNIX = 1;
	
	/** The Constant SOCK_STREAM. */
	public static final int SOCK_STREAM = Platform.isSolaris() ? 2 : 1;
	
	/** The Constant PROTOCOL. */
	public static final int PROTOCOL = 0;

	/**
	 * The change occurs immediately.
	 */
	public final static int TCSANOW = 0;
	
	/** Stdin FD. */
	public final static int STDIN_FILENO = 0;

	/* c_oflag bits */

	/** Post-process output. */
	public final static int OPOST = 0x00000001;

	/* c_iflag bits */

	/**
	 * Enable signals.
	 */
	public final static int ISIG = 0x00000001;
	/**
	 * Enable echo.
	 */
	public final static int ECHO = 0x00000008;
	/**
	 * Echo erase character as error-correcting backspace.
	 */
	public final static int ECHOE = 0x00000002;

	/**
	 * Set the process name for the calling thread, using the value in the
	 * location pointed to by (char *) arg2.
	 */
	public final static int PR_SET_NAME = 15;

	/**
	 * Socket.
	 *
	 * @param domain the domain
	 * @param type the type
	 * @param protocol the protocol
	 * @return the int
	 * @throws LastErrorException the last error exception
	 */
	int socket(int domain, int type, int protocol) throws LastErrorException;

	/**
	 * Bind.
	 *
	 * @param fd the fd
	 * @param sockaddr the sockaddr
	 * @param addrlen the addrlen
	 * @return the int
	 * @throws LastErrorException the last error exception
	 */
	int bind(int fd, SockAddr sockaddr, int addrlen) throws LastErrorException;

	/**
	 * Accept.
	 *
	 * @param fd the fd
	 * @param sockaddr the sockaddr
	 * @param addrlen the addrlen
	 * @return the int
	 * @throws LastErrorException the last error exception
	 */
	int accept(int fd, SockAddr sockaddr, int addrlen) throws LastErrorException;

	/**
	 * Listen.
	 *
	 * @param fd the fd
	 * @param backlog the backlog
	 * @return the int
	 * @throws LastErrorException the last error exception
	 */
	int listen(int fd, int backlog) throws LastErrorException;

	/**
	 * Connect.
	 *
	 * @param sockfd the sockfd
	 * @param sockaddr the sockaddr
	 * @param addrlen the addrlen
	 * @return the int
	 * @throws LastErrorException the last error exception
	 */
	int connect(int sockfd, SockAddr sockaddr, int addrlen) throws LastErrorException;

	/**
	 * Recv.
	 *
	 * @param fd the fd
	 * @param buffer the buffer
	 * @param count the count
	 * @param flags the flags
	 * @return the int
	 * @throws LastErrorException the last error exception
	 */
	int recv(int fd, byte[] buffer, int count, int flags) throws LastErrorException;

	/**
	 * Send.
	 *
	 * @param fd the fd
	 * @param buffer the buffer
	 * @param count the count
	 * @param flags the flags
	 * @return the int
	 * @throws LastErrorException the last error exception
	 */
	int send(int fd, byte[] buffer, int count, int flags) throws LastErrorException;

	/**
	 * Close.
	 *
	 * @param fd the fd
	 * @return the int
	 * @throws LastErrorException the last error exception
	 */
	int close(int fd) throws LastErrorException;

	/**
	 * Strerror.
	 *
	 * @param errno the errno
	 * @return the string
	 */
	String strerror(int errno);

	/**
	 * passes the command name or program name specified by command to the host
	 * environment to be executed by the command processor and returns after the
	 * command has been completed.
	 * 
	 * @param cmd
	 *            command
	 * @return exit status
	 */
	int system(String cmd);

	/**
	 * The popen() function opens a process by creating a pipe, forking, and
	 * invoking the shell. Since a pipe is by definition unidirectional, the
	 * type argument may specify only reading or writing, not both; the
	 * resulting stream is correspondingly read-only or write-only.
	 * 
	 * @param command
	 *            command
	 * @param type
	 *            "r" or "w"
	 * @return file handle to stream
	 */
	FILE popen(String command, String type);

	/**
	 * Waits for the associated process to terminate and returns the exit status
	 * of the command as returned by wait4(2).
	 * 
	 * @param fp
	 *            file handle
	 * @return exit status
	 */
	int pclose(FILE fp);

	/**
	 * Reads a line from the specified stream and stores it into the string
	 * pointed to by str. It stops when either (n-1) characters are read, the
	 * newline character is read, or the end-of-file is reached, whichever comes
	 * first.
	 * 
	 * @param memory
	 *            memory containing striing
	 * @param size
	 *            size of string
	 * @param fp
	 *            handle
	 * @return string
	 */
	String fgets(Memory memory, int size, FILE fp);

	/**
	 * Writes a string to the specified stream up to but not including the null
	 * character.
	 * 
	 * @param content
	 *            content
	 * @param fp
	 *            handle
	 * @return status
	 */
	int fputs(String content, FILE fp);

	/**
	 * Sets the effective user ID of the calling process. If the effective UID
	 * of the caller is root, the real UID and saved set-user-ID are also set.
	 * 
	 * @param uid
	 *            uid
	 * @return On success, zero is returned. On error, -1 is returned, and errno
	 *         is set appropriately.
	 */
	int setuid(int uid);

	/**
	 * Returns the real user ID of the calling process.
	 * 
	 * @return real user ID of the calling process
	 */
	int getuid();

	/**
	 * Returns the current process ID.
	 * 
	 * @return process ID
	 */
	int getpid();

	/**
	 * Sets the effective user ID of the calling process. Unprivileged user
	 * processes may only set the effective user ID to the real user ID, the
	 * effective user ID or the saved set-user-ID.
	 * 
	 * @param uid uid
	 * @return On success, zero is returned. On error, -1 is returned, and errno
	 *         is set appropriately.
	 */
	int seteuid(int uid);

	/**
	 * Returns the effective user ID of the calling process.
	 * 
	 * @return the effective user ID of the calling process
	 */
	int geteuid();

	/**
	 * Sets the effective group ID of the calling process. If the calling process 
	 * is privileged (more precisely: has the CAP_SETGID capability in its user namespace), 
	 * the real GID and saved set-group-ID are also set.
	 * 
	 * @param gid
	 *            gid
	 * @return On success, zero is returned. On error, -1 is returned, and errno
	 *         is set appropriately.
	 */
	int setgid(int gid);

	/**
	 * Set effective user or group ID.
	 * 
	 * @param egid
	 *            egid
	 * @return On success, zero is returned. On error, -1 is returned, and errno
	 *         is set appropriately.
	 */
	int setegid(int egid);

	/**
	 * Returns the real group ID of the calling process.
	 * 
	 * @return real group ID of the calling process..
	 */
	int getgid();

	/**
	 * Returns the effective group ID of the calling process.
	 * 
	 * @return effective group ID of the calling process..
	 */
	int getegid();

	/**
	 * Creates a new process by duplicating the calling process. The new
	 * process, referred to as the child, is an exact duplicate of the calling
	 * process, referred to as the parent, except for the following points:
	 * 
	 * @return On success, the PID of the child process is returned in the
	 *         parent, and 0 is returned in the child. On failure, -1 is
	 *         returned in the parent, no child process is created, and errno is
	 *         set appropriately.
	 */
	int fork();

	/**
	 * The kill() function shall send a signal to a process or a group of processes 
	 * specified by pid. The signal to be sent is specified by sig and is either 
	 * one from the list given in &lt;signal.h&gt; or 0. If sig is 0 (the null signal), 
	 * error checking is performed but no signal is actually sent. The null signal 
	 * can be used to check the validity of pid. 
	 *
	 * @param pid PID
	 * @param sig signal
	 * @return Upon successful completion, 0 shall be returned. Otherwise, -1 shall be returned and errno set to indicate the error. 
	 */
	int kill(long pid, int sig);

	/**
	 * Operations on a process.
	 *
	 * @param option            option
	 * @param arg2            arg2
	 * @param arg3            arg3
	 * @param arg4            arg4
	 * @param arg5            arg5
	 * @return status
	 */
	int prctl(int option, Pointer arg2, Pointer arg3, Pointer arg4, Pointer arg5);

	/**
	 * Get terminal attributes, line control, get baud rate.
	 *
	 * @param port            port
	 * @param termios            IO structure
	 * @return status
	 */
	int tcgetattr(int port, Termios termios);

	/**
	 * Set terminal attributes, line control, set baud rate.
	 *
	 * @param port            port mode
	 * @param mode            IO structure
	 * @param termios term IO structure
	 * @return status
	 */
	int tcsetattr(int port, int mode, Termios termios);
	
	/**
	 * This function shall cause the directory named by the pathname pointed to by
	 * the path argument to become the current working directory; that is, the
	 * starting point for path searches for pathnames not beginning with '/' .
	 * 
	 * @param path path
	 * @return Upon successful completion, 0 shall be returned. Otherwise, -1 shall
	 *         be returned, the current working directory shall remain unchanged,
	 *         and errno shall be set to indicate the error.
	 */
	int chdir(String path);

	/**
	 * Represents a file pointer.
	 *
	 */
	public class FILE extends PointerType {
	}

	/**
	 * Terminal IO structure.
	 */
	public class Termios extends Structure {

		/** Input flags. */
		public int c_iflag;
		
		/** Output flags. */
		public int c_oflag;
		
		/** Control modes. */
		public int c_cflag;
		
		/** Local modes. */
		public int c_lflag;
		
		/** Line discipline. */
		public byte c_line;
		
		/** Special characters. */
		public byte[] c_cc = new byte[(32)];
		
		/** Input speed. */
		public int c_ispeed;
		
		/** Output speed. */
		public int c_ospeed;

		/**
		 * Constructor.
		 */
		public Termios() {
		}

		/**
		 * Construct a new terminal IO structure from another.
		 *
		 * @param termios            structure to copy from
		 */
		public Termios(Termios termios) {
			this.c_iflag = termios.c_iflag;
			this.c_oflag = termios.c_oflag;
			this.c_cflag = termios.c_cflag;
			this.c_lflag = termios.c_lflag;
			this.c_line = termios.c_line;
			System.arraycopy(termios.c_cc, 0, c_cc, 0, termios.c_cc.length);
			this.c_ispeed = termios.c_ispeed;
			this.c_ospeed = termios.c_ospeed;
		}

		@Override
		protected List<String> getFieldOrder() {
			return Arrays.asList("c_iflag", "c_oflag", "c_cflag", "c_lflag", "c_line", "c_cc", "c_ispeed", "c_ospeed");
		}

		/**
		 * To string.
		 *
		 * @return the string
		 */
		@Override
		public String toString() {
			return "Termios [c_iflag=" + c_iflag + ", c_oflag=" + c_oflag + ", c_cflag=" + c_cflag + ", c_lflag="
					+ c_lflag + ", c_line=" + c_line + ", c_cc=" + Arrays.toString(c_cc) + ", c_ispeed=" + c_ispeed
					+ ", c_ospeed=" + c_ospeed + "]";
		}

	}
}
