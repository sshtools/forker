package com.sshtools.forker.services.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sshtools.forker.client.OSCommand;
import com.sshtools.forker.services.Service;
import com.sshtools.forker.services.ServiceService;
import com.sshtools.forker.services.ServicesContext;
import com.sshtools.forker.services.ServicesListener;

public class AutoServiceService implements ServiceService {
	private ServiceService services;

	public AutoServiceService() throws Exception {
		detect();
	}

	private void detect() throws IOException {
		List<ServiceService> l = new ArrayList<ServiceService>();
		if (OSCommand.hasCommand("systemctl")) {
			l.add(new SystemDServiceService());
		}
		if (l.isEmpty() && OSCommand.hasCommand("initctl")) {
			l.add(new InitctlServiceService());
		}
		if (l.isEmpty() && OSCommand.hasCommand("service")) {
			l.add(new SysVServiceService());
		}
		// Fallback to SysV
		if (l.isEmpty())
			throw new IOException("Could not detect any service systems.");
		services = new CompoundServicesService(l.toArray(new ServiceService[0]));
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
	public List<Service> getServices() throws IOException {
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
