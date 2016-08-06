package com.sshtools.forker.common;

/**
 * Various constants used by the Forker Daemon protocol.
 *
 */
public class States {

	/**
	 * Remote command or operation executed OK
	 */
	public final static int OK = 0;
	/**
	 * Remote command or operation failed to execute OK
	 */
	public final static int FAILED = 1;
	/**
	 * Apply operation to input stream
	 */
	public final static int IN = 2;
	/**
	 * Apply operation to output stream
	 */
	public final static int ERR = 3;
	/**
	 * End of stream
	 */
	public final static int END = 4;
	/**
	 * Apply operation to output stream
	 */
	public final static int OUT = 5;
	/**
	 * Kill process
	 */
	public final static int KILL = 6;
	/**
	 * Close output stream
	 */
	public final static int CLOSE_OUT = 7;
	/**
	 * Close error stream
	 */
	public final static int CLOSE_ERR = 8;
	/**
	 * Close input stream
	 */
	public final static int CLOSE_IN = 9;
	/**
	 * Flush output stream
	 */
	public final static int FLUSH_OUT = 10;
	/**
	 * Window size changed (either direction)
	 */
	public final static int WINDOW_SIZE = 11;
}
