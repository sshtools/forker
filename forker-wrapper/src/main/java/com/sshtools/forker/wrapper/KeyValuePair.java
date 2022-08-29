package com.sshtools.forker.wrapper;

public class KeyValuePair {
	String key;
	String value;
	private boolean bool;

	public KeyValuePair(String line) {
		key = line;
		int idx = line.indexOf('=');
		int spcidx = line.indexOf(' ');
		if (spcidx != -1 && (spcidx < idx || idx == -1)) {
			idx = spcidx;
		}
		if (idx != -1) {
			value = line.substring(idx + 1).trim();
			key = line.substring(0, idx);
		} else {
			bool = true;
		}
	}

	public KeyValuePair(String key, String value) {
		this.key = key;
		this.value = value;
	}

	public String getName() {
		return key;
	}

	public String getValue() {
		return value;
	}

	public boolean isBool() {
		return bool;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "KeyValuePair [key=" + key + ", value=" + value + ", bool=" + bool + "]";
	}
}