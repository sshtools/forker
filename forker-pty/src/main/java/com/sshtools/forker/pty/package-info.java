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
/**
 * Provides the classes required to support creation of Pseudo Terminals for
 * interactive shells and other commands. This currently is only available when
 * <i>Forker Daemon</i> is installed, and requires client code set the I/O mode
 * of {@link com.sshtools.forker.pty.PTYExecutor#PTY} when using ForkerBuilder.
 * <p>
 * For example, this in conjunction with other Forker suite facilities (such as
 * as privilege escalation and run as user) could be used to create an SSH or
 * Telnet server in Java.
 */
package com.sshtools.forker.pty;