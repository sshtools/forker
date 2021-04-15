package com.sshtools.forker.client;

import java.io.IOException;
import java.util.List;

import com.sshtools.forker.common.IO;
import com.sun.jna.Platform;

/**
 * Specialised version of {@link ShellBuilder} that builds a process appropriate
 * for running PowerShell commands.
 */
public class PowerShellBuilder extends ShellBuilder {
	{
		if (Platform.isWindows())
			shell("powershell.exe");
		else
			shell("pwsh");

		io(IO.IO);
		redirectErrorStream(true);
	}

	/**
	 * Constructor.
	 * 
	 * @param configuration configuration
	 * @param command command
	 */
	public PowerShellBuilder(ForkerConfiguration configuration, List<String> command) {
		super(configuration, command);
	}

	/**
	 * Constructor.
	 * 
	 * @param configuration configuration
	 * @param command command
	 */
	public PowerShellBuilder(ForkerConfiguration configuration, String... command) {
		super(configuration, command);
	}

	/**
	 * Constructor.
	 * 
	 * @param command command
	 */
	public PowerShellBuilder(List<String> command) {
		super(command);
	}

	/**
	 * Constructor.
	 * 
	 * @param command command
	 */
	public PowerShellBuilder(String... command) {
		super(command);
	}

	@Override
	public <P extends ForkerProcess> P start(ForkerProcessListener listener) throws IOException {
		List<String> a = command();
		if(a.size() > 0) {
			a.add(0, "-Command");
		}
		return super.start(listener);
	}
}
