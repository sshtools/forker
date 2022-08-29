package com.sshtools.forker.examples;

import com.sshtools.forker.wrapper.ForkerWrapper;

/**
 * Shows you can embed {@link ForkerWrapper}.
 */
public class NativeForkWrapperTest {

	public static void main(String[] args) throws Exception {
//		OSCommand.restartAsAdministrator(NativeForkWrapperTest.class.getPackageName(),  NativeForkWrapperTest.class, args);

		System.out.println("Running as " + System.getProperty("user.name"));		
		ForkerWrapper fw = new ForkerWrapper();

		// fw.setProperty("quiet", true);
		// fw.setProperty("level", "SEVERE");

		fw.getWrappedApplication().setModule(NativeForkWrappedTest.class.getPackageName());
		fw.getWrappedApplication().setClassname(NativeForkWrappedTest.class.getName());
		fw.getConfiguration().setRemaining("arg1");
		fw.getConfiguration().setProperty("level", "FINE");
		fw.getConfiguration().setProperty("native-fork", "true");
		fw.getConfiguration().setProperty("daemon", "true");

		// Start and wait for wrapper to exit
		System.out.println("Wrapped process returned: " + fw.start());
	}
}
