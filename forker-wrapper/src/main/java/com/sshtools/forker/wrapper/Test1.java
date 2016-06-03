package com.sshtools.forker.wrapper;

import com.sshtools.forker.client.OSCommand;

public class Test1 {

	public Test1() {
	}

	public static void main(String[] args) throws Exception {
		OSCommand.run("id");
		// OSCommand.elevate();
		// try {
		// OSCommand.run("id");
		// }
		// finally {
		// OSCommand.restrict();
		// }

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					for (int i = 0; i < 20; i++) {
						System.out.println("EXIT WAITINIG " + i);
						Thread.sleep(1000);
					}
				} catch (Exception e) {
				}
			}
		});

		for (int i = 0; i < 1000; i++) {
			System.out.println("WAITINIG " + i);
			Thread.sleep(1000);
		}

	}

}
