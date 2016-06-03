package com.nervepoint.forker.examples;

import java.io.IOException;

import org.apache.commons.io.IOUtils;

import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.client.ShellBuilder;
import com.sshtools.forker.client.impl.ForkerProcess;
import com.sshtools.forker.client.impl.ForkerProcess.Listener;
import com.sshtools.forker.common.IO;

public class Shell {

	public static void main(String[] args) throws Exception {
		ForkerBuilder shell = new ShellBuilder().loginShell(true).io(IO.PTY).redirectErrorStream(true);
		final Process p = shell.start();

		/*
		 * NOTE: The process will actually be an instance of ForkerProcess in
		 * the case of PTY. You can cast this to add listeners for window size
		 */
		ForkerProcess fp = (ForkerProcess) p;
		fp.addListener(new Listener() {
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
