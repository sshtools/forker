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
package com.sshtools.forker.wrapper;

import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.common.IO;
import com.sshtools.forker.common.IO.DefaultIO;

/**
 * I/O mode that allows executed or Java classes processes to be wrapped in
 * {@link ForkerWrapper}. *
 */
public class WrapperIO extends DefaultIO {

	/**
	 * I/O mode to use to obtain a {@link Process} that is wrapped in
	 * {@link ForkerWrapper} using the standard {@link ForkerBuilder}.
	 */
	public static final IO WRAPPER = DefaultIO.valueOf("WRAPPER");

	public WrapperIO() {
		super("WRAPPER", true, false);
	}

}
