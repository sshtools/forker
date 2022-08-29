package com.sshtools.forker.examples;

/** This is the application that {@link NativeForkWrapperTest} will launch */
public class NativeForkWrappedTest {

	public NativeForkWrappedTest() {
	}

	public static void main(String[] args) throws Exception {
		System.out.println("Running as " + System.getProperty("user.name"));

		for (int i = 0; i < 1000; i++) {
			System.out.println("Running " + i);
			Thread.sleep(1000);
		}
	}

}
