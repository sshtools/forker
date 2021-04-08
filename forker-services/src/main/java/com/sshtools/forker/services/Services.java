/**
 * Copyright © 2015 - 2021 SSHTOOLS Limited (support@sshtools.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sshtools.forker.services;

import com.sshtools.forker.services.impl.AutoServiceService;

public class Services {
	private final static Object lock = new Object();
	private static ServiceService service;

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
