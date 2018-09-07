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
	Kernel32 INSTANCE = Native.loadLibrary("kernel32", Kernel32.class, W32APIOptions.DEFAULT_OPTIONS);

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
