package com.sshtools.forker.services.impl;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.NullOutputStream;

import com.sshtools.forker.client.OSCommand;
import com.sshtools.forker.services.AbstractService;
import com.sshtools.forker.services.Service;
import com.sshtools.forker.services.Service.Status;
import com.sshtools.forker.services.ServiceService;
import com.sshtools.forker.services.ServicesContext;

public class InitctlServiceService extends AbstractServiceService implements ServiceService {
	private static final long POLL_DELAY = 1000;
	private List<Service> services = new ArrayList<>();

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
				return "LoadInitCtlServices";
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
			OSCommand.runCommand(new NullOutputStream(), Arrays.asList("initctl", "restart", service.getNativeName()));
		} finally {
			OSCommand.restrict();
			load();
		}
	}

	@Override
	public void startService(Service service) throws Exception {
		OSCommand.elevate();
		try {
			OSCommand.runCommand(new NullOutputStream(), Arrays.asList("initctl", "start", service.getNativeName()));
		} finally {
			OSCommand.restrict();
			load();
		}
	}

	@Override
	public void stopService(Service service) throws Exception {
		OSCommand.elevate();
		try {
			OSCommand.runCommand(new NullOutputStream(), Arrays.asList("initctl", "stop", service.getNativeName()));
		} finally {
			OSCommand.restrict();
			load();
		}
	}

	private void load() {
		try {
			List<Service> oldServices = new ArrayList<>(services);
			List<Service> newServices = new ArrayList<>();
			List<Service> addServices = new ArrayList<>();
			List<Service> updateServices = new ArrayList<>();
			newServices.clear();
			for (String line : OSCommand.runCommandAndCaptureOutput("initctl", "list")) {
				// Skip lines that start with whitepspace (they are sub jobs?)
				if (!line.startsWith("\t")) {
					String[] serviceProcess = line.split(",{1}");
					String[] serviceDetails = serviceProcess[0].split("\\s+");
					// Job name and status
					String jobName = serviceDetails[0];
					String instanceOrStatus = serviceDetails[1];
					String instanceName = jobName;
					String jobStatus = instanceOrStatus;
					if (instanceOrStatus.startsWith("(")) {
						instanceName = instanceOrStatus.substring(1, instanceOrStatus.length() - 2);
						jobStatus = serviceDetails[2];
					}
					String[] statusArr = jobStatus.split("/");
					String goal = statusArr[0];
					String state = statusArr.length > 1 ? statusArr[1] : "unknown";
					// Process ID (if a running daemon)
					int processId = Integer.MIN_VALUE;
					if (serviceProcess.length > 1) {
						String[] process = serviceProcess[1].trim().split("\\s+");
						if (process[0].equals("process")) {
							processId = Integer.parseInt(process[1]);
						}
					}
					// Translate to the status
					Status status = null;
					if (goal.equals("start")) {
						if (state.equals("unknown") || state.equals("starting") || state.equals("pre-start")
								|| state.equals("post-start") || state.equals("spawned")) {
							status = Status.STARTING;
						} else if (state.equals("running")) {
							status = Status.STARTED;
						} else {
							throw new IOException("Unknown status " + status + " in goal " + goal);
						}
					} else if (goal.equals("stop")) {
						if (state.equals("unknown") || state.equals("pre-stop") || state.equals("stopping")
								|| state.equals("post-stop")) {
							status = Status.STOPPING;
						} else if (state.equals("waiting") || state.equals("killed")) {
							status = Status.STOPPED;
						} else {
							throw new IOException("Unknown status " + status + " in goal " + goal);
						}
					} else {
						throw new IOException("Unknown goal " + goal + " (" + line + ")");
					}
					// Have gathered enough to create Service instance
					InitctlService service = new InitctlService(instanceName, status);
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

	public static class InitctlService extends AbstractService {
		public InitctlService(String nativeName, Status status) {
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
		// First determine what the default is
		boolean enabledByDefault = isEnabledByDefault(service);
		if (!startOnBoot && enabledByDefault) {
			PrintWriter pw = new PrintWriter(getServiceOverrideFile(service));
			try {
				pw.println("manual");
			} finally {
				pw.close();
			}
		} else if (startOnBoot && !enabledByDefault) {
			PrintWriter pw = new PrintWriter(getServiceOverrideFile(service));
			try {
				pw.println("start on runlevel [2345]");
			} finally {
				pw.close();
			}
		} else {
			getServiceOverrideFile(service).delete();
		}
	}

	protected boolean isEnabledByDefault(Service service) throws IOException {
		boolean enabledByDefault = false;
		File confFile = getServiceConfFile(service);
		if (confFile.exists()) {
			for (String line : FileUtils.readLines(confFile, "UTF-8")) {
				if (line.startsWith("start on runlevel")) {
					enabledByDefault = true;
					break;
				}
			}
		}
		return enabledByDefault;
	}

	@Override
	public boolean isStartOnBoot(Service service) throws Exception {
		File override = getServiceOverrideFile(service);
		boolean defaultState = isEnabledByDefault(service);
		if (!override.exists())
			return defaultState;
		for (String line : FileUtils.readLines(override, "UTF-8")) {
			if (line.startsWith("start on runlevel"))
				return true;
		}
		return false;
	}

	protected File getServiceConfFile(Service service) {
		return new File("/etc/init/" + service.getNativeName() + ".conf");
	}

	protected File getServiceOverrideFile(Service service) {
		return new File("/etc/init/" + service.getNativeName() + ".override");
	}
}
