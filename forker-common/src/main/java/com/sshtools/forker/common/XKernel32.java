package com.sshtools.forker.common;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.win32.W32APIOptions;

public interface XKernel32 extends Kernel32 {
	/**
	 * Instance
	 */
	XKernel32 INSTANCE = Native.load("Kernel32", XKernel32.class, W32APIOptions.UNICODE_OPTIONS);

	int SetCurrentDirectoryW(String path);
}
