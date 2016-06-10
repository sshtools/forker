package com.nervepoint.forker.examples;

/** This is the application that {@link WrapperTest} will launch */
public class WrappedTest {

	public WrappedTest() {
	}

	public static void main(String[] args) throws Exception {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					for (int i = 0; i < 20; i++) {
						System.out.println("Waiting to exit " + i + "/" + 20);
						Thread.sleep(1000);
					}
				} catch (Exception e) {
				}
			}
		});

		for (int i = 0; i < 1000; i++) {
			System.out.println("Running " + i);
			Thread.sleep(1000);
		}

	}

}
