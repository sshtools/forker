package com.sshtools.forker.services.impl;

import com.sshtools.forker.client.OSCommand;
import com.sshtools.forker.services.Service;
import com.sshtools.forker.services.ServiceService;
import com.sshtools.forker.services.ServicesContext;
import com.sshtools.forker.services.ServicesListener;
import com.sun.jna.Platform;

import java.io.IOException;
import java.util.List;

public class AutoServiceService implements ServiceService {
	private ServiceService services;

	public AutoServiceService() throws IOException {
		detect();
	}

	private void detect() throws IOException {
		if (Platform.isWindows()) {
			services = new Win32ServiceService();
		} else {
			if (OSCommand.hasCommand("systemctl")) {
				services = new SystemDServiceService();
			}
			if (services == null && OSCommand.hasCommand("initctl")) {
				services = new InitctlServiceService();
			}
			if (services == null && OSCommand.hasCommand("service")) {
				services = new SysVServiceService();
			}
			if (services == null)
				throw new IOException("Could not detect any service systems.");
		}
	}

	@Override
	public void addListener(ServicesListener listener) {
		services.addListener(listener);
	}

	@Override
	public void removeListener(ServicesListener listener) {
		services.removeListener(listener);
	}

	@Override
	public List<? extends Service> getServices() throws IOException {
		return services.getServices();
	}

	@Override
	public void restartService(Service service) throws Exception {
		services.restartService(service);
	}

	@Override
	public void startService(Service service) throws Exception {
		services.startService(service);
	}

	@Override
	public void stopService(Service service) throws Exception {
		services.stopService(service);
	}

	@Override
	public Service getService(String name) throws IOException {
		return services.getService(name);
	}

	@Override
	public void setStartOnBoot(Service service, boolean startOnBoot) throws Exception {
		services.setStartOnBoot(service, startOnBoot);
	}

	@Override
	public boolean isStartOnBoot(Service service) throws Exception {
		return services.isStartOnBoot(service);
	}

	@Override
	public void configure(ServicesContext context) {
		services.configure(context);
	}

	@Override
	public void pauseService(Service service) throws Exception {
		services.pauseService(service);
	}

	@Override
	public void unpauseService(Service service) throws Exception {
		services.unpauseService(service);
	}
}
