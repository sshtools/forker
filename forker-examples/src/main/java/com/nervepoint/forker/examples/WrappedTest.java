/**
 * Copyright © 2015 - 2018 SSHTOOLS Limited (support@sshtools.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
