package com.sshtools.forker.examples;

import java.io.IOException;

import com.sshtools.forker.client.ShellBuilder;
import com.sshtools.forker.common.OS;
import com.sshtools.forker.common.Util;
import com.sshtools.forker.pty.PTYProcess;
import com.sshtools.forker.pty.PTYProcess.PTYProcessListener;

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
		
		
		/* ShellBuilder is a specialisation of ForkerBuilder */
		ShellBuilder shell = new ShellBuilder();
		shell.io(PTYProcess.PTY);
		
		/* Demonstrate we are actually in a different shell by setting PS1 */
		shell.environment().put("MYENV", "An environment variable");
		
		/* Start the shell, giving it a window size listener */
		final Process p = shell.start(new PTYProcessListener() {
			@Override
			public void windowSizeChanged(int ptyWidth, int ptyHeight) {
				System.out.println("Window size changed to " + ptyWidth + " x " + ptyHeight);
			}
		});

		
		new Thread() {
			public void run() {
				try {
					Util.copy(System.in, p.getOutputStream());
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
		Util.copy(p.getInputStream(), System.out);
		int ret = p.waitFor();
		System.err.println("Exited with code: " + ret);
		System.exit(ret);
	}
}
