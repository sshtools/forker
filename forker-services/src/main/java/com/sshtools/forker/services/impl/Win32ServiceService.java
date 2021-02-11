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
import com.sshtools.forker.common.XAdvapi32;
import com.sshtools.forker.common.XWinsvc;
import com.sshtools.forker.services.AbstractService;
import com.sshtools.forker.services.Service;
import com.sshtools.forker.services.Service.Status;
import com.sshtools.forker.services.ServiceService;
import com.sshtools.forker.services.ServicesContext;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.Winsvc;
import com.sun.jna.platform.win32.Winsvc.SC_HANDLE;
import com.sun.jna.platform.win32.Winsvc.SERVICE_STATUS_PROCESS;
import com.sun.jna.ptr.IntByReference;

public class Win32ServiceService extends AbstractServiceService implements ServiceService {
	private static final long POLL_DELAY = 1000;
	private List<Service> services = new ArrayList<>();
	final static Logger LOG = Logger.getLogger(Win32ServiceService.class.getName());

	@Override
	public List<Service> getServices() throws IOException {
		return services;
	}

	@Override
	public void configure(ServicesContext app) {
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
		OSCommand.elevate();
		try {
			OSCommand.runCommand(new NullOutputStream(),
					Arrays.asList("cmd.exe", "/c", "sc", "restart", service.getNativeName()));
		} finally {
			OSCommand.restrict();
			load();
		}
	}

	@Override
	public void pauseService(Service service) throws Exception {
		OSCommand.elevate();
		try {
			OSCommand.runCommand(new NullOutputStream(),
					Arrays.asList("cmd.exe", "/c", "sc", "pause", service.getNativeName()));
		} finally {
			OSCommand.restrict();
			load();
		}
	}

	@Override
	public void unpauseService(Service service) throws Exception {
		OSCommand.elevate();
		try {
			OSCommand.runCommand(new NullOutputStream(),
					Arrays.asList("cmd.exe", "/c", "sc", "continue", service.getNativeName()));
		} finally {
			OSCommand.restrict();
			load();
		}
	}

	@Override
	public void startService(Service service) throws Exception {
		OSCommand.elevate();
		try {
			OSCommand.runCommand(new NullOutputStream(),
					Arrays.asList("cmd.exe", "/c", "sc", "start", service.getNativeName()));
		} finally {
			OSCommand.restrict();
			load();
		}
	}

	@Override
	public void stopService(Service service) throws Exception {
		OSCommand.elevate();
		try {
			OSCommand.runCommand(new NullOutputStream(),
					Arrays.asList("cmd.exe", "/c", "sc", "stop", service.getNativeName()));
		} finally {
			OSCommand.restrict();
			load();
		}
	}

