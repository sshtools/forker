package com.sshtools.forker.updater;

import java.util.Collections;
import java.util.List;

public class Launcher {
	
	public enum Scope {
		USER, GLOBAL
	}
	
	private String id;
	private Scope scope;
	private String executable;
	private List<String> arguments = Collections.emptyList();
	private String description;
	
	public Launcher() {
	}
	
	public Launcher(String id) {
		this.id = id;
	}

	public Scope scope() {
		return scope;
	}

	public Launcher scope(Scope scope) {
		this.scope = scope;
		return this;
	}


	public String executable() {
		return executable;
	}

	public Launcher executable(String executable) {
		this.executable = executable;
		return this;
	}

	public String id() {
		return id;
	}

	public Launcher id(String id) {
		this.id = id;
		return this;
	}

	public List<String> getArguments() {
		return arguments;
	}

	public Launcher arguments(List<String> arguments) {
		this.arguments = arguments;
		return this;
	}

	public String description() {
		return description;
	}

	public Launcher description(String description) {
		this.description = description;
		return this;
	}

}
