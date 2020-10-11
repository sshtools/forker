package com.nervepoint.forker.examples;

import java.io.File;

import com.sshtools.forker.wrapper.ForkerWrapper;

/**
 * Shows you can embed {@link ForkerWrapper} and use a JavaScript
 * configuration file to configure it.
 */
public class WrapperJavascriptTest {

	public static void main(String[] args) throws Exception {
		ForkerWrapper fw = new ForkerWrapper();
		fw.getWrappedApplication().setArguments(args);
		fw.readConfigFile(new File("wrapper-javascript-test.cfg.js"));
		
		// Start and wait for wrapper to exit
		System.out.println("Wrapped process returned: " + fw.start());
	}
}
