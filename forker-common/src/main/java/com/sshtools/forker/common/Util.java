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
package com.sshtools.forker.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import com.sun.jna.Platform;

/**
 * Other utilities.
 */
public class Util {

	private static final int DEFAULT_BUFFER_SIZE = 4096;

	/**
	 * Recursively deletes `item`, which may be a directory. Symbolic links will be
	 * deleted instead of their referents. Returns a boolean indicating whether
	 * `item` still exists. http://stackoverflow.com/questions/8666420
	 * 
	 * @param item file to delete
	 * @return deleted OK
	 */
	public static boolean deleteRecursiveIfExists(File item) {
//		if (!item.exists()) {
//			System.out.println("   no existy");
//			return true;
//		}
		if (!Files.isSymbolicLink(item.toPath()) && item.isDirectory()) {
			File[] subitems = item.listFiles();
			for (File subitem : subitems) {
				if (!deleteRecursiveIfExists(subitem))
					return false;
			}
		}
		return item.delete();
	}

	/**
	 * Parse a space separated string into a list, treating portions quotes with
	 * single quotes as a single element. Single quotes themselves and spaces can be
	 * escaped with a backslash.
	 * 
	 * @param command command to parse
	 * @return parsed command
	 */
	public static List<String> parseQuotedString(String command) {
		List<String> args = new ArrayList<String>();
		boolean escaped = false;
		boolean quoted = false;
		StringBuilder word = new StringBuilder();
		for (int i = 0; i < command.length(); i++) {
			char c = command.charAt(i);
			if (c == '"' && !escaped) {
				if (quoted) {
					quoted = false;
				} else {
					quoted = true;
				}
			} else if (c == '\\' && !escaped) {
				escaped = true;
			} else if (c == ' ' && !escaped && !quoted) {
				if (word.length() > 0) {
					args.add(word.toString());
					word.setLength(0);
					;
				}
			} else {
				word.append(c);
			}
		}
		if (word.length() > 0)
			args.add(word.toString());
		return args;
	}

	/**
	 * Escape single quotes by turning them into double single quotes.
	 * 
	 * @param src source string
	 * @return escaped string
	 */
	public static String escapeSingleQuotes(String src) {
		return src.replace("'", "''");
	}

	/**
	 * Escape double quotes by turning them into double double quotes.
	 * 
	 * @param src source string
	 * @return escaped string
	 */
	public static String escapeDoubleQuotes(String src) {
		return src.replace("\"", "\"\"");
	}

	/**
	 * Get a username given it's ID.
	 * 
	 * @param id ID
	 * @return username
	 * @throws IOException on any error
	 */
	public static String getUsernameForID(String id) throws IOException {
		if (Platform.isLinux()) {
			// TODO what about NIS etc?
			for (String line : readLines(new File("/etc/passwd"))) {
				String[] arr = line.split(":");
				if (arr.length > 2 && arr[2].equals(id)) {
					return arr[0];
				}
			}
		} else {
			throw new UnsupportedOperationException("Cannot get username for ID " + id + ", I do not know how to do that on this platform.");
		}
		return null;
	}

	/**
	 * Quote and escape a list of command elements into a single string.
	 * 
	 * @param cmd command elements
	 * @return quoted and escaped string
	 */
	public static StringBuilder getQuotedCommandString(List<String> cmd) {
		// Take existing command and turn it into one escaped command
		StringBuilder bui = new StringBuilder();
		for (int i = 0; i < cmd.size(); i++) {
			if (bui.length() > 0) {
				bui.append(' ');
			}
			if (i > 0)
				bui.append("'");
			bui.append(Util.escapeSingleQuotes(cmd.get(i)));
			if (i > 0)
				bui.append("'");
		}
		return bui;
	}

	/**
	 * Get the ID for a user givens it's username.
	 * 
	 * @param username username
	 * @return user ID
	 * @throws IOException on any error
	 */
	public static String getIDForUsername(String username) throws IOException {
		if (Platform.isLinux()) {
			// TODO what about NIS etc?
			for (String line : readLines(new File("/etc/passwd"))) {
				String[] arr = line.split(":");
				if (arr.length > 2 && arr[0].equals(username)) {
					return arr[2];
				}
			}
		} else {
			throw new UnsupportedOperationException();
		}
		return null;
	}

