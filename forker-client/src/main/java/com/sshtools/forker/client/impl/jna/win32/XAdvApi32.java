package com.sshtools.forker.client.impl.jna.win32;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.WinBase.PROCESS_INFORMATION;
import com.sun.jna.platform.win32.WinBase.STARTUPINFO;

public interface XAdvApi32 extends Advapi32 {
  XAdvApi32 INSTANCE =
        (XAdvApi32) Native.loadLibrary("AdvApi32", XAdvApi32.class);

  boolean CreateProcessWithLogonW
            (WString lpUsername,
             WString lpDomain,
             WString lpPassword,
             int dwLogonFlags,
             WString lpApplicationName,
             WString lpCommandLine,
             int dwCreationFlags,
             Pointer lpEnvironment,
             WString lpCurrentDirectory,
             STARTUPINFO  lpStartupInfo,
             PROCESS_INFORMATION lpProcessInfo);

  public static final int LOGON_WITH_PROFILE          = 0x00000001;
  public static final int LOGON_NETCREDENTIALS_ONLY   = 0x00000002;


  int CREATE_NO_WINDOW            = 0x08000000;
  int CREATE_UNICODE_ENVIRONMENT  = 0x00000400;
  int CREATE_NEW_CONSOLE          = 0x00000010;
  int DETACHED_PROCESS            = 0x00000008;
}
