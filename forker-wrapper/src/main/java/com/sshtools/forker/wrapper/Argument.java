package com.sshtools.forker.wrapper;

public class Argument {

	private ArgumentType type;
	private String value;

	public Argument(String value) {
		this(ArgumentType.OPTION, value);
	}

	public Argument(ArgumentType type, String value) {
		this.type = type;
		this.value = value;
	}

	public ArgumentType type() {
		return type;
	}

	public String toArgFileLine() {
		switch (type) {
		case QUOTED:
			return quote(value, true);
		case VALUED_OPTION:
			int idx = value.indexOf('=');
			if (idx == -1)
				return quote(value, true);
			else
				return value.substring(0, idx + 1) + quote(value.substring(idx + 1), true);
		case VALUED_EXTENDED_OPTION:
			idx = value.indexOf(':');
			if (idx == -1)
				return quote(value, true);
			else
				return value.substring(0, idx + 1) + quote(value.substring(idx + 1), true);
		default:
			return value;
		}
	}

	protected String quote(String value, boolean escapeBackslashes) {
		if (hasWhitespace(value)) {
			return "\"" + value.replace("\\", "\\\\") + "\"";
		} else
			return value;
	}

	protected boolean hasWhitespace(String v) {
		return v.contains(" ") || v.contains("\t") || v.contains("\n") || v.contains("\r");
	}

	public String toProcessBuildArgument() {
		/*
		 * ProcessBuilder type arguments (i.e. as added to the List<String> arguments)
		 * are added pretty much as is, except for VALUED_OPTION.
		 * 
		 * TODO Check this is actually needed for VALUED_OPTION and this processing
		 * wasnt added just for argfile
		 */
		if (type == ArgumentType.VALUED_OPTION) {
			int idx = value.indexOf('=');
			if (idx == -1)
				return quote(value, false);
			else
				return value.substring(0, idx + 1) + quote(value.substring(idx + 1), false);
		} else
			return value;
	}

}
