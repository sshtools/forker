package com.sshtools.forker.updater.maven.plugin;

import java.util.Objects;

import org.apache.maven.plugins.annotations.Parameter;

public final class StageFile {

	@Parameter(property = "source", required = true)
	String source;

	@Parameter(property = "target", defaultValue = ".", required = true)
	String target = ".";

	@Parameter(property = "skip")
	boolean skip;

	public StageFile() {
	}

	public StageFile(String source) {
		super();
		this.source = source;
	}

	public StageFile(String source, String target) {
		super();
		this.source = source;
		this.target = target;
	}

	public boolean isSkip() {
		return skip;
	}

	public void setSkip(boolean skip) {
		this.skip = skip;
	}

	@Override
	public int hashCode() {
		return Objects.hash(source, target);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StageFile other = (StageFile) obj;
		return Objects.equals(source, other.source) && Objects.equals(target, other.target);
	}

	@Override
	public String toString() {
		return "StageFile [source=" + source + ", target=" + target + ", skip=" + skip + "]";
	}

}