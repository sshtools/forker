package com.sshtools.forker.common;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.Structure;

/**
 * Interface to the native C library.
 */
public interface CSystem extends Library {
	/**
	 * Instance of library.
	 */
	CSystem INSTANCE = (CSystem) Native.load((Platform.isWindows() ? "msvcrt" : "c"), CSystem.class);

	/**
	 * The change occurs immediately.
	 */
	public final static int TCSANOW = 0;
	/**
	 * Stdin FD
	 */
	public final static int STDIN_FILENO = 0;

	/* c_oflag bits */

	/**
	 * Post-process output
	 */
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
	 * Operations on a process
	 * 
	 * @param option
	 *            option
	 * @param arg2
	 *            arg2
	 * @param arg3
	 *            arg3
	 * @param arg4
	 *            arg4
	 * @param arg5
	 *            arg5
	 * @return status
	 */
	int prctl(int option, Pointer arg2, Pointer arg3, Pointer arg4, Pointer arg5);

	/**
	 * Get terminal attributes, line control, get baud rate
	 * 
	 * @param port
	 *            port
	 * @param termios
	 *            IO structure
	 * @return status
	 */
	int tcgetattr(int port, Termios termios);

	/**
	 * Set terminal attributes, line control, set baud rate
	 * 
	 * @param port
	 *            port mode
	 * @param mode
	 *            IO structure
	 * @param termios term IO structure
	 * @return status
	 */
	int tcsetattr(int port, int mode, Termios termios);

	/**
	 * Represents a file pointer.
	 *
	 */
	public class FILE extends PointerType {
	}

	/**
	 * Terminal IO structure
	 */
	public class Termios extends Structure {

		/**
		 * Input flags
		 */
		public int c_iflag;
		/**
		 * Output flags
		 */
		public int c_oflag;
		/**
		 * Control modes
		 */
		public int c_cflag;
		/**
		 * Local modes
		 */
		public int c_lflag;
		/**
		 * Line discipline
		 */
		public byte c_line;
		/**
		 * Special characters
		 */
		public byte[] c_cc = new byte[(32)];
		/**
		 * Input speed
		 */
		public int c_ispeed;
		/**
		 * Output speed
		 */
		public int c_ospeed;

		/**
		 * Constructor
		 */
		public Termios() {
		}

		/**
		 * Construct a new terminal IO structure from another
		 * 
		 * @param termios
		 *            structure to copy from
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

		@Override
		public String toString() {
			return "Termios [c_iflag=" + c_iflag + ", c_oflag=" + c_oflag + ", c_cflag=" + c_cflag + ", c_lflag="
					+ c_lflag + ", c_line=" + c_line + ", c_cc=" + Arrays.toString(c_cc) + ", c_ispeed=" + c_ispeed
					+ ", c_ospeed=" + c_ospeed + "]";
		}

	}
}
