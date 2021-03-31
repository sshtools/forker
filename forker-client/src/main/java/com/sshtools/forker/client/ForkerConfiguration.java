package com.sshtools.forker.client;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import com.sshtools.forker.client.impl.DefaultProcessFactory;
import com.sshtools.forker.client.impl.LocalProcessFactory;
import com.sshtools.forker.client.impl.POpenProcessFactory;
import com.sshtools.forker.client.impl.SystemProcessFactory;

/**
 * Holds the list of {@link ForkerProcessFactory} instances that a
 * {@link ForkerBuilder} may use.
 */
public abstract class ForkerConfiguration {
	private static ForkerConfiguration instance;
	private static Object lock = new Object();
	private List<ForkerProcessFactory> processFactories = new ArrayList<ForkerProcessFactory>();

	protected ForkerConfiguration() {
		synchronized (lock) {
			if (instance == null)
				instance = this;
		}
		for (ForkerProcessFactory io : ServiceLoader.load(ForkerProcessFactory.class)) {
			processFactories.add(io);
		}
		processFactories.add(new NonBlockingProcessFactory());
		processFactories.add(new POpenProcessFactory());
		processFactories.add(new SystemProcessFactory());
		processFactories.add(new DefaultProcessFactory());
		processFactories.add(new LocalProcessFactory());
	}

	/**
	 * Get the list of {@link ForkerProcessFactory}s that will be invoked trying
	 * the handle the process. This is exposed to allow special ordering of the
	 * factories if required.
	 * 
	 * @return factories
	 */
	public List<ForkerProcessFactory> getProcessFactories() {
		return processFactories;
	}

	/**
	 * Get the {@link ForkerProcessFactory} instance that might be used to
	 * launch this process given it's class. This allows specific process types
	 * to be configured.
	 * 
	 * @param clazz class of factory
	 * @param <T> type of factory
	 * @return factory instance
	 */
	@SuppressWarnings("unchecked")
	public <T extends ForkerProcessFactory> T processFactory(Class<T> clazz) {
		for (ForkerProcessFactory pf : processFactories) {
			if (pf.getClass().equals(clazz)) {
				return (T) pf;
			}
		}
		return null;
	}

	/**
	 * Get the default configuration. Lazily created, if no instance has yet
	 * been created, a default one will be created and registered as the default
	 * and so will be returned here. If you want to provide your own
	 * implementation, extend {@link ForkerConfiguration} and just instantiate
	 * it before using {@link ForkerBuilder} without passing in a
	 * {@link ForkerConfiguration}.
	 * 
	 * @return configuration default
	 */
	public static ForkerConfiguration getDefault() {
		synchronized (lock) {
			if (instance == null)
				new ForkerConfiguration() {
				};
			return instance;
		}
	}
}
