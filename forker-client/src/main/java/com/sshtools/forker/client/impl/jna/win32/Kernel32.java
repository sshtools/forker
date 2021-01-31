package com.sshtools.forker.client.impl.jna.win32;

import java.nio.ByteBuffer;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.W32APIOptions;

/**
 * Extends JNA Platform Kernel32 a little.
 */
public interface Kernel32 extends com.sun.jna.platform.win32.Kernel32 {
	/** The instance. */
	Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class, W32APIOptions.DEFAULT_OPTIONS);

	/**
	 * @param hFile
	 * @param lpBuffer
	 * @param nNumberOfBytesToRead
	 * @param lpNumberOfBytesRead
	 * @param lpOverlapped
	 * @return status
	 */
	int ReadFile(HANDLE hFile, ByteBuffer lpBuffer, int nNumberOfBytesToRead, IntByReference lpNumberOfBytesRead,
			OVERLAPPED lpOverlapped);

	/**
	 * @param hFile
	 * @param lpBuffer
	 * @param nNumberOfBytesToWrite
	 * @param lpNumberOfBytesWritten
	 * @param lpOverlapped
	 * @return status
	 */
	int WriteFile(HANDLE hFile, ByteBuffer lpBuffer, int nNumberOfBytesToWrite, IntByReference lpNumberOfBytesWritten,
			OVERLAPPED lpOverlapped);

	/**
	 * @param hThread
	 * @return status
	 */
	DWORD ResumeThread(HANDLE hThread);

	/**
	 * @param lpApplicationName
	 * @param lpCommandLine
	 * @param lpProcessAttributes
	 * @param lpThreadAttributes
	 * @param bInheritHandles
	 * @param dwCreationFlags
	 * @param lpEnvironment
	 * @param lpCurrentDirectory
	 * @param lpStartupInfo
	 * @param lpProcessInformation
	 * @return status
	 */
	boolean CreateProcessW(WString lpApplicationName, char[] lpCommandLine, SECURITY_ATTRIBUTES lpProcessAttributes,
			SECURITY_ATTRIBUTES lpThreadAttributes, boolean bInheritHandles, DWORD dwCreationFlags, Pointer lpEnvironment,
			char[] lpCurrentDirectory, STARTUPINFO lpStartupInfo, PROCESS_INFORMATION lpProcessInformation);

	/**
	 * @param lpFileName
	 * @param dwDesiredAccess
	 * @param dwShareMode
	 * @param lpSecurityAttributes
	 * @param dwCreationDisposition
	 * @param dwFlagsAndAttributes
	 * @param hTemplateFile
	 * @return status
	 */
	HANDLE CreateFile(WString lpFileName, int dwDesiredAccess, int dwShareMode,
			SECURITY_ATTRIBUTES lpSecurityAttributes, int dwCreationDisposition, int dwFlagsAndAttributes, HANDLE hTemplateFile);
	/**
	 * @param name
	 * @param dwOpenMode
	 * @param dwPipeMode
	 * @param nMaxInstances
	 * @param nOutBufferSize
	 * @param nInBufferSize
	 * @param nDefaultTimeOut
	 * @param securityAttributes
	 * @return status
	 */
	HANDLE CreateNamedPipeW(WString name, int dwOpenMode, int dwPipeMode, int nMaxInstances,
			int nOutBufferSize, int nInBufferSize, int nDefaultTimeOut, SECURITY_ATTRIBUTES securityAttributes);
}
