package com.sshtools.forker.services.impl;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.sshtools.forker.client.OSCommand;
import com.sshtools.forker.common.Util.NullOutputStream;
import com.sshtools.forker.services.AbstractService;
import com.sshtools.forker.services.Service;
import com.sshtools.forker.services.ServiceService;
import com.sshtools.forker.services.ServicesContext;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.W32Service;
import com.sun.jna.platform.win32.W32ServiceManager;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.Winsvc;
import com.sun.jna.platform.win32.Winsvc.ENUM_SERVICE_STATUS_PROCESS;
import com.sun.jna.platform.win32.Winsvc.SC_HANDLE;
import com.sun.jna.platform.win32.Winsvc.SERVICE_STATUS_PROCESS;

public class Win32ServiceService extends AbstractServiceService implements ServiceService {
	private static final long POLL_DELAY = 1000;
	private List<Service> services = new ArrayList<>();
	private W32ServiceManager smgr;
	final static Logger LOG = Logger.getLogger(Win32ServiceService.class.getName());

	@Override
	public List<Service> getServices() throws IOException {
		return services;
	}

	@Override
	public void configure(ServicesContext app) {
		smgr = new W32ServiceManager();
		load();
		app.schedule(new Runnable() {
			@Override
			public String toString() {
				return "LoadWin32Services";
			}

			@Override
			public void run() {
				load();
			}
		}, POLL_DELAY, POLL_DELAY, TimeUnit.MILLISECONDS);
	}

	@Override
	public void restartService(Service service) throws Exception {
		if(service.getStatus().isRunning())
			stopService(service);
		startService(service);
	}

	@Override
	public void pauseService(Service service) throws Exception {
		W32Service srv = smgr.openService(service.getNativeName(), WinNT.GENERIC_EXECUTE);
		try {
			srv.pauseService();
		} finally {
			srv.close();
		}
	}

	@Override
	public void unpauseService(Service service) throws Exception {
		W32Service srv = smgr.openService(service.getNativeName(), WinNT.GENERIC_EXECUTE);
		try {
			srv.continueService();
		} finally {
			srv.close();
		}
	}

	@Override
	public void startService(Service service) throws Exception {
		W32Service srv = smgr.openService(service.getNativeName(), WinNT.GENERIC_EXECUTE);
		try {
			srv.startService();
		} finally {
			srv.close();
		}
	}

	@Override
	public void stopService(Service service) throws Exception {
		W32Service srv = smgr.openService(service.getNativeName(), WinNT.GENERIC_EXECUTE);
		try {
			srv.stopService();
		} finally {
			srv.close();
		}
	}

	private void load() {
		synchronized (smgr) {
			try {
				List<Service> oldServices = new ArrayList<>(services);
				List<Service> newServices = new ArrayList<>();
				List<Service> addServices = new ArrayList<>();
				List<Service> updateServices = new ArrayList<>();
				newServices.clear();

				ENUM_SERVICE_STATUS_PROCESS[] srvs = smgr.enumServicesStatusExProcess(WinNT.SERVICE_WIN32,
						Winsvc.SERVICE_STATE_ALL, null);
				for (ENUM_SERVICE_STATUS_PROCESS srv : srvs) {
					Win32Service service = new Win32Service(srv.lpDisplayName);
					addService(oldServices, newServices, addServices, updateServices, service);
				}

//				XAdvapi32 advapi32 = XAdvapi32.INSTANCE;
//				SC_HANDLE serviceManager = Win32ServiceService.getManager(null, Winsvc.SC_MANAGER_ENUMERATE_SERVICE);
//				try {
//					Memory buf = null;
//					int service_data_size = 0;
//					int info = Winsvc.SC_ENUM_PROCESS_INFO;
//
//					boolean retVal;
//
//					IntByReference bytesNeeded = new IntByReference(0);
//					IntByReference srvCount = new IntByReference(0);
//					IntByReference resumeHandle = new IntByReference(0);
//
//					int srvType = WinNT.SERVICE_WIN32;
//					int srvState = Winsvc.SERVICE_STATE_ALL;
//
//					/* Get the required memory size by calling with null data and size of 0 */
//
//					retVal = Advapi32.INSTANCE.EnumServicesStatusEx(serviceManager, info, srvType, srvState, buf,
//							service_data_size, bytesNeeded, srvCount, resumeHandle, null);
//
//					int err = Native.getLastError();
//
//					if ((!retVal) || err == Kernel32.ERROR_MORE_DATA) {
//						/* Need more space */
//
//						int bytesCount = bytesNeeded.getValue();
//
//						buf = new Memory(bytesCount);
//						buf.clear();
//						service_data_size = bytesCount;
//
//						resumeHandle.setValue(0);
//
//						retVal = Advapi32.INSTANCE.EnumServicesStatusEx(serviceManager, info, srvType, srvState, buf,
//								service_data_size, bytesNeeded, srvCount, resumeHandle, null);
//
//						if (!retVal) {
//							err = Native.getLastError();
//							throw new IOException(String.format("Failed to enumerate services. %d. %s", err,
//									Kernel32Util.formatMessageFromLastErrorCode(err)));
//
//						}
//					} else
//						throw new IOException(String.format("Failed to enumerate services. %d. %s", err,
//								Kernel32Util.formatMessageFromLastErrorCode(err)));
//
//					XWinsvc.ENUM_SERVICE_STATUS_PROCESS serviceStatus = new XWinsvc.ENUM_SERVICE_STATUS_PROCESS(buf);
//					Structure[] serviceStatuses = serviceStatus.toArray(srvCount.getValue());
//					for (int i = 0; i < serviceStatuses.length; i++) {
//						XWinsvc.ENUM_SERVICE_STATUS_PROCESS serviceInfo = (XWinsvc.ENUM_SERVICE_STATUS_PROCESS) serviceStatuses[i];
//						Win32Service service = new Win32Service(serviceInfo);
//						addService(oldServices, newServices, addServices, updateServices, service);
//					}
//
//				} finally {
//					advapi32.CloseServiceHandle(serviceManager);
//				}

				services = newServices;
				for (Service s : oldServices) {
					if (newServices.indexOf(s) == -1) {
						fireServiceRemoved(s);
					}
				}
				for (Service s : addServices) {
					fireServiceAdded(s);
				}
				for (Service s : updateServices) {
					fireStateChange(s);
				}
			} finally {
				smgr.close();
			}
		}
	}

