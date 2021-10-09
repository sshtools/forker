package com.sshtools.forker.wrapper;

import java.awt.SplashScreen;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AbstractWrapper {


	/** The logger. */
	protected Logger logger = Logger.getGlobal();

	/**
	 * Sets the log level.
	 *
	 * @param lvl the new log level
	 */
	public void setLogLevel(String lvl) {
		setLogLevel(Level.parse(lvl));
	}

	/**
	 * Gets the logger.
	 *
	 * @return the logger
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * Sets the log level.
	 *
	 * @param lvl the new log level
	 */
	public void setLogLevel(Level lvl) {
		Logger logger = this.logger;
		do {
			logger.setLevel(lvl);
			for (Handler h : logger.getHandlers()) {
				h.setLevel(lvl);
			}
			logger = logger.getParent();
		} while (logger != null);
	}

	public void closeSplash() {
		try {
			final SplashScreen splash = SplashScreen.getSplashScreen();
			if(splash != null)
				splash.close();
		}
		catch(Exception e) {
			logger.log(Level.FINE, "Ignoring splash error.", e);
		}
	}

	/**
	 * Reconfigure logging.
	 */
	protected void reconfigureLogging(String levelName) {
		Level lvl = Level.parse(levelName);
		setLogLevel(lvl);
	}
}
