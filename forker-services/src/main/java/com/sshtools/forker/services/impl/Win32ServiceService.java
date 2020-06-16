package com.sshtools.forker.services.impl;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.commons.io.output.NullOutputStream;

import com.sshtools.forker.client.OSCommand;
import com.sshtools.forker.services.AbstractService;
import com.sshtools.forker.services.Service;
import com.sshtools.forker.services.Service.Status;
import com.sshtools.forker.services.ServiceService;
import com.sshtools.forker.services.ServicesContext;

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
			OSCommand.runCommand(new NullOutputStream(), Arrays.asList("cmd.exe", "/c", "sc", "restart", service.getNativeName()));
		} finally {
			OSCommand.restrict();
			load();
		}
	}

	@Override
	public void pauseService(Service service) throws Exception {
		OSCommand.elevate();
		try {
			OSCommand.runCommand(new NullOutputStream(), Arrays.asList("cmd.exe", "/c", "sc", "pause", service.getNativeName()));
		} finally {
			OSCommand.restrict();
			load();
		}
	}

	@Override
	public void unpauseService(Service service) throws Exception {
		OSCommand.elevate();
		try {
			OSCommand.runCommand(new NullOutputStream(), Arrays.asList("cmd.exe", "/c", "sc", "continue", service.getNativeName()));
		} finally {
			OSCommand.restrict();
			load();
		}
	}

	@Override
	public void startService(Service service) throws Exception {
		OSCommand.elevate();
		try {
			OSCommand.runCommand(new NullOutputStream(), Arrays.asList("cmd.exe", "/c", "sc", "start", service.getNativeName()));
		} finally {
			OSCommand.restrict();
			load();
		}
	}

	@Override
	public void stopService(Service service) throws Exception {
		OSCommand.elevate();
		try {
			OSCommand.runCommand(new NullOutputStream(), Arrays.asList("cmd.exe", "/c", "sc", "stop", service.getNativeName()));
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
			Win32Service service = null;
			for (String line : OSCommand.runCommandAndCaptureOutput("cmd.exe", "/c", "sc", "query")) {
				// Skip lines that start with whitepspace (they are sub jobs?)
				String[] args = line.split(":");
				if (args.length > 0) {
					if (args[0].equals("SERVICE_NAME")) {
						if (service != null) {
							addService(oldServices, newServices, addServices, updateServices, service);
						}
						service = new Win32Service(args[1], Status.STOPPED);
					}
					if (service == null) {
						LOG.warning(String.format("Unexpected output '%s' in service command.", line));
					} else {
						if (args[0].equals("DISPLAY_NAME")) {
							// TODO not used yet
						} else if (args[0].equals("STATE")) {
							int state = Integer.parseInt(args[1].split("\\s+")[0]);
							switch (state) {
							case 1:
								service.setStatus(Status.STOPPED);
								break;
							case 2:
								service.setStatus(Status.STARTING);
								break;
							case 3:
								service.setStatus(Status.STOPPING);
								break;
							case 4:
								service.setStatus(Status.STARTED);
								break;
							case 5:
								service.setStatus(Status.UNPAUSING);
								break;
							case 6:
								service.setStatus(Status.PAUSING);
								break;
							case 7:
								service.setStatus(Status.PAUSED);
								break;
							default:
								service.setStatus(Status.STOPPED);
								LOG.warning(String.format("Unknown service state '%d' for service %s.", state,
										service.getNativeName()));
							}
						}
					}
				}
			}
			if (service != null) {
				addService(oldServices, newServices, addServices, updateServices, service);
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
		public Win32Service(String nativeName, Status status) {
			super(nativeName, status);
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
			for (String line : OSCommand.runCommandAndCaptureOutput("cmd.exe", "/c", "sc", "qc", service.getNativeName())) {
				if (line.startsWith("START_TYPE")) {
					String[] a = line.split(":");
					int state = Integer.parseInt(a[1].split("\\s+")[0]);
					return state == 2;
				}
			}
		}
		return false;
	}
}
