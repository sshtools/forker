/**
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
package com.sshtools.forker.wrapper;

public interface ForkerWrapperMXBean {
	void ready();
	
	void wrapped(String jmxUrl);

	String getClassname();

	String getModule();

	String[] getArguments();

	void restart() throws InterruptedException;

	void restart(boolean wait) throws InterruptedException;
	
	void stop() throws InterruptedException;

	void stop(boolean wait) throws InterruptedException;
	
	void setLogLevel(String lvl);
	
	void ping();
}