	private void load() {
		try (Closeable o = OSCommand.elevated()) {
			List<Service> oldServices = new ArrayList<>(services);
			List<Service> newServices = new ArrayList<>();
			List<Service> addServices = new ArrayList<>();
			List<Service> updateServices = new ArrayList<>();
			newServices.clear();

			XAdvapi32 advapi32 = XAdvapi32.INSTANCE;
			SC_HANDLE serviceManager = Win32ServiceService.getManager(null, Winsvc.SC_MANAGER_ENUMERATE_SERVICE);
			try {
				Memory buf = null;
				int service_data_size = 0;
				int info = Winsvc.SC_ENUM_PROCESS_INFO;

				boolean retVal;

				IntByReference bytesNeeded = new IntByReference(0);
				IntByReference srvCount = new IntByReference(0);
				IntByReference resumeHandle = new IntByReference(0);

				int srvType = WinNT.SERVICE_WIN32;
				int srvState = Winsvc.SERVICE_STATE_ALL;

				/* Get the required memory size by calling with null data and size of 0 */

				retVal = Advapi32.INSTANCE.EnumServicesStatusEx(serviceManager, info, srvType, srvState, buf,
						service_data_size, bytesNeeded, srvCount, resumeHandle, null);

				int err = Native.getLastError();

				if ((!retVal) || err == Kernel32.ERROR_MORE_DATA) {
					/* Need more space */

					int bytesCount = bytesNeeded.getValue();

					buf = new Memory(bytesCount);
					buf.clear();
					service_data_size = bytesCount;

					resumeHandle.setValue(0);

					retVal = Advapi32.INSTANCE.EnumServicesStatusEx(serviceManager, info, srvType, srvState, buf,
							service_data_size, bytesNeeded, srvCount, resumeHandle, null);

					if (!retVal) {
						err = Native.getLastError();
						throw new IOException(String.format("Failed to enumerate services. %d. %s", err,
								Kernel32Util.formatMessageFromLastErrorCode(err)));

					}
				} else
					throw new IOException(String.format("Failed to enumerate services. %d. %s", err,
							Kernel32Util.formatMessageFromLastErrorCode(err)));

				XWinsvc.ENUM_SERVICE_STATUS_PROCESS serviceStatus = new XWinsvc.ENUM_SERVICE_STATUS_PROCESS(buf);
				Structure[] serviceStatuses = serviceStatus.toArray(srvCount.getValue());
				for (int i = 0; i < serviceStatuses.length; i++) {
					XWinsvc.ENUM_SERVICE_STATUS_PROCESS serviceInfo = (XWinsvc.ENUM_SERVICE_STATUS_PROCESS) serviceStatuses[i];
					Win32Service service = new Win32Service(serviceInfo);
					addService(oldServices, newServices, addServices, updateServices, service);
				}

			} finally {
				advapi32.CloseServiceHandle(serviceManager);
			}

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
		} catch (Exception e) {
			// TODO
			e.printStackTrace();
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

	public static class Win32Service extends AbstractService {
		public Win32Service(XWinsvc.ENUM_SERVICE_STATUS_PROCESS nativeService) {
			super(nativeService.lpServiceName.getWideString(0));
		}

		@Override
		public Status getStatus() {

			XAdvapi32 advapi32 = XAdvapi32.INSTANCE;
			SC_HANDLE serviceManager = getManager(null, WinNT.GENERIC_READ);

			try {

				SC_HANDLE service = advapi32.OpenService(serviceManager, getNativeName(), WinNT.GENERIC_READ);
				if (service != null) {
					try {

						IntByReference pcbBytesNeeded = new IntByReference();

						/* Get size */

//						if (!advapi32.QueryServiceConfig(service, null, 0, pcbBytesNeeded)) {
//
//							/* Data */
//
//							int cbBufSize = pcbBytesNeeded.getValue();
//							if (cbBufSize > 8192)
//								cbBufSize = 8192;
//
//							Memory buffer = new Memory(cbBufSize);
//
//							buffer.clear();
//
//							if (advapi32.QueryServiceConfig(service, buffer, cbBufSize, pcbBytesNeeded)) {
//
//								QUERY_SERVICE_CONFIG lpServiceConfig = new QUERY_SERVICE_CONFIG();
//
//								lpServiceConfig.init(buffer);
//
//								if (lpServiceConfig.dwStartType == Advapi32.SERVICE_DISABLED)
//
//									result |= Service.STATE_DISABLED;
//
//								if (lpServiceConfig.dwStartType == Advapi32.SERVICE_BOOT_START
//										| lpServiceConfig.dwStartType == Advapi32.SERVICE_SYSTEM_START
//
//										| lpServiceConfig.dwStartType == Advapi32.SERVICE_AUTO_START)
//
//									result |= Service.STATE_AUTOMATIC;
//
//								if (lpServiceConfig.dwStartType == Advapi32.SERVICE_DEMAND_START)
//
//									result |= Service.STATE_MANUAL;
//
//								if ((lpServiceConfig.dwServiceType & Advapi32.SERVICE_INTERACTIVE_PROCESS) != 0)
//
//									result |= Service.STATE_INTERACTIVE;
//
//							}
//
//							else {
//								int err = Native.getLastError();
//								throw new IOException(String.format("Failed to get buffer size. %d. %s", err,
//										Kernel32Util.formatMessageFromLastErrorCode(err)));
//							}
//						} else {
//							int err = Native.getLastError();
//							throw new IOException(String.format("Failed to query service config. %d. %s", err,
//									Kernel32Util.formatMessageFromLastErrorCode(err)));
//
//						}

						if (!advapi32.QueryServiceStatusEx(service, (byte) advapi32.SC_STATUS_PROCESS_INFO, null, 0,
								pcbBytesNeeded)) {

							int cbBufSize = pcbBytesNeeded.getValue();

							Memory buffer = new Memory(cbBufSize);
							buffer.clear();

							if (advapi32.QueryServiceStatusEx(service, (byte) advapi32.SC_STATUS_PROCESS_INFO, buffer,
									cbBufSize, pcbBytesNeeded)) {

								SERVICE_STATUS_PROCESS lpBuffer = new SERVICE_STATUS_PROCESS();

								lpBuffer.init(buffer);

								if (lpBuffer.dwCurrentState == Winsvc.SERVICE_RUNNING)
									return Status.STARTED;

								if (lpBuffer.dwCurrentState == Winsvc.SERVICE_PAUSED)
									return Status.PAUSED;

							}

							else {
								int err = Native.getLastError();
								throw new IOException(String.format("Failed to get buffer size. %d. %s", err,
										Kernel32Util.formatMessageFromLastErrorCode(err)));
							}
						} else {
							int err = Native.getLastError();
							throw new IOException(String.format("Failed to get service state. %d. %s", err,
									Kernel32Util.formatMessageFromLastErrorCode(err)));
						}
					} finally {
						advapi32.CloseServiceHandle(service);
					}
				}
			} finally {
				advapi32.CloseServiceHandle(serviceManager);
			}
			return Status.STOPPED;

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
