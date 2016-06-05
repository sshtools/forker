package com.sshtools.forker.common;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

public interface Userenv extends StdCallLibrary {
	Userenv INSTANCE = (Userenv) Native.loadLibrary("userenv", Userenv.class, W32APIOptions.UNICODE_OPTIONS);

	boolean CreateEnvironmentBlock(PointerByReference lpEnvironment, HANDLE phToken, boolean inherit);

	boolean DestroyEnvironmentBlock(Pointer lpEnvironment);

	boolean GetUserProfileDirectoryW(HANDLE phToken, char[] lpProfileDir, IntByReference lpcchSize);

}
