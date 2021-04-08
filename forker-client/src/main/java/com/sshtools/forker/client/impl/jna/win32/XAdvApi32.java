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
package com.sshtools.forker.client.impl.jna.win32;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.WinBase.PROCESS_INFORMATION;
import com.sun.jna.platform.win32.WinBase.STARTUPINFO;

/**
 * Advapi32
 */
public interface XAdvApi32 extends Advapi32 {
	/**
	 * The instance
	 */
	XAdvApi32 INSTANCE = (XAdvApi32) Native.loadLibrary("AdvApi32", XAdvApi32.class);

	/**
	 * Creates a new process and its primary thread. Then the new process runs
	 * the specified executable file in the security context of the specified
	 * credentials (user, domain, and password). It can optionally load the user
	 * profile for a specified user.
	 * 
	 * @param lpUsername
	 * @param lpDomain
	 * @param lpPassword
	 * @param dwLogonFlags
	 * @param lpApplicationName
	 * @param lpCommandLine
	 * @param dwCreationFlags
	 * @param lpEnvironment
	 * @param lpCurrentDirectory
	 * @param lpStartupInfo
	 * @param lpProcessInfo
	 * @return status
	 */
	boolean CreateProcessWithLogonW(WString lpUsername, WString lpDomain, WString lpPassword, int dwLogonFlags,
			WString lpApplicationName, WString lpCommandLine, int dwCreationFlags, Pointer lpEnvironment,
			WString lpCurrentDirectory, STARTUPINFO lpStartupInfo, PROCESS_INFORMATION lpProcessInfo);

	/**
	 * Log on, then load the user's profile in the HKEY_USERS registry key. The
	 * function returns after the profile has been loaded. Loading the profile
	 * can be time-consuming, so it is best to use this value only if you must
	 * access the information in the HKEY_CURRENT_USER registry key.
	 */
	public static final int LOGON_WITH_PROFILE = 0x00000001;
	/**
	 * Log on, but use the specified credentials on the network only. The new
	 * process uses the same token as the caller, but the system creates a new
	 * logon session within LSA, and the process uses the specified credentials
	 * as the default credentials.
	 * 
	 * This value can be used to create a process that uses a different set of
	 * credentials locally than it does remotely. This is useful in inter-domain
	 * scenarios where there is no trust relationship.
	 * 
	 */
	public static final int LOGON_NETCREDENTIALS_ONLY = 0x00000002;
	/**
	 * The process is a console application that is being run without a console
	 * window. Therefore, the console handle for the application is not set.
	 */
	int CREATE_NO_WINDOW = 0x08000000;
	/**
	 * If this flag is set, the environment block pointed to by lpEnvironment
	 * uses Unicode characters. Otherwise, the environment block uses ANSI
	 * characters.
	 */
	int CREATE_UNICODE_ENVIRONMENT = 0x00000400;
	/**
	 * The new process has a new console, instead of inheriting its parent's
	 * console (the default). For more information, see Creation of a Console.
	 * 
	 * This flag cannot be used with DETACHED_PROCESS.
	 */
	int CREATE_NEW_CONSOLE = 0x00000010;
	/**
	 * For console processes, the new process does not inherit its parent's
	 * console (the default). The new process can call the AllocConsole function
	 * at a later time to create a console. For more information, see Creation
	 * of a Console. This value cannot be used with CREATE_NEW_CONSOLE.
	 * 
	 */
	int DETACHED_PROCESS = 0x00000008;
}
