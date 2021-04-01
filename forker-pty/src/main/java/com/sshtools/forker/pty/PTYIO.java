package com.sshtools.forker.pty;

import com.sshtools.forker.common.IO.DefaultIO;

public final class PTYIO extends DefaultIO {
	public PTYIO() {
		super("PTY", true, false, true, true);
	}
}