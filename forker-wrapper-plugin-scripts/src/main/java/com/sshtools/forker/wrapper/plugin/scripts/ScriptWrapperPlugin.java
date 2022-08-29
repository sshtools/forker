package com.sshtools.forker.wrapper.plugin.scripts;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.sshtools.forker.wrapper.ForkerWrapper;
import com.sshtools.forker.wrapper.KeyValuePair;
import com.sshtools.forker.wrapper.WrapperPlugin;

public class ScriptWrapperPlugin implements WrapperPlugin {
	private ScriptEngine engine;
	private ForkerWrapper wrapper;

	@Override
	public void init(ForkerWrapper wrapper) {
		this.wrapper = wrapper;
	}

	@Override
	public void readConfigFile(File file, List<KeyValuePair> properties) throws IOException {
		//
		// TODO restart app and/or adjust other configuration on reload
		// TODO it shouldnt reload one at a time, it should wait a short while for
		// all changes, then reload all configuration files in the same order
		// 'properties' should
		// be cleared before all are reloaded.

		if (file.getName().endsWith(".js")) {
			if (engine == null) {
				ScriptEngineManager engineManager = new ScriptEngineManager();
				engine = engineManager.getEngineByName("nashorn");
				Bindings bindings = engine.createBindings();
				bindings.put("wrapper", wrapper);
				bindings.put("log", wrapper.getLogger());
				if (engine == null)
					throw new IOException("Cannot find JavaScript engine. Are you on at least Java 8?");
			}
			FileReader r = new FileReader(file);
			try {
				@SuppressWarnings("unchecked")
				Map<String, Object> o = (Map<String, Object>) engine.eval(r);
				for (Map.Entry<String, Object> en : o.entrySet()) {
					if (en.getValue() instanceof Map) {
						@SuppressWarnings("unchecked")
						Map<String, Object> m = (Map<String, Object>) en.getValue();
						for (Map.Entry<String, Object> men : m.entrySet()) {
							properties.add(new KeyValuePair(en.getKey(),
									men.getValue() == null ? null : String.valueOf(men.getValue())));
						}
					} else
						properties.add(new KeyValuePair(en.getKey(),
								en.getValue() == null ? null : String.valueOf(en.getValue())));
				}
			} catch (ScriptException e) {
				throw new IOException("Failed to evaluate configuration script.", e);
			}
		}
		
	}

}
