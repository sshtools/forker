/*
 * The contents of this file is dual-licensed under 2
 * alternative Open Source/Free licenses: LGPL 2.1 or later and
 * Apache License 2.0. (starting with JNA version 4.0.0).
 *
 * You can freely decide which license you want to apply to
 * the project.
 *
 * You may obtain a copy of the LGPL License at:
 *
 * http://www.gnu.org/licenses/licenses.html
 *
 * A copy is also included in the downloadable source code package
 * containing JNA, in file "LGPL2.1".
 *
 * You may obtain a copy of the Apache License at:
 *
 * http://www.apache.org/licenses/
 *
 * A copy is also included in the downloadable source code package
 * containing JNA, in file "AL2.0".
 */
package com.sshtools.forker.wrapper;

import java.nio.ByteBuffer;

import com.sun.jna.Library;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.Winsvc;
import com.sun.jna.platform.win32.Winsvc.HandlerEx;
import com.sun.jna.platform.win32.Winsvc.SERVICE_MAIN_FUNCTION;
import com.sun.jna.platform.win32.Winsvc.SERVICE_STATUS;
import com.sun.jna.platform.win32.Winsvc.SERVICE_STATUS_HANDLE;
import com.sun.jna.platform.win32.Winsvc.SERVICE_TABLE_ENTRY;

/**
 * Baseclass for a Win32 service.
 */
public abstract class Win32ServiceBase {

	/**
	 * Implementation of the service control function.
	 */
	private class ServiceControl implements HandlerEx {

		/**
		 * Called when the service get a control code.
		 *
		 * @param dwControl
		 * @param dwEventType
		 * @param lpEventData
		 * @param lpContext
		 */
		public int callback(int dwControl, int dwEventType, Pointer lpEventData, Pointer lpContext) {
			log("ServiceControl.callback() - %d, %d", dwControl, dwEventType);
			switch (dwControl) {
			case Winsvc.SERVICE_CONTROL_STOP:
			case Winsvc.SERVICE_CONTROL_SHUTDOWN:
				onStop();
				synchronized (waitObject) {
					waitObject.notifyAll();
				}
				break;
			case Winsvc.SERVICE_CONTROL_PAUSE:
				onPause();
				break;
			case Winsvc.SERVICE_CONTROL_CONTINUE:
				onContinue();
				break;
			}
			return WinError.NO_ERROR;
		}
	}

	/**
	 * Implementation of the service main function.
	 */
	private class ServiceMain implements SERVICE_MAIN_FUNCTION {

		/**
		 * Called when the service is starting.
		 *
		 * @param dwArgc   number of arguments
		 * @param lpszArgv pointer to arguments
		 */
		public void callback(int dwArgc, Pointer lpszArgv) {
			log("ServiceMain.callback() - %d", dwArgc);

			serviceControl = new ServiceControl();
			serviceStatusHandle = Advapi32.INSTANCE.RegisterServiceCtrlHandlerEx(name, serviceControl, null);

			reportStatus(Winsvc.SERVICE_START_PENDING, WinError.NO_ERROR, 25000);

			log("callback() - starting network");
			start();
			onStart();

			try {
				synchronized (waitObject) {
					waitObject.wait();
				}
			} catch (InterruptedException ex) {
			}
			log("callback() - reporting SERVICE_STOPPED");
			reportStatus(Winsvc.SERVICE_STOPPED, WinError.NO_ERROR, 0);

			// Avoid returning from ServiceMain, which will cause a crash
			// See http://support.microsoft.com/kb/201349, which recommends
			// having init() wait for this thread.
			// Waiting on this thread in init() won't fix the crash, though.
			// System.exit(0);
		}
	}

	public static interface TunnelInterface extends Library {
		/** Unused, keys are generated using Java */
		void WireGuardGenerateKeyPair(ByteBuffer publicKey, ByteBuffer privateKey);

		boolean WireGuardTunnelService(WString confFile);
	}

	public static TunnelInterface INSTANCE;

	private static void log(String msgFmt, Object... args) {
		System.out.println(String.format(msgFmt, args));
	}

	private final Object waitObject = new Object();

	private ServiceMain serviceMain;
	private ServiceControl serviceControl;
	private SERVICE_STATUS_HANDLE serviceStatusHandle;
	private String name;

	public Win32ServiceBase() {
	}

	public Win32ServiceBase(String name) {
		this.name = name;
		System.out.println(String.format("Preparing Wireguard configuration for %s (in %s)", name));
	}

	public abstract void start();

	/**
	 * Initialize the service, connect to the ServiceControlManager.
	 */
	public void init() {
		log("init() - setting up table. name: %s service name: %s  conf: %s", name);
		serviceMain = new ServiceMain();
		SERVICE_TABLE_ENTRY entry = new SERVICE_TABLE_ENTRY();
		entry.lpServiceName = name;
		entry.lpServiceProc = serviceMain;

		log("init() - starting the dispatcher");
		Advapi32.INSTANCE.StartServiceCtrlDispatcher((SERVICE_TABLE_ENTRY[]) entry.toArray(2));
		log("init() - started the dispatcher");
	}

	/*
	 * Called when service should stop.
	 */
	public void onContinue() {
		log("init() - continue");
		reportStatus(Winsvc.SERVICE_RUNNING, WinError.NO_ERROR, 0);
	}

	/*
	 * Called when service should stop.
	 */
	public void onPause() {
		log("init() - paused");
		reportStatus(Winsvc.SERVICE_PAUSED, WinError.NO_ERROR, 0);
	}

	/**
	 * Called when service is starting.
	 */
	public void onStart() {
		log("init() - service starting");
		reportStatus(Winsvc.SERVICE_RUNNING, WinError.NO_ERROR, 0);
	}

	/*
	 * Called when service should stop.
	 */
	public void onStop() {
		log("init() - stop pending");
		reportStatus(Winsvc.SERVICE_STOP_PENDING, WinError.NO_ERROR, 25000);
	}

	/**
	 * Report service status to the ServiceControlManager.
	 *
	 * @param status        status
	 * @param win32ExitCode exit code
	 * @param waitHint      time to wait
	 */
	private void reportStatus(int status, int win32ExitCode, int waitHint) {
		SERVICE_STATUS serviceStatus = new SERVICE_STATUS();
		serviceStatus.dwServiceType = WinNT.SERVICE_WIN32_OWN_PROCESS;
		serviceStatus.dwControlsAccepted = status == Winsvc.SERVICE_START_PENDING ? 0
				: (Winsvc.SERVICE_ACCEPT_STOP | Winsvc.SERVICE_ACCEPT_SHUTDOWN | Winsvc.SERVICE_CONTROL_PAUSE
						| Winsvc.SERVICE_CONTROL_CONTINUE);
		serviceStatus.dwWin32ExitCode = win32ExitCode;
		serviceStatus.dwWaitHint = waitHint;
		serviceStatus.dwCurrentState = status;

		Advapi32.INSTANCE.SetServiceStatus(serviceStatusHandle, serviceStatus);
	}

}