	/**
	 * Read lines from a file (default encoding)
	 * 
	 * @param file file to read
	 * @return lines
	 * @throws IOException on error
	 */
	public static List<String> readLines(File file) throws IOException {
		try (InputStream in = new FileInputStream(file)) {
			return readLines(in);
		}
	}

	/**
	 * Read lines from a file (default encoding)
	 * 
	 * @param file     file to read
	 * @param encoding encoding
	 * @return lines
	 * @throws IOException on error
	 */
	public static List<String> readLines(File file, String encoding) throws IOException {
		try (InputStream in = new FileInputStream(file)) {
			return readLines(in, encoding);
		}
	}

	/**
	 * Read lines from a stream (default encoding)
	 * 
	 * @param in stream to read
	 * @return lines
	 * @throws IOException on error
	 */
	public static List<String> readLines(InputStream in) throws IOException {
		return readLines(in, Charset.defaultCharset().toString());
	}

	/**
	 * Read lines from a stream (default encoding)
	 * 
	 * @param in      stream to read
	 * @param charSet character set
	 * @return lines
	 * @throws IOException on error
	 */
	public static List<String> readLines(InputStream in, String charSet) throws IOException {
		List<String> l = new ArrayList<>();
		BufferedReader r = new BufferedReader(new InputStreamReader(in, charSet));
		String a;
		while ((a = r.readLine()) != null)
			l.add(a);
		return l;
	}

	/**
	 * Copy one file to another.
	 * 
	 * @param input  in
	 * @param output out
	 * @return count
	 * @throws IOException on error
	 */
	public static int copy(final File input, final File output) throws IOException {
		try (InputStream in = new FileInputStream(input)) {
			try (OutputStream out = new FileOutputStream(output)) {
				return copy(in, out);
			}
		}
	}

	/**
	 * Copy one stream to another.
	 * 
	 * @param input  in
	 * @param output out
	 * @return count
	 * @throws IOException on error
	 */
	public static int copy(final InputStream input, final OutputStream output) throws IOException {
		byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
		long count = 0;
		int n;
		while (-1 != (n = input.read(buffer))) {
			if (output != null)
				output.write(buffer, 0, n);
			count += n;
		}
		if (count > Integer.MAX_VALUE) {
			return -1;
		}
		return (int) count;
	}

	/**
	 * An {@link OutputStream} that routes to two other {@link OutputStream}
	 * instances.
	 */
	public static class TeeOutputStream extends OutputStream {

		private OutputStream left;
		private OutputStream right;

		/**
		 * Construct.
		 * 
		 * @param left  left
		 * @param right left
		 */
		public TeeOutputStream(OutputStream left, OutputStream right) {
			this.left = left;
			this.right = right;
		}

		@Override
		public void close() throws IOException {
			try {
				left.close();
			} finally {
				right.close();
			}
		}

		@Override
		public void flush() throws IOException {
			left.flush();
			right.flush();
		}

		@Override
		public void write(byte[] buf, int off, int len) throws IOException {
			left.write(buf, off, len);
			right.write(buf, off, len);
		}

		@Override
		public void write(byte[] buf) throws IOException {
			left.write(buf);
			right.write(buf);
		}

		@Override
		public void write(int b) throws IOException {
			left.write(b);
			right.write(b);
		}

	}

	/**
	 * {@link OutputStream} that doesn't go anywhere.
	 */
	public static class NullOutputStream extends OutputStream {
		@Override
		public void write(int arg0) throws IOException {
		}

	}

	/**
	 * Delete a directory recursively.
	 * 
	 * @param path path
	 */
	public static void deleteDirectory(Path path) {
		try {
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					if (exc == null) {
						Files.delete(dir);
						return FileVisitResult.CONTINUE;
					} else {
						throw exc;
					}
				}
			});
		} catch (IOException ioe) {
			throw new IllegalStateException("Failed to delete directory.", ioe);
		}

	}

	/**
	 * Get if a string is null or empty.
	 * 
	 * @param str string
	 * @return blank
	 */
	public static boolean isBlank(String str) {
		return str == null || str.length() == 0;
	}

	/**
	 * Get if a string is neither null nor empty.
	 * 
	 * @param str string
	 * @return not blank
	 */
	public static boolean isNotBlank(String str) {
		return !isBlank(str);
	}
}
