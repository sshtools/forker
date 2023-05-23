package com.sshtools.forker.services;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

public interface ServiceService extends Closeable {
	public final static String BUNDLE = "services";
	
	void configure(ServicesContext context);

	void addListener(ServicesListener listener);

	void removeListener(ServicesListener listener);

	List<? extends Service> getServices() throws IOException;

	void restartService(Service service) throws Exception;

	void startService(Service service) throws Exception;

	void pauseService(Service service) throws Exception;

	void unpauseService(Service service) throws Exception;

	void stopService(Service service) throws Exception;

	void setStartOnBoot(Service service, boolean startOnBoot) throws Exception;

	boolean isStartOnBoot(Service service) throws Exception;

	Service getService(String name) throws IOException;

	default boolean hasService(String name) throws IOException {
		return getService(name) != null;
	}

	default void close() throws IOException {
	}
}
