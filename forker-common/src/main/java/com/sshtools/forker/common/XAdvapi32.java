package com.sshtools.forker.common;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinBase.PROCESS_INFORMATION;
import com.sun.jna.platform.win32.WinBase.STARTUPINFO;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.W32APIOptions;

public interface XAdvapi32 extends Advapi32 {

	/**
	 * Log on, then load the user's profile in the HKEY_USERS registry key. The
	 * function returns after the profile has been loaded. Loading the profile
	 * can be time-consuming, so it is best to use this value only if you must
	 * access the information in the HKEY_CURRENT_USER registry key.
	 * 
	 * Windows Server 2003: The profile is unloaded after the new process has
	 * been terminated, regardless of whether it has created child processes.
	 */
	public final static int LOGON_WITH_PROFILE = 0x00000001;
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
	 * The system does not validate the specified credentials. Therefore, the
	 * process can start, but it may not have access to network resources.
	 */
	public final static int LOGON_NETCREDENTIALS_ONLY = 0x00000002;

	/**
	 * The new process does not inherit the error mode of the calling process.
	 * Instead, the new process gets the current default error mode. An
	 * application sets the current default error mode by calling SetErrorMode.
	 * 
	 * This flag is enabled by default.
	 */
	public final static int CREATE_DEFAULT_ERROR_MODE = 0x04000000;
	/**
	 * The new process has a new console, instead of inheriting the parent's
	 * console. This flag cannot be used with the DETACHED_PROCESS flag.
	 * 
	 * This flag is enabled by default.
	 */
	public final static int CREATE_NEW_CONSOLE = 0x00000010;
	/**
	 * The new process is the root process of a new process group. The process
	 * group includes all processes that are descendants of this root process.
	 * The process identifier of the new process group is the same as the
	 * process identifier, which is returned in the lpProcessInfo parameter.
	 * Process groups are used by the GenerateConsoleCtrlEvent function to
	 * enable sending a CTRL+C or CTRL+BREAK signal to a group of console
	 * processes.
	 * 
	 * This flag is enabled by default.
	 */
	public final static int CREATE_NEW_PROCESS_GROUP = 0x00000200;
	/**
	 * This flag is only valid starting a 16-bit Windows-based application. If
	 * set, the new process runs in a private Virtual DOS Machine (VDM). By
	 * default, all 16-bit Windows-based applications run in a single, shared
	 * VDM. The advantage of running separately is that a crash only terminates
	 * the single VDM; any other programs running in distinct VDMs continue to
	 * function normally. Also, 16-bit Windows-based applications that run in
	 * separate VDMs have separate input queues. That means that if one
	 * application stops responding momentarily, applications in separate VDMs
	 * continue to receive input.
	 */
	public final static int CREATE_SEPARATE_WOW_VDM = 0x00000800;
	/**
	 * The primary thread of the new process is created in a suspended state,
	 * and does not run until the ResumeThread function is called.
	 */
	public final static int CREATE_SUSPENDED = 0x00000004;
	/**
	 * Indicates the format of the lpEnvironment parameter. If this flag is set,
	 * the environment block pointed to by lpEnvironment uses Unicode
	 * characters. Otherwise, the environment block uses ANSI characters.
	 */
	public final static int CREATE_UNICODE_ENVIRONMENT = 0x00000400;
	/**
	 * The process is a console application that is being run without a console
	 * window. Therefore, the console handle for the application is not set.
	 * 
	 * This flag is ignored if the application is not a console application, or
	 * if it is used with either CREATE_NEW_CONSOLE or DETACHED_PROCESS.
	 */
	public final static int CREATE_NO_WINDOW = 0x08000000;
	/**
	 * The process is created with extended startup information; the
	 * lpStartupInfo parameter specifies a STARTUPINFOEX structure.
	 * 
	 * Windows Server 2003: This value is not supported.
	 */
	public final static int EXTENDED_STARTUPINFO_PRESENT = 0x00080000;

	XAdvapi32 INSTANCE = (XAdvapi32) Native.loadLibrary("Advapi32", XAdvapi32.class, W32APIOptions.UNICODE_OPTIONS);

	/**
	 * Creates a new process and its primary thread. The new process runs in the
	 * security context of the specified token. It can optionally load the user
	 * profile for the specified user.
	 *
	 * The process that calls CreateProcessWithTokenW must have the
	 * SE_IMPERSONATE_NAME privilege. If this function fails with
	 * ERROR_PRIVILEGE_NOT_HELD (1314), use the CreateProcessAsUser or
	 * CreateProcessWithLogonW function instead. Typically, the process that
	 * calls CreateProcessAsUser must have the SE_INCREASE_QUOTA_NAME privilege
	 * and may require the SE_ASSIGNPRIMARYTOKEN_NAME privilege if the token is
	 * not assignable. CreateProcessWithLogonW requires no special privileges,
	 * but the specified user account must be allowed to log on interactively.
	 * Generally, it is best to use CreateProcessWithLogonW to create a process
	 * with alternate credentials.
	 *
	 * @param hToken
	 *            A handle to the primary token that represents a user.
	 * @param dwLogonFlags
	 *            The logon option.. For a list of values, see Logon Flags.
	 * @param lpApplicationName
	 *            The name of the module to be executed.
	 * @param lpCommandLine
	 *            The command line to be executed.
	 * @param dwCreationFlags
	 *            The flags that control the priority class and the creation of
	 *            the process. For a list of values, see Process Creation Flags.
	 * @param lpEnvironment
	 *            A pointer to an environment block for the new process. If this
	 *            parameter is NULL, the new process uses the environment of the
	 *            calling process.
	 *
	 *            An environment block consists of a null-terminated block of
	 *            null-terminated strings. Each string is in the following form:
	 *            name=value\0
	 * @param lpCurrentDirectory
	 *            The full path to the current directory for the process. The
	 *            string can also specify a UNC path.
	 * @param lpStartupInfo
	 *            A pointer to a STARTUPINFO or STARTUPINFOEX structure.
	 * @param lpProcessInformation
	 *            A pointer to a PROCESS_INFORMATION structure that receives
	 *            identification information about the new process.
	 * @return If the function succeeds, the return value is nonzero. If the
	 *         function fails, the return value is zero. To get extended error
	 *         information, call GetLastError.
	 */
	public boolean CreateProcessWithTokenW(HANDLE hToken, int dwLogonFlags, String lpApplicationName,
			String lpCommandLine, int dwCreationFlags, String lpEnvironment, String lpCurrentDirectory,
			STARTUPINFO lpStartupInfo, PROCESS_INFORMATION lpProcessInformation);

	boolean CreateProcessWithLogonW(String lpUsername, String lpDomain, String lpPassword, int dwLogonFlags,
			String lpApplicationName, String lpCommandLine, int dwCreationFlags, Pointer lpEnvironment,
			String lpCurrentDirectory, STARTUPINFO lpStartupInfo, PROCESS_INFORMATION lpProcessInfo);
}
