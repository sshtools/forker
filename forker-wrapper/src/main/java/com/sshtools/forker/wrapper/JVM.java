package com.sshtools.forker.wrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.SystemUtils;

import com.sshtools.forker.client.OSCommand;

public class JVM {

	public static class Version implements Comparable<Version> {
		private int[] elements;
		private String versionString;

		public Version(String versionString) {
			parseFromString(versionString);
		}

		public void parseFromString(String versionString) {
			this.versionString = versionString;
			String[] stringElements = versionString.split("[^a-zA-Z0-9]+");
			if (stringElements.length < 2 || stringElements.length > 4) {
				throw new IllegalArgumentException("Version number '" + versionString
						+ "' incorrect. Must be in the format <major>.<minor>.<release>[?TAG]");
			} else if (stringElements.length == 3) {
				stringElements = new String[] { stringElements[0], stringElements[1], stringElements[2], "0" };
			} else if (stringElements.length == 2) {
				stringElements = new String[] { stringElements[0], stringElements[1], "0", "0" };
			}
			elements = new int[stringElements.length];
			int idx = 0;
			int element;
			for (String string : stringElements) {
				element = Integer.parseInt(string);
				elements[idx] = element;
				idx++;
			}
		}

		public int hashCode() {
			return toString().hashCode();
		}

		public boolean equals(Object o) {
			return o != null && o instanceof Version && ((Version) o).compareTo(this) == 0;
		}

		public int[] getVersionElements() {
			return elements;
		}

		public String toString() {
			return versionString;
		}

		public int compareTo(Version version) {
			if (version == null) {
				return 1;
			}
			int[] otherElements = version.getVersionElements();
			for (int i = 0; i < 4; i++) {
				if (elements[i] != otherElements[i]) {
					return elements[i] - otherElements[i];
				}
			}
			return 0;
		}

	}

	private String path;
	private Version version;

	public JVM(String path) throws IOException {
		this.path = path;
		for (String line : OSCommand.runCommandAndCaptureOutput(path, "-version")) {
			String[] arr = line.split("\\s+");
			String ver = arr[arr.length - 1];
			if (ver.startsWith("\"") && ver.endsWith("\"")) {
				ver = ver.substring(1, ver.length() - 1);
			}
			version = new Version(ver);
			break;
		}
	}

	public String getPath() {
		return path;
	}

	public Version getVersion() {
		return version;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
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
		JVM other = (JVM) obj;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "JVM [path=" + path + ", version=" + version + "]";
	}

	public static List<JVM> jvms() {
		List<JVM> jvms = new ArrayList<>();
		if (SystemUtils.IS_OS_LINUX) {
			try {
				for (String j : OSCommand.runCommandAndCaptureOutput("update-alternatives", "--list", "java")) {
					jvms.add(new JVM(j));
				}
			} catch (Exception e) {
				// TODO Non Debian?
			}
		} else
			throw new UnsupportedOperationException();
		return jvms;
	}

	public static void main(String[] args) {
		for (JVM j : jvms()) {
			System.out.println(j);
		}
	}
}
