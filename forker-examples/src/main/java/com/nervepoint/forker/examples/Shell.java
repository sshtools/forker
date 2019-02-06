package com.nervepoint.forker.examples;

import java.io.IOException;

import org.apache.commons.io.IOUtils;

import com.sshtools.forker.client.Forker;
import com.sshtools.forker.client.ShellBuilder;
import com.sshtools.forker.client.impl.ForkerDaemonProcess.Listener;
import com.sshtools.forker.common.OS;

/**
 * This example shows how to create an interactive shell. 
 *
 */
public class Shell {

	public static void main(String[] args) throws Exception {
		/*
		 * This example reads from stdin (i.e. the console), so stdin needs to be unbuffered with
		 * no local echoing at this end of the pipe, the following function
		 * attempts to do this. 
		 */
		OS.unbufferedStdin();
		
		/* PTY requires the daemon, so load it now (or connect to an existing one if you
		 * have started it yourself). */
		Forker.loadDaemon();
//		Forker.connectDaemon(new Instance("NOAUTH:57872"));
		
		/* ShellBuilder is a specialisation of ForkerBuilder */
		ShellBuilder shell = new ShellBuilder();
		shell.shell("pwsh");
		//shell.loginShell(true);
//		shell.io(PTYExecutor.PTY);
		shell.redirectErrorStream(true);
		
		/* Demonstrate we are actually in a different shell by setting PS1 */
		shell.environment().put("MYENV", "An environment variable");
		
		/* Start the shell, giving it a window size listener */
		final Process p = shell.start(new Listener() {
			@Override
			public void windowSizeChanged(int ptyWidth, int ptyHeight) {
				System.out.println("Window size changed to " + ptyWidth + " x " + ptyHeight);
			}
		});

		
		new Thread() {
			public void run() {
				try {
					IOUtils.copy(System.in, p.getOutputStream());
				} catch (IOException e) {
				} finally {
					// Close the process input stream when stdin closes, this
					// will end the process
					try {
						p.getOutputStream().close();
					} catch (IOException e) {
					}
				}
			}
		}.start();
		IOUtils.copy(p.getInputStream(), System.out);
		int ret = p.waitFor();
		System.err.println("Exited with code: " + ret);
		System.exit(ret);
	}
}
