package com.nervepoint.forker.examples;

import com.sshtools.forker.wrapper.ForkerWrapper;

/**
 * Shows you can embed {@link ForkerWrapper}.
 */
public class WrapperTest {

	public static void main(String[] args) throws Exception {
		ForkerWrapper fw = new ForkerWrapper();

		// fw.setProperty("quiet", true);
		// fw.setProperty("level", "SEVERE");

		fw.getWrappedApplication().setClassname(WrappedTest.class.getName());
		fw.getWrappedApplication().setArguments("arg1");

		// Start and wait for wrapper to exit
		System.out.println("Wrapped process returned: " + fw.start());
	}
}
