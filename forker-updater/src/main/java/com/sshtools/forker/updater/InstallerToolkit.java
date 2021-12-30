package com.sshtools.forker.updater;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

public interface InstallerToolkit {

	void init();

	int getPriority();

	boolean isAvailable();

	Set<Class<? extends Handler<?, ?>>> getHandlers();

	public static class Util {

		static List<InstallerToolkit> tks;
		static Map<Class<? extends Handler<?, ?>>, List<Handler<?, ?>>> impls = new HashMap<>();

		static <S> List<S> createAll(Class<S> clazz) {
			ServiceLoader<S> toolkits = ServiceLoader.load(clazz);
			List<S> tks = new ArrayList<>();
			Iterator<S> it = toolkits.iterator();
			while (it.hasNext()) {
				tks.add(it.next());
			}
			return tks;
		}

		public static InstallerToolkit getToolkit() {
			createToolkits();
			return tks.get(0);
		}

		@SuppressWarnings("unchecked")
		public static <H extends Handler<?, ?>> H getBestHandler(Class<? extends Handler<?, ?>> clazz) {
			createToolkits();
			List<H> s = (List<H>) impls.get(clazz);
			if (s == null) {
				s = (List<H>) createAll(clazz);
				impls.put(clazz, (List<Handler<?, ?>>) s);
			}
			for (H h : s) {
				if (getToolkit().getHandlers().contains(h.getClass())) {
					return h;
				}
			}
			throw new IllegalArgumentException(String.format("Couldn't find any registered handler of type %s", clazz));
		}

		protected static void createToolkits() {
			if (tks == null) {
				tks = new ArrayList<>();
				for (InstallerToolkit t : createAll(InstallerToolkit.class)) {
					if (t.isAvailable())
						tks.add(t);
				}
				Collections.sort(tks, (a, b) -> {
					return Integer.valueOf(a.getPriority()).compareTo(b.getPriority()) * -1;
				});
				for (InstallerToolkit tk : tks) {
					tk.init();
					break;
				}
			}
		}
	}

}
