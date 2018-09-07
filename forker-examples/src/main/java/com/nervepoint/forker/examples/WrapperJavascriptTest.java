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

import java.io.File;

import com.sshtools.forker.wrapper.ForkerWrapper;

/**
 * Shows you can embed {@link ForkerWrapper} and use a JavaScript
 * configuration file to configure it.
 */
public class WrapperJavascriptTest {

	public static void main(String[] args) throws Exception {
		ForkerWrapper fw = new ForkerWrapper();
		fw.setArguments(args);
		fw.readConfigFile(new File("wrapper-javascript-test.cfg.js"));
		
		// Start and wait for wrapper to exit
		System.out.println("Wrapped process returned: " + fw.start());
	}
}
