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

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

/**
 * Interface to select calls from the Userenv.dll library on Windows.
 *
 */
public interface Userenv extends StdCallLibrary {
	/**
	 * Instance
	 */
	Userenv INSTANCE = (Userenv) Native.loadLibrary("userenv", Userenv.class, W32APIOptions.UNICODE_OPTIONS);

	/**
	 * Retrieves the environment variables for the specified user. This block
	 * can then be passed to the CreateProcessAsUser function.
	 * 
	 * @param lpEnvironment
	 *            environment pointer to populate
	 * @param phToken
	 *            the token
	 * @param inherit
	 *            inherit variables from this process
	 * @return ok
	 */
	boolean CreateEnvironmentBlock(PointerByReference lpEnvironment, HANDLE phToken, boolean inherit);

	/**
	 * Frees environment variables created by the CreateEnvironmentBlock
	 * function.
	 * 
	 * @param lpEnvironment
	 *            Pointer to the environment block created by
	 *            CreateEnvironmentBlock. The environment block is an array of
	 *            null-terminated Unicode strings. The list ends with two nulls
	 *            (\0\0).
	 * @return TRUE if successful; otherwise, FALSE. To get extended error
	 *         information, call GetLastError.
	 */
	boolean DestroyEnvironmentBlock(Pointer lpEnvironment);

	/**
	 * Retrieves the path to the root directory of the specified user's profile.
	 * 
	 * @param phToken
	 *            A token for the user, which is returned by the LogonUser,
	 *            CreateRestrictedToken, DuplicateToken, OpenProcessToken, or
	 *            OpenThreadToken function. The token must have TOKEN_QUERY
	 *            access. For more information, see Access Rights for
	 *            Access-Token Objects.
	 * @param lpProfileDir
	 *            A pointer to a buffer that, when this function returns
	 *            successfully, receives the path to the specified user's
	 *            profile directory.
	 * @param lpcchSize
	 *            Specifies the size of the lpProfileDir buffer, in TCHARs.
	 * 
	 *            If the buffer specified by lpProfileDir is not large enough or
	 *            lpProfileDir is NULL, the function fails and this parameter
	 *            receives the necessary buffer size, including the terminating
	 *            null character.
	 * @return TRUE if successful; otherwise, FALSE. To get extended error
	 *         information, call GetLastError.
	 */
	boolean GetUserProfileDirectoryW(HANDLE phToken, char[] lpProfileDir, IntByReference lpcchSize);

}
