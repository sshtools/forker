package com.sshtools.forker.wrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.SystemUtils;

import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.PositionalParamSpec;
import picocli.CommandLine.ParseResult;

public class Configuration {

	private List<KeyValuePair> properties = new ArrayList<KeyValuePair>();
	private List<KeyValuePair> externalProperties = new ArrayList<KeyValuePair>();
	private Object cfgLock = new Object();
	private ParseResult cmd;
	private List<String> remaining = new ArrayList<>();

	public Configuration() {
	}

	public Object getCfgLock() {
		return cfgLock;
	}

	public ParseResult getCmd() {
		return cmd;
	}

	public List<KeyValuePair> getExternalProperties() {
		return externalProperties;
	}

	public List<KeyValuePair> getProperties() {
		return properties;
	}

	public boolean getSwitch(String key, boolean defaultValue) {
		String optionValue = getOptionValue(key, null);
		if (optionValue != null)
			return !"false".equals(optionValue);
		if (cmd != null && cmd.hasMatchedOption(key))
			return true;
		if (isBool(key)) {
			return true;
		}
		return false;
	}

	public boolean isBool(String key) {
		synchronized (cfgLock) {
			for (KeyValuePair nvp : properties) {
				if (nvp.getName().equals(key))
					return nvp.isBool();
			}
			return false;
		}
	}

	public String getProperty(String key) {
		synchronized (cfgLock) {
			String val = null;
			for (KeyValuePair nvp : properties) {
				if (nvp.getName().equals(key))
					val = nvp.getValue();
			}
			return val;
		}
	}

	public void setProperty(String key, Object value) {
		synchronized (properties) {
			for (KeyValuePair nvp : properties) {
				if (nvp.getName().equals(key)) {
					nvp.setValue(String.valueOf(value));
					return;
				}
			}
			KeyValuePair kp = new KeyValuePair(key, String.valueOf(value));
			externalProperties.add(kp);
			properties.add(0, kp);
		}
	}

	public void removeProperty(String key, String pattern) {
		synchronized (properties) {
			for (Iterator<KeyValuePair> it = properties.iterator(); it.hasNext();) {
				KeyValuePair kp = it.next();
				if (kp.key.equals(key) && kp.value.matches(pattern)) {
					it.remove();
					externalProperties.remove(kp);
				}
			}
		}
	}

	public void addProperty(String key, Object value) {
		synchronized (properties) {
			KeyValuePair kp = new KeyValuePair(key, String.valueOf(value));
			externalProperties.add(kp);
			properties.add(0, kp);
		}
	}

	public List<String> getOptionValues(String key) {
		synchronized (cfgLock) {
			String os = getOsPrefix();
			OptionSpec matchedOption = cmd == null ? null : cmd.matchedOption(key);
			if (matchedOption != null) {
				return matchedOption.originalStringValues();
			}
			List<String> valList = new ArrayList<String>();
			for (KeyValuePair nvp : properties) {
				if ((nvp.getName().equals(key) || nvp.getName().equals(os + "-" + key)) && nvp.getValue() != null) {
					valList.add(nvp.getValue());
				}
			}
			/*
			 * System properties, e.g. forker.somevar.1=val, forker.somevar.2=val2
			 */
			List<String> varNames = new ArrayList<String>();
			for (Map.Entry<Object, Object> en : System.getProperties().entrySet()) {
				if (((String) en.getKey()).startsWith("forker." + (key.replace("-", ".")) + ".")
						|| ((String) en.getKey())
								.startsWith("forker." + os.replace("-", ".") + "." + (key.replace("-", ".")) + ".")) {
					varNames.add((String) en.getKey());
				}
			}
			Collections.sort(varNames);
			for (String vn : varNames) {
				valList.add(System.getProperty(vn));
			}
			/*
			 * Environment variables, e.g. FORKER_SOMEVAR_1=val, FORKER_SOMEVAR_2=val2
			 */
			varNames.clear();
			for (Map.Entry<String, String> en : System.getenv().entrySet()) {
				if (en.getKey().startsWith("FORKER_" + (key.toUpperCase().replace("-", "_")) + "_") || en.getKey()
						.startsWith("FORKER_" + ((os + "-" + key).toUpperCase().replace("-", "_")) + "_")) {
					varNames.add(en.getKey());
				}
			}
			Collections.sort(varNames);
			for (String vn : varNames) {
				valList.add(System.getenv(vn));
			}
			return valList;
		}
	}

	public String getOsPrefix() {
		if (SystemUtils.IS_OS_WINDOWS)
			return "windows";
		else if (SystemUtils.IS_OS_MAC_OSX)
			return "mac-osx";
		else if (SystemUtils.IS_OS_LINUX)
			return "linux";
		else if (SystemUtils.IS_OS_UNIX)
			return "unix";
		else
			return "other";
	}

	public String getOptionValue(String key, String defaultValue) {
		String os = getOsPrefix();
		/* Look for OS specific options in preference */
		String val = System.getProperty("forkerwrapper." + key.replace("-", "."),
				System.getProperty("forkerwrapper." + (os + "." + key).replace("-", ".")));
		if (val == null) {
			val = System.getProperty("forkerwrapper." + key.replace("-", "."),
					System.getProperty("forkerwrapper." + (key.replace("-", "."))));
			if (val == null) {
				val = System.getenv("FORKER_" + (os + "-" + key).replace("-", "_").toUpperCase());
				if (val == null) {
					val = System.getenv("FORKER_" + key.replace("-", "_").toUpperCase());
					if (val == null) {
						val = getProperty(os + "-" + key);
						if (val == null) {
							val = getProperty(key);
						}
					}
				}
			}
		}

//		if (val == null) {
//			val = cmd == null ? null : cmd.(os + "-" + key);
		if (val == null)
			val = cmd == null || cmd.matchedOption(key) == null ? null : cmd.matchedOption(key).getValue().toString();
//		}

		if (val == null)
			val = defaultValue;

		return val;
	}
	
	public List<String> getRemaining() {
		return remaining;
	}

	public void init(ParseResult cmd) {
		this.cmd = cmd;
		if(cmd != null) {
			for(PositionalParamSpec p : cmd.matchedPositionals()) {
				remaining.addAll(p.stringValues());
			}
			remaining.addAll(cmd.unmatched());
		}
	}

	public void setRemaining(String... remaining) {
		this.remaining.clear();
		this.remaining.addAll(Arrays.asList(remaining));
	}
}
