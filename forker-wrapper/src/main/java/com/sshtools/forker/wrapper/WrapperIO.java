package com.sshtools.forker.wrapper;

import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.common.IO;
import com.sshtools.forker.common.IO.DefaultIO;

/**
 * I/O mode that allows executed or Java classes processes to be wrapped in
 * {@link ForkerWrapper}. *
 */
public class WrapperIO extends DefaultIO {

	/**
	 * I/O mode to use to obtain a {@link Process} that is wrapped in
	 * {@link ForkerWrapper} using the standard {@link ForkerBuilder}.
	 */
	public static final IO WRAPPER = DefaultIO.valueOf("WRAPPER");

	public WrapperIO() {
		super("WRAPPER", true, false);
	}

}
