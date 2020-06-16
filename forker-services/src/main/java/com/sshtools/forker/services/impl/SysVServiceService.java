package com.sshtools.forker.services.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.output.NullOutputStream;

import com.sshtools.forker.client.OSCommand;
import com.sshtools.forker.services.AbstractService;
import com.sshtools.forker.services.Service;
import com.sshtools.forker.services.Service.Status;
import com.sshtools.forker.services.ServicesContext;

public class SysVServiceService extends AbstractServiceService {

	private static final long POLL_DELAY = 10000;

	private List<Service> services = new ArrayList<>();

	@Override
	public List<Service> getServices() throws IOException {
		return services;
	}

	@Override
	public void configure(ServicesContext app) {
		app.schedule(new Runnable() {
			@Override
			public String toString() {
				return "LoadSysVServices";
			}

			@Override
			public void run() {
				load();
			}
		}, 0, POLL_DELAY, TimeUnit.MILLISECONDS);
	}

	@Override
	public void restartService(Service service) throws Exception {
		OSCommand.elevate();
		try {
			OSCommand.runCommand(Arrays.asList("service", service.getNativeName(), "restart"));
		} finally {
			OSCommand.restrict();
			load();
		}
	}

	@Override
	public void startService(Service service) throws Exception {
		OSCommand.elevate();
		try {
			OSCommand.runCommand(Arrays.asList("service", service.getNativeName(), "start"));
		} finally {
			OSCommand.restrict();
			load();
		}
	}

	@Override
	public void stopService(Service service) throws Exception {
		OSCommand.elevate();
		try {
			OSCommand.runCommand(Arrays.asList("service", service.getNativeName(), "stop"));
		} finally {
			OSCommand.restrict();
			load();
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

	private void load() {
		try {

			List<Service> oldServices = new ArrayList<>(services);
			List<Service> newServices = new ArrayList<>();
			List<Service> addServices = new ArrayList<>();
			List<Service> updateServices = new ArrayList<>();

			newServices.clear();
			for (File line : new File("/etc/init.d").listFiles()) {

				String instanceName = line.getName();
				if (line.isFile()) {
					SysVInfo sysV = new SysVInfo(line);
					if (!instanceName.equals("skeleton") && sysV.isStartable()) {
						Status status;
						if (OSCommand.runCommand(null, new NullOutputStream(), "service", instanceName,
								"status") == 0) {
							status = Status.STARTED;
						} else {
							status = Status.STOPPED;
						}

						// Have gathered enough to create Service instance
						SysVService service = new SysVService(instanceName, status);
						newServices.add(service);

						// If the service didn't previously exist, or state has
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
				}
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

	public static class SysVService extends AbstractService {

		public SysVService(String nativeName, Status status) {
			super(nativeName, status);
		}

	}

	@Override
	public void setStartOnBoot(Service service, boolean startOnBoot) throws Exception {
		if (startOnBoot) {
			//OSCommand.runCommand("update-rc.d", service.getNativeName(), "defaults");
			OSCommand.runCommand("update-rc.d", service.getNativeName(), "enable");
		} else {
			OSCommand.runCommand("update-rc.d", service.getNativeName(), "disable");
		}
	}

	@Override
	public boolean isStartOnBoot(Service service) throws Exception {
		for (char c : new char[] { '0', '1', '2', '3', '4', '5', '6', 'S' }) {
			File d = new File("/etc/rc" + c + ".d");
			for (File f : d.listFiles()) {
				if (f.getName().matches("^S\\d\\d" + service.getNativeName() + "$")) {
					return true;
				}
			}
		}
		return false;
	}

	class SysVInfo {
		List<Character> defaultStart = new ArrayList<Character>();
		List<Character> defaultStop = new ArrayList<Character>();

		SysVInfo(File file) throws IOException {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			try {
				String l;
				boolean inHeader = false;
				String shell = null;
				while ((l = reader.readLine()) != null) {
					l = l.trim();
					if (shell == null) {
						if (l.startsWith("#!")) {
							shell = l;
						} else {
							shell = "";
							break;
						}
					}
					if (!shell.equals("")) {
						if (l.startsWith("### BEGIN INIT INFO")) {
							inHeader = true;
						} else if (l.startsWith("### END INIT INFO")) {
							inHeader = false;
							// All done reading
							break;
						} else if (inHeader) {
							l = l.substring(1).trim();
							String[] args = l.split("\\s+");
							if (args.length > 0) {
								if (args[0].equals("Default-Start:")) {
									for (int i = 1; i < args.length; i++) {
										defaultStart.add(args[i].charAt(0));
									}
								} else if (args[0].equals("Default-Stop:")) {
									for (int i = 1; i < args.length; i++) {
										defaultStop.add(args[i].charAt(0));
									}
								}
							}
						}
					}
				}
			} finally {
				reader.close();
			}
		}

		public boolean isStartable() {

			char lowest = 0xff;
			char highest = 0;
			for (Character c : defaultStart) {
				lowest = (char) Math.min(lowest, c);
				highest = (char) Math.max(highest, c);
			}

			if (!defaultStart.isEmpty() && !defaultStop.isEmpty()) {
				return true;
			}

			// ssh seems to have no stop levels?
			if (!defaultStart.isEmpty()) {
				if (lowest == '2' && highest == '5') {
					return true;
				}
			}

			// TODO Auto-generated method stub
			return false;
		}
	}

}
