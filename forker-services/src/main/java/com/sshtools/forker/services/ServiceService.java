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
