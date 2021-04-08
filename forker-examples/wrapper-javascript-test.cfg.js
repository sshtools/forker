/*
 * Copyright © 2015 - 2021 SSHTOOLS Limited (support@sshtools.com)
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
/* This is a test JavaScript wrapper configuration file.
 * You can mix code and configuration, but this script
 * must evaluate to (return) an object containing the configuration
 * properties (i.e. a map of all the same properties that might
 * be provided by other methods) 
 */

java.lang.System.out.println('Im configured by a script!');

/* Return the configuration object. The outer brackets are required. */

({
	main: 'com.nervepoint.forker.examples.WrappedTest',
	level: 'WARNING',
	arg: [
		'arg1',
		'arg2'
	]
})