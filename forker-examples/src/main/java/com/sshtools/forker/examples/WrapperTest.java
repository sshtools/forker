package com.sshtools.forker.examples;

import com.sshtools.forker.wrapper.ForkerWrapper;

/**
 * Shows you can embed {@link ForkerWrapper}.
 */
public class WrapperTest {

	public static void main(String[] args) throws Exception {
		ForkerWrapper fw = new ForkerWrapper();

		fw.getWrappedApplication().setModule(WrappedTest.class.getPackageName());
		fw.getWrappedApplication().setClassname(WrappedTest.class.getName());
		fw.getConfiguration().setRemaining("arg1");
		fw.getConfiguration().setProperty("level", "FINE");

		// Start and wait for wrapper to exit
		System.out.println("Wrapped process returned: " + fw.start());
	}
}
