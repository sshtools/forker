package com.sshtools.forker.common;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.WinBase.PROCESS_INFORMATION;
import com.sun.jna.platform.win32.WinBase.STARTUPINFO;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.win32.W32APIOptions;

/**
 * Extension the JNA Platform's {@link Advapi32} that adds some functions
 * required by Forker.
 */
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

	/**
	 * Instance
	 */
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

	/**
	 * Creates a new process and its primary thread. Then the new process runs
	 * the specified executable file in the security context of the specified
	 * credentials (user, domain, and password). It can optionally load the user
	 * profile for a specified user.
	 * 
	 * This function is similar to the CreateProcessAsUser and
	 * CreateProcessWithTokenW functions, except that the caller does not need
	 * to call the LogonUser function to authenticate the user and get a token.
	 * 
	 * @param lpUsername
	 * 
	 *            The name of the user. This is the name of the user account to
	 *            log on to. If you use the UPN format, user@DNS_domain_name,
	 *            the lpDomain parameter must be NULL.
	 * 
	 *            The user account must have the Log On Locally permission on
	 *            the local computer. This permission is granted to all users on
	 *            workstations and servers, but only to administrators on domain
	 *            controllers.
	 * 
	 * @param lpDomain
	 *            The name of the domain or server whose account database
	 *            contains the lpUsername account. If this parameter is NULL,
	 *            the user name must be specified in UPN format.
	 * @param lpPassword
	 * 
	 *            The clear-text password for the lpUsername account.
	 * 
	 * @param dwLogonFlags
	 *            The logon option. This parameter can be 0 (zero) or one of the
	 *            following values.
	 * 
	 *            LOGON_WITH_PROFILE 0x00000001
	 * 
	 * 
	 *            Log on, then load the user profile in the HKEY_USERS registry
	 *            key. The function returns after the profile is loaded. Loading
	 *            the profile can be time-consuming, so it is best to use this
	 *            value only if you must access the information in the
	 *            HKEY_CURRENT_USER registry key.
	 * 
	 *            Windows Server 2003: The profile is unloaded after the new
	 *            process is terminated, whether or not it has created child
	 *            processes.
	 * 
	 *            Windows XP: The profile is unloaded after the new process and
	 *            all child processes it has created are terminated.
	 * 
	 *            LOGON_NETCREDENTIALS_ONLY 0x00000002
	 * 
	 * 
	 *            Log on, but use the specified credentials on the network only.
	 *            The new process uses the same token as the caller, but the
	 *            system creates a new logon session within LSA, and the process
	 *            uses the specified credentials as the default credentials.
	 * 
	 *            This value can be used to create a process that uses a
	 *            different set of credentials locally than it does remotely.
	 *            This is useful in inter-domain scenarios where there is no
	 *            trust relationship.
	 * 
	 *            The system does not validate the specified credentials.
	 *            Therefore, the process can start, but it may not have access
	 *            to network resources.
	 * @param lpApplicationName
	 * 
	 *            The name of the module to be executed. This module can be a
	 *            Windows-based application. It can be some other type of module
	 *            (for example, MS-DOS or OS/2) if the appropriate subsystem is
	 *            available on the local computer.
	 * 
	 *            The string can specify the full path and file name of the
	 *            module to execute or it can specify a partial name. If it is a
	 *            partial name, the function uses the current drive and current
	 *            directory to complete the specification. The function does not
	 *            use the search path. This parameter must include the file name
	 *            extension; no default extension is assumed.
	 * 
	 *            The lpApplicationName parameter can be NULL, and the module
	 *            name must be the first white space–delimited token in the
	 *            lpCommandLine string. If you are using a long file name that
	 *            contains a space, use quoted strings to indicate where the
	 *            file name ends and the arguments begin; otherwise, the file
	 *            name is ambiguous.
	 * 
	 *            For example, the following string can be interpreted in
	 *            different ways:
	 * 
	 *            "c:\program files\sub dir\program name"
	 * 
	 *            The system tries to interpret the possibilities in the
	 *            following order:
	 * 
	 *            c:\program.exe files\sub dir\program name c:\program
	 *            files\sub.exe dir\program name c:\program files\sub
	 *            dir\program.exe name c:\program files\sub dir\program name.exe
	 * 
	 *            If the executable module is a 16-bit application,
	 *            lpApplicationName should be NULL, and the string pointed to by
	 *            lpCommandLine should specify the executable module and its
	 *            arguments.
	 * 
	 * @param lpCommandLine
	 * 
	 *            The command line to be executed. The maximum length of this
	 *            string is 1024 characters. If lpApplicationName is NULL, the
	 *            module name portion of lpCommandLine is limited to MAX_PATH
	 *            characters.
	 * 
	 *            The function can modify the contents of this string.
	 *            Therefore, this parameter cannot be a pointer to read-only
	 *            memory (such as a const variable or a literal string). If this
	 *            parameter is a constant string, the function may cause an
	 *            access violation.
	 * 
	 *            The lpCommandLine parameter can be NULL, and the function uses
	 *            the string pointed to by lpApplicationName as the command
	 *            line.
	 * 
	 *            If both lpApplicationName and lpCommandLine are non-NULL,
	 *            *lpApplicationName specifies the module to execute, and
	 *            *lpCommandLine specifies the command line. The new process can
	 *            use GetCommandLine to retrieve the entire command line.
	 *            Console processes written in C can use the argc and argv
	 *            arguments to parse the command line. Because argv[0] is the
	 *            module name, C programmers typically repeat the module name as
	 *            the first token in the command line.
	 * 
	 *            If lpApplicationName is NULL, the first white space–delimited
	 *            token of the command line specifies the module name. If you
	 *            are using a long file name that contains a space, use quoted
	 *            strings to indicate where the file name ends and the arguments
	 *            begin (see the explanation for the lpApplicationName
	 *            parameter). If the file name does not contain an extension,
	 *            .exe is appended. Therefore, if the file name extension is
	 *            .com, this parameter must include the .com extension. If the
	 *            file name ends in a period with no extension, or if the file
	 *            name contains a path, .exe is not appended. If the file name
	 *            does not contain a directory path, the system searches for the
	 *            executable file in the following sequence:
	 * 
	 *            The directory from which the application loaded. The current
	 *            directory for the parent process. The 32-bit Windows system
	 *            directory. Use the GetSystemDirectory function to get the path
	 *            of this directory. The 16-bit Windows system directory. There
	 *            is no function that obtains the path of this directory, but it
	 *            is searched. The Windows directory. Use the
	 *            GetWindowsDirectory function to get the path of this
	 *            directory. The directories that are listed in the PATH
	 *            environment variable. Note that this function does not search
	 *            the per-application path specified by the App Paths registry
	 *            key. To include this per-application path in the search
	 *            sequence, use the ShellExecute function.
	 * 
	 *            The system adds a null character to the command line string to
	 *            separate the file name from the arguments. This divides the
	 *            original string into two strings for internal processing.
	 * 
	 * @param dwCreationFlags
	 *            he flags that control how the process is created. The
	 *            CREATE_DEFAULT_ERROR_MODE, CREATE_NEW_CONSOLE, and
	 *            CREATE_NEW_PROCESS_GROUP flags are enabled by default— even if
	 *            you do not set the flag, the system functions as if it were
	 *            set. You can specify additional flags as noted. Value Meaning
	 * 
	 *            CREATE_DEFAULT_ERROR_MODE 0x04000000
	 * 
	 * 
	 * 
	 *            The new process does not inherit the error mode of the calling
	 *            process. Instead, CreateProcessWithLogonW gives the new
	 *            process the current default error mode. An application sets
	 *            the current default error mode by calling SetErrorMode.
	 * 
	 *            This flag is enabled by default.
	 * 
	 *            CREATE_NEW_CONSOLE 0x00000010
	 * 
	 * 
	 * 
	 *            The new process has a new console, instead of inheriting the
	 *            parent's console. This flag cannot be used with the
	 *            DETACHED_PROCESS flag.
	 * 
	 *            This flag is enabled by default.
	 * 
	 *            CREATE_NEW_PROCESS_GROUP 0x00000200
	 * 
	 * 
	 * 
	 *            The new process is the root process of a new process group.
	 *            The process group includes all processes that are descendants
	 *            of this root process. The process identifier of the new
	 *            process group is the same as the process identifier, which is
	 *            returned in the lpProcessInfo parameter. Process groups are
	 *            used by the GenerateConsoleCtrlEvent function to enable
	 *            sending a CTRL+C or CTRL+BREAK signal to a group of console
	 *            processes.
	 * 
	 *            This flag is enabled by default.
	 * 
	 *            CREATE_SEPARATE_WOW_VDM 0x00000800
	 * 
	 * 
	 * 
	 *            This flag is only valid starting a 16-bit Windows-based
	 *            application. If set, the new process runs in a private Virtual
	 *            DOS Machine (VDM). By default, all 16-bit Windows-based
	 *            applications run in a single, shared VDM. The advantage of
	 *            running separately is that a crash only terminates the single
	 *            VDM; any other programs running in distinct VDMs continue to
	 *            function normally. Also, 16-bit Windows-based applications
	 *            that run in separate VDMs have separate input queues, which
	 *            means that if one application stops responding momentarily,
	 *            applications in separate VDMs continue to receive input.
	 * 
	 *            CREATE_SUSPENDED 0x00000004
	 * 
	 * 
	 * 
	 *            The primary thread of the new process is created in a
	 *            suspended state, and does not run until the ResumeThread
	 *            function is called.
	 * 
	 *            CREATE_UNICODE_ENVIRONMENT 0x00000400
	 * 
	 * 
	 * 
	 *            Indicates the format of the lpEnvironment parameter. If this
	 *            flag is set, the environment block pointed to by lpEnvironment
	 *            uses Unicode characters. Otherwise, the environment block uses
	 *            ANSI characters.
	 * 
	 *            EXTENDED_STARTUPINFO_PRESENT 0x00080000
	 * 
	 * 
	 * 
	 *            The process is created with extended startup information; the
	 *            lpStartupInfo parameter specifies a STARTUPINFOEX structure.
	 * 
	 *            Windows Server 2003 and Windows XP: This value is not
	 *            supported.
	 * 
	 * 
	 * 
	 *            This parameter also controls the new process's priority class,
	 *            which is used to determine the scheduling priorities of the
	 *            process's threads. For a list of values, see GetPriorityClass.
	 *            If none of the priority class flags is specified, the priority
	 *            class defaults to NORMAL_PRIORITY_CLASS unless the priority
	 *            class of the creating process is IDLE_PRIORITY_CLASS or
	 *            BELOW_NORMAL_PRIORITY_CLASS. In this case, the child process
	 *            receives the default priority class of the calling process.
	 * @param lpEnvironment
	 * 
	 *            A pointer to an environment block for the new process. If this
	 *            parameter is NULL, the new process uses an environment created
	 *            from the profile of the user specified by lpUsername.
	 * 
	 *            An environment block consists of a null-terminated block of
	 *            null-terminated strings. Each string is in the following form:
	 * 
	 *            name=value
	 * 
	 *            Because the equal sign (=) is used as a separator, it must not
	 *            be used in the name of an environment variable.
	 * 
	 *            An environment block can contain Unicode or ANSI characters.
	 *            If the environment block pointed to by lpEnvironment contains
	 *            Unicode characters, ensure that dwCreationFlags includes
	 *            CREATE_UNICODE_ENVIRONMENT. If this parameter is NULL and the
	 *            environment block of the parent process contains Unicode
	 *            characters, you must also ensure that dwCreationFlags includes
	 *            CREATE_UNICODE_ENVIRONMENT.
	 * 
	 *            An ANSI environment block is terminated by two 0 (zero) bytes:
	 *            one for the last string and one more to terminate the block. A
	 *            Unicode environment block is terminated by four zero bytes:
	 *            two for the last string and two more to terminate the block.
	 * 
	 *            To retrieve a copy of the environment block for a specific
	 *            user, use the CreateEnvironmentBlock function.
	 * 
	 * @param lpCurrentDirectory
	 * 
	 *            The full path to the current directory for the process. The
	 *            string can also specify a UNC path.
	 * 
	 *            If this parameter is NULL, the new process has the same
	 *            current drive and directory as the calling process. This
	 *            feature is provided primarily for shells that need to start an
	 *            application, and specify its initial drive and working
	 *            directory.
	 * 
	 * @param lpStartupInfo
	 *            A pointer to a STARTUPINFO or STARTUPINFOEX structure. The
	 *            application must add permission for the specified user account
	 *            to the specified window station and desktop, even for
	 *            WinSta0\Default.
	 * 
	 *            If the lpDesktop member is NULL or an empty string, the new
	 *            process inherits the desktop and window station of its parent
	 *            process. The application must add permission for the specified
	 *            user account to the inherited window station and desktop.
	 * 
	 *            Windows XP: CreateProcessWithLogonW adds permission for the
	 *            specified user account to the inherited window station and
	 *            desktop.
	 * 
	 *            Handles in STARTUPINFO or STARTUPINFOEX must be closed with
	 *            CloseHandle when they are no longer needed. Important If the
	 *            dwFlags member of the STARTUPINFO structure specifies
	 *            STARTF_USESTDHANDLES, the standard handle fields are copied
	 *            unchanged to the child process without validation. The caller
	 *            is responsible for ensuring that these fields contain valid
	 *            handle values. Incorrect values can cause the child process to
	 *            misbehave or crash. Use the Application Verifier runtime
	 *            verification tool to detect invalid handles.
	 * 
	 * @param lpProcessInfo
	 * 
	 *            A pointer to a PROCESS_INFORMATION structure that receives
	 *            identification information for the new process, including a
	 *            handle to the process.
	 * 
	 *            Handles in PROCESS_INFORMATION must be closed with the
	 *            CloseHandle function when they are not needed.
	 * 
	 * @return If the function succeeds, the return value is nonzero.
	 * 
	 *         If the function fails, the return value is 0 (zero). To get
	 *         extended error information, call GetLastError.
	 * 
	 *         Note that the function returns before the process has finished
	 *         initialization. If a required DLL cannot be located or fails to
	 *         initialize, the process is terminated. To get the termination
	 *         status of a process, call GetExitCodeProcess.
	 */
	boolean CreateProcessWithLogonW(String lpUsername, String lpDomain, String lpPassword, int dwLogonFlags,
			String lpApplicationName, String lpCommandLine, int dwCreationFlags, Pointer lpEnvironment,
			String lpCurrentDirectory, STARTUPINFO lpStartupInfo, PROCESS_INFORMATION lpProcessInfo);
}
