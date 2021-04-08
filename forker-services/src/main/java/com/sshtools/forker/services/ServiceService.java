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

import java.io.IOException;
import java.util.List;

public interface ServiceService {
	public final static String BUNDLE = "services";
	
	void configure(ServicesContext context);

	void addListener(ServicesListener listener);

	void removeListener(ServicesListener listener);

	List<Service> getServices() throws IOException;

	void restartService(Service service) throws Exception;

	void startService(Service service) throws Exception;

	void pauseService(Service service) throws Exception;

	void unpauseService(Service service) throws Exception;

	void stopService(Service service) throws Exception;

	void setStartOnBoot(Service service, boolean startOnBoot) throws Exception;

	boolean isStartOnBoot(Service service) throws Exception;

	Service getService(String name) throws IOException;
}
