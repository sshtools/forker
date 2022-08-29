package com.sshtools.forker.pipes;

import com.sshtools.forker.common.CSystem;
import com.sun.jna.LastErrorException;

/**
 * Unix utilities.
 */
public class Unix {
	static String formatError(LastErrorException lee) {
		try {
			return CSystem.INSTANCE.strerror(lee.getErrorCode());
		} catch (Throwable t) {
			return lee.getMessage();
		}
	}
}
