package com.sshtools.forker.services;

import com.sshtools.forker.services.impl.AutoServiceService;

public class Services {
	private final static Object lock = new Object();
	private static ServiceService service;

	public static void main(String[] args) throws Exception {
		for (Service srv : get().getServices()) {
			System.out.println(srv);
		}
	}

	public static ServiceService get() {
		synchronized (lock) {
			if (service == null) {
				try {
					String impl = System.getProperty("forker.services.impl");
					if (impl == null)
						service = new AutoServiceService();
					else {
						ClassLoader cloader = Thread.currentThread().getContextClassLoader();
						if (cloader == null)
							cloader = Services.class.getClassLoader();
						service = (ServiceService) cloader.loadClass(impl).getConstructor().newInstance();
					}
				} catch (RuntimeException re) {
					throw re;
				} catch (Exception e) {
					throw new IllegalStateException("Failed to initialize services implementation.", e);
				}

				// Now initialize
				service.configure(new DefaultContext());
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
