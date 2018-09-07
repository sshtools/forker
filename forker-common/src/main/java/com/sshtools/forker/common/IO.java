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
package com.sshtools.forker.common;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Represents an <i>I/O Mode</i>. Each one allows the user to choose how the
 * process will be executed using <code>ForkerBuild.io(IO)</code> to set the
 * mode. Additional modes may be plugged in the standard Java service mechanism
 * (with each class name being listed the resource
 * META-INF/services/com.sshtools.forker.common.IO).
 *
 */
public interface IO {
	// INPUT, OUTPUT, IO, DEFAULT, SINK, PTY, DAEMON

	/**
	 * A mode indicating only the input stream is required.
	 */
	public final static IO INPUT = new DefaultIO("INPUT", true, true);
	/**
	 * A mode indicating only the output stream iis required
	 */
	public final static IO OUTPUT = new DefaultIO("OUTPUT", true, true, false, false);
	/**
	 * A mode indicating both input and output are requiired
	 */
	public final static IO IO = new DefaultIO("IO", true, true);
	/**
	 * A mode indicating the default {@link ProcessBuilder} should be used.
	 */
	public final static IO DEFAULT = new DefaultIO("DEFAULT", true, true);
	/**
	 * A mode indicating no I/O is required at all.
	 */
	public final static IO SINK = new DefaultIO("SINK", true, true, false, false);
	/**
	 * A mode indicating non-blocking I/O is required.
	 */
	public final static IO NON_BLOCKING = new DefaultIO("NON_BLOCKING", true, true, false, true);
	/**
	 * A mode indicating the daemon should be used.
	 */
	public final static IO DAEMON = new DefaultIO("DAEMON", true, true);

	/**
	 * This is here for backwards compatibility, and is a mode indicating a Pty
	 * is required.
	 */
	public final static IO PTY = new DefaultIO("PTY", false, true);

	/**
	 * Get if the mode may be used from the same runtime as the called
	 * 
	 * @return local mode
	 */
	boolean isLocal();

	/**
	 * Get if the mode may be handle remotely, i.e. by the Forker Daemon
	 * 
	 * @return remote mode
	 */
	boolean isRemote();

	/**
	 * Get if by default, this mode should immediately flush any standard input
	 * to the daemon.
	 * 
	 * @return auto flush stdin
	 */
	boolean isAutoFlushStdIn();

	/**
	 * Get if standard error redirection is allowed.
	 * 
	 * @return allow standard error redirection
	 */
	boolean isAllowStdErrRedirect();

	/**
	 * Get the name of this mode.
	 * 
	 * @return mode
	 */
	String name();

	/**
	 * Default IO implementation.
	 *
	 */
	public static class DefaultIO implements IO {
		private boolean remote;
		private boolean local;
		private String name;
		private boolean autoFlushStdIn;
		private boolean allowStdErrRedirect;

		private static Map<String, IO> ios = new HashMap<String, IO>();
		private static boolean iosLoaded;

		/**
		 * Constructor
		 * 
		 * @param name
		 *            mode name
		 * @param local
		 *            allow local
		 * @param remote
		 *            allow remote
		 */
		public DefaultIO(String name, boolean local, boolean remote) {
			this(name, local, remote, false, true);
		}

		/**
		 * Constructor
		 * 
		 * @param name
		 *            mode name
		 * @param local
		 *            allow local
		 * @param remote
		 *            allow remote
		 * @param autoFlushStdIn
		 *            auto flush stdin
		 * @param allowStdErrRedirect
		 *            allow stderr redirection
		 */
		public DefaultIO(String name, boolean local, boolean remote, boolean autoFlushStdIn,
				boolean allowStdErrRedirect) {
			this.name = name;
			this.local = local;
			this.autoFlushStdIn = autoFlushStdIn;
			this.remote = remote;
			this.allowStdErrRedirect = allowStdErrRedirect;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			DefaultIO other = (DefaultIO) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}

		/**
		 * Get a list of all available I/O modes.
		 * 
		 * @return list of all available I/O modes
		 */
		public static Collection<IO> values() {
			loadIos();
			return ios.values();
		}

		/**
		 * Get one of the available I/O modes given it's name.
		 * 
		 * @param name
		 *            I/O mode name
		 * @return I/O mode
		 */
		public static IO valueOf(String name) {
			loadIos();
			IO io = ios.get(name);
			if (io == null)
				throw new IllegalArgumentException("No I/O named " + name);
			return io;
		}

		private static void loadIos() {
			if (!iosLoaded) {
				ios.put(DEFAULT.name(), DEFAULT);
				ios.put(INPUT.name(), INPUT);
				ios.put(OUTPUT.name(), OUTPUT);
				ios.put(IO.name(), IO);
				ios.put(SINK.name(), SINK);
				ios.put(DAEMON.name(), DAEMON);
				for (IO io : ServiceLoader.load(IO.class)) {
					ios.put(io.name(), io);
				}
				iosLoaded = true;
			}
		}

		@Override
		public boolean isLocal() {
			return local;
		}

		@Override
		public boolean isRemote() {
			return remote;
		}

		public String name() {
			return name;
		}

		public String toString() {
			return name;
		}

		@Override
		public boolean isAutoFlushStdIn() {
			return autoFlushStdIn;
		}

		@Override
		public boolean isAllowStdErrRedirect() {
			return allowStdErrRedirect;
		}
	}
}