package com.sshtools.forker.updater;

import java.util.ArrayList;
import java.util.List;

public class Launcher {

	private List<Entry> entries = new ArrayList<>();
	private String id;
	
	public Launcher(String id) {
		this.id = id;
	}
	
	public String id() {
		return id;
	}

	public List<Entry> entries() {
		return entries;
	}
}
