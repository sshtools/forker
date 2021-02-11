package com.sshtools.forker.services;

import java.io.IOException;
import java.util.List;

import com.sshtools.forker.services.impl.AutoServiceService;

public class Services {
	private final static Object lock = new Object();
	private static ServiceService service;

	public static void main(String[] args) throws Exception {
		ServiceService serviceService = get();
		for (Service srv : serviceService.getServices()) {
			System.out.println(srv);
		}
		serviceService.close();
	}

	public static ServiceService get() {
		synchronized (lock) {
			if (service == null) {
				ServiceService newServiceService = null;
				try {
					String impl = System.getProperty("forker.services.impl");
					if (impl == null)
						newServiceService = new AutoServiceService();
					else {
						ClassLoader cloader = Thread.currentThread().getContextClassLoader();
						if (cloader == null)
							cloader = Services.class.getClassLoader();
						newServiceService = (ServiceService) cloader.loadClass(impl).getConstructor().newInstance();
					}
				} catch (RuntimeException re) {
					throw re;
				} catch (Exception e) {
					throw new IllegalStateException("Failed to initialize services implementation.", e);
				}

				// Now initialize
				DefaultContext ctx = new DefaultContext();
				newServiceService.configure(ctx);
				ServiceService underlyingServiceService = newServiceService;
				
				service = new ServiceService() {
					
					@Override
					public void close() throws IOException {
						try {
							ServiceService.super.close();
						}
						finally {
							ctx.close();
						}
					}

					@Override
					public void unpauseService(Service service) throws Exception {
						underlyingServiceService.unpauseService(service);
					}
					
					@Override
					public void stopService(Service service) throws Exception {
						underlyingServiceService.stopService(service);
					}
					
					@Override
					public void startService(Service service) throws Exception {
						underlyingServiceService.startService(service);
					}
					
					@Override
					public void setStartOnBoot(Service service, boolean startOnBoot) throws Exception {
						underlyingServiceService.setStartOnBoot(service, startOnBoot);
					}
					
					@Override
					public void restartService(Service service) throws Exception {
						underlyingServiceService.restartService(service);
					}
					
					@Override
					public void removeListener(ServicesListener listener) {
						underlyingServiceService.removeListener(listener);
					}
					
					@Override
					public void pauseService(Service service) throws Exception {
						underlyingServiceService.pauseService(service);
					}
					
					@Override
					public boolean isStartOnBoot(Service service) throws Exception {
						return underlyingServiceService.isStartOnBoot(service);
					}
					
					@Override
					public List<Service> getServices() throws IOException {
						return underlyingServiceService.getServices();
					}
					
					@Override
					public Service getService(String name) throws IOException {
						return underlyingServiceService.getService(name);
					}
					
					@Override
					public void configure(ServicesContext context) {
						underlyingServiceService.configure(context);						
					}
					
					@Override
					public void addListener(ServicesListener listener) {
						underlyingServiceService.addListener(listener);
					}
				};				
			}
			return service;
		}
	}

	public static void set(ServiceService service) {
		synchronized (lock) {
			if (service == null) {
				Services.service = service;
			} else
				throw new IllegalStateException("Must call set() before the first call to get().");
		}
	}
}
