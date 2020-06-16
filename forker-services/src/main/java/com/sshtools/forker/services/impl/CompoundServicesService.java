package com.sshtools.forker.services.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sshtools.forker.services.Service;
import com.sshtools.forker.services.ServiceService;
import com.sshtools.forker.services.ServicesContext;
import com.sshtools.forker.services.ServicesListener;

public class CompoundServicesService implements ServiceService {
	protected List<ServiceService> serviceServices = new ArrayList<>();

	public CompoundServicesService(ServiceService... serviceServices) {
		this.serviceServices.addAll(Arrays.asList(serviceServices));
	}

	@Override
	public void addListener(ServicesListener listener) {
		for (ServiceService s : serviceServices) {
			s.addListener(listener);
		}
	}

	@Override
	public void removeListener(ServicesListener listener) {
		for (ServiceService s : serviceServices) {
			s.removeListener(listener);
		}
	}

	@Override
	public List<Service> getServices() throws IOException {
		List<Service> l = new ArrayList<>();
		Set<String> n = new HashSet<>();
		for (ServiceService s : serviceServices) {
			for (Service srv : s.getServices()) {
				if (!n.contains(srv.getNativeName())) {
					l.add(srv);
					n.add(srv.getNativeName());
				}
			}
		}
		return l;
	}

	@Override
	public void restartService(Service service) throws Exception {
		for (ServiceService s : serviceServices) {
			List<Service> l = s.getServices();
			if (l.contains(service)) {
				s.restartService(service);
				return;
			}
		}
		throw new IllegalArgumentException("Unknown service " + service);
	}

	@Override
	public void startService(Service service) throws Exception {
		for (ServiceService s : serviceServices) {
			List<Service> l = s.getServices();
			if (l.contains(service)) {
				s.startService(service);
				return;
			}
		}
		throw new IllegalArgumentException("Unknown service " + service);
	}

	@Override
	public void stopService(Service service) throws Exception {
		for (ServiceService s : serviceServices) {
			List<Service> l = s.getServices();
			if (l.contains(service)) {
				s.stopService(service);
				return;
			}
		}
		throw new IllegalArgumentException("Unknown service " + service);
	}

	@Override
	public Service getService(String name) throws IOException {
		for (ServiceService s : serviceServices) {
			Service sv = s.getService(name);
			if (sv != null) {
				return sv;
			}
		}
		return null;
	}

	@Override
	public void setStartOnBoot(Service service, boolean startOnBoot) throws Exception {
		for (ServiceService s : serviceServices) {
			List<Service> l = s.getServices();
			if (l.contains(service)) {
				s.setStartOnBoot(service, startOnBoot);
				return;
			}
		}
		throw new IllegalArgumentException("Unknown service " + service);
	}

	@Override
	public boolean isStartOnBoot(Service service) throws Exception {
		for (ServiceService s : serviceServices) {
			List<Service> l = s.getServices();
			if (l.contains(service)) {
				return s.isStartOnBoot(service);
			}
		}
		throw new IllegalArgumentException("Unknown service " + service);
	}

	@Override
	public void configure(ServicesContext context) {
		for (ServiceService srv : serviceServices)
			srv.configure(context);
	}

	@Override
	public void pauseService(Service service) throws Exception {
		for (ServiceService s : serviceServices) {
			List<Service> l = s.getServices();
			if (l.contains(service)) {
				s.pauseService(service);
				return;
			}
		}
		throw new IllegalArgumentException("Unknown service " + service);
	}

	@Override
	public void unpauseService(Service service) throws Exception {
		for (ServiceService s : serviceServices) {
			List<Service> l = s.getServices();
			if (l.contains(service)) {
				s.unpauseService(service);
				return;
			}
		}
		throw new IllegalArgumentException("Unknown service " + service);
	}
}
