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

import com.sshtools.forker.wrapper.ForkerWrapper;

/**
 * Shows you can embed {@link ForkerWrapper}.
 */
public class WrapperTest {

	public static void main(String[] args) throws Exception {
		ForkerWrapper fw = new ForkerWrapper();

		// fw.setProperty("quiet", true);
		// fw.setProperty("level", "SEVERE");
		
		fw.setClassname(WrappedTest.class.getName());
		fw.setArguments("arg1");
		
		// Start and wait for wrapper to exit
		System.out.println("Wrapped process returned: " + fw.start());
	}
}