	private void addService(List<Service> oldServices, List<Service> newServices, List<Service> addServices,
			List<Service> updateServices, Win32Service service) {
		newServices.add(service);
		// If the service didn't previously exist, or state
		// has
		// changed, fire an event
		int idx = oldServices.indexOf(service);
		if (idx == -1) {
			addServices.add(service);
		} else {
			Service s = oldServices.get(idx);
			if (s.getStatus() != service.getStatus()) {
				updateServices.add(service);
			}
			oldServices.remove(s);
		}
	}

	class Win32Service extends AbstractService {
		public Win32Service(String name) {
			super(name);
		}

		@Override
		public Status getStatus() {
			W32Service srv = smgr.openService(getNativeName(), WinNT.GENERIC_READ);
			try {
				SERVICE_STATUS_PROCESS q = srv.queryStatus();

				if (q.dwCurrentState == Winsvc.SERVICE_RUNNING)
					return Status.STARTED;
				else if (q.dwCurrentState == Winsvc.SERVICE_START_PENDING)
					return Status.STARTING;
				else if (q.dwCurrentState == Winsvc.SERVICE_PAUSE_PENDING)
					return Status.PAUSING;
				else if (q.dwCurrentState == Winsvc.SERVICE_PAUSED)
					return Status.PAUSED;
				else if (q.dwCurrentState == Winsvc.SERVICE_CONTINUE_PENDING)
					return Status.UNPAUSING;
				else if (q.dwCurrentState == Winsvc.SERVICE_STOP_PENDING)
					return Status.STOPPING;
				else if (q.dwCurrentState == Winsvc.SERVICE_STOPPED)
					return Status.STOPPED;
				else
					return Status.UNKNOWN;
			} finally {
				srv.close();
			}
		}
	}

	@Override
	public Service getService(String name) throws IOException {
		for (Service s : getServices()) {
			if (s.getNativeName().equals(name)) {
				return s;
			}
		}
		return null;
	}

	@Override
	public void setStartOnBoot(Service service, boolean startOnBoot) throws Exception {
		boolean enabledByDefault = isStartOnBoot(service);
		if (!startOnBoot && enabledByDefault) {
			try (Closeable o = OSCommand.elevated()) {
				OSCommand.runCommand(new NullOutputStream(),
						Arrays.asList("cmd.exe", "/c", "sc", "config", service.getNativeName(), "start=", "manual"));
			}
		} else if (startOnBoot && !enabledByDefault) {
			try (Closeable o = OSCommand.elevated()) {
				OSCommand.runCommand(new NullOutputStream(),
						Arrays.asList("cmd.exe", "/c", "sc", "config", service.getNativeName(), "start=", "auto"));
			}
		}
	}

	@Override
	public boolean isStartOnBoot(Service service) throws Exception {
		try (Closeable o = OSCommand.elevated()) {
			for (String line : OSCommand.runCommandAndCaptureOutput("cmd.exe", "/c", "sc", "qc",
					service.getNativeName())) {
				if (line.startsWith("START_TYPE")) {
					String[] a = line.split(":");
					int state = Integer.parseInt(a[1].split("\\s+")[0]);
					return state == 2;
				}
			}
		}
		return false;
	}

	public static SC_HANDLE getManager(String machine, int access) {
		SC_HANDLE handle = Advapi32.INSTANCE.OpenSCManager(machine, null, access);
		if (handle == null) {
			int err = Native.getLastError();
			if (err == 5)
				throw new IllegalStateException("Access denied. Check credentials");
			else
				throw new IllegalStateException(String.format("Failed OpenSCManager: %s", Integer.toHexString(err)));
		}
		return (handle);
	}
}
