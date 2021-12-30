package com.sshtools.forker.updater.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Module;
import org.apache.bcel.classfile.ModuleRequires;
import org.apache.bcel.classfile.Utility;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.Type;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.FileUtils;

import com.sshtools.forker.client.OSCommand;

public class Analyser {

	static class ItemContext {
		String module;

	}

	public static class AnalysedItem {

		private String module;
		private Artifact artifact;
		private Path file;
		private boolean useAsModule = true;
		private boolean explicitModule = false;

		public Artifact getArtifact() {
			return artifact;
		}

		public Path getFile() {
			return file == null ? (artifact == null ? null : artifact.getFile().toPath()) : file;
		}

		public boolean isArtifact() {
			return artifact != null;
		}

		public boolean isNonArtifact() {
			return !isArtifact() && file != null;
		}

		public boolean isForkerModule() {
			return (module != null && module.startsWith("com.sshtools.forker"))
					|| (file != null && file.getFileName().toString().startsWith("forker-"));
		}
		
		public boolean isExplicitModule() {
			return explicitModule;
		}
		
		public boolean isGeneratedModuleName() {
			return module == null;
		}

		public boolean isExplicitModuleOrHasMETAINFName() {
			return module != null;
		}

		public String getModule() {
			if (module == null) {
				Path artifactFile = getFile();
				String pathStr = artifactFile.getFileName().toString();
				if (pathStr.endsWith(".jar")) {
					pathStr = pathStr.substring(0, pathStr.length() - 4);
				}
				Pattern p = Pattern.compile("-(\\d+(\\.|$))");
				Matcher m = p.matcher(pathStr);
				if (m.find()) {
					pathStr = pathStr.substring(0, m.start());
				}
				pathStr = pathStr.replaceAll("[^A-Za-z0-9]", ".");
				pathStr = pathStr.replaceAll("\\.\\.", ".");
				while (pathStr.startsWith("."))
					pathStr = pathStr.substring(1);
				while (pathStr.endsWith("."))
					pathStr = pathStr.substring(0, pathStr.length() - 1);
				return pathStr;
			} else
				return module;
		}

		public void setModule(String module) {
			this.module = module;
		}

		public boolean isUseAsModule() {
			return useAsModule;
		}

		@Override
		public int hashCode() {
			return Objects.hash(artifact, file);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			AnalysedItem other = (AnalysedItem) obj;
			return Objects.equals(artifact, other.artifact) && Objects.equals(file, other.file);
		}

		public ArtifactRepository getRepository() {
			return null;
		}

		@Override
		public String toString() {
			return "AnalysedItem [module=" + module + ", artifact=" + artifact + ", file=" + file + "]";
		}
	}

	public static void main(String[] args) throws Exception {
//		Analyser a = new Analyser();
//		a.log(new DumbContext());
//		a.files.add(Paths.get(
//				"/home/tanktarta/.m2/repository/com/sshtools/forker-updater/1.7-SNAPSHOT/forker-updater-1.7-SNAPSHOT.jar"));
//		a.analyse();
		Pattern p = Pattern.compile("-(\\d+(\\.|$))");
		String pathStr = "svgSalamander-0.0.1-SNAPSHOT";
		Matcher m = p.matcher(pathStr);
		if(m.find()) {
			pathStr = pathStr.substring(0, m.start());
		}
		pathStr = pathStr.replaceAll("[^A-Za-z0-9]", ".");
		pathStr = pathStr.replaceAll("\\.\\.", ".");
		while (pathStr.startsWith("."))
			pathStr = pathStr.substring(1);
		while (pathStr.endsWith("."))
			pathStr = pathStr.substring(0, pathStr.length() - 1);
		System.out.println("PathStr: " + pathStr);
	}

	private List<String> mainClasses = new ArrayList<>();
	private Set<String> requiredModules = new LinkedHashSet<>();
	private Set<String> discoveredModules = new LinkedHashSet<>();
	private Set<String> availableSystemModules = new LinkedHashSet<>();
	private Set<AnalysedItem> analysedItems = new LinkedHashSet<>();
	private Set<Artifact> artifacts = new LinkedHashSet<>();
	private Set<Path> files = new LinkedHashSet<>();
	private Context context;
	private int indent;
	private Log log;

	public Analyser() {
	}

	public Analyser add(Analyser analyser) {
		mainClasses.addAll(analyser.mainClasses);
		requiredModules.addAll(analyser.requiredModules);
		discoveredModules.addAll(analyser.discoveredModules);
		analysedItems.addAll(analyser.analysedItems);
		availableSystemModules.addAll(analyser.availableSystemModules);
		return this;
	}

	public Context context() {
		return context;
	}

	public Analyser log(Context context) {
		this.context = context;
		this.log = context.getLog();
		return this;
	}

	public Set<AnalysedItem> analysedItems() {
		return analysedItems;
	}

	public int indent() {
		return indent;
	}

	public Analyser indent(int indent) {
		this.indent = indent;
		return this;
	}

	public List<String> mainClasses() {
		return mainClasses;
	}

	public Set<String> discoveredModules() {
		return Collections.unmodifiableSet(discoveredModules);
	}

	public Set<String> unresolvedModules() {
		Set<String> rem = remainingModules();
		for (Iterator<String> it = rem.iterator(); it.hasNext();) {
			String mod = it.next();
			if (isSystemModule(mod)) {
				it.remove();
			}
		}
		return rem;
	}

	public Set<String> availableSystemModules() {
		return availableSystemModules;
	}

	public Set<String> systemModules() {
		Set<String> rem = remainingModules();
		for (Iterator<String> it = rem.iterator(); it.hasNext();) {
			String mod = it.next();
			if (!isSystemModule(mod)) {
				it.remove();
			}
		}
		return rem;
	}

	public boolean forkerModules() {
		boolean forkerModules = false;
		for (AnalysedItem item : analysedItems) {
			forkerModules = forkerModules || item.isForkerModule();
		}
		return forkerModules;
	}

	protected boolean isSystemModule(String mod) {
		return availableSystemModules.contains(mod);
	}

	public Set<String> remainingModules() {
		Set<String> n = new LinkedHashSet<>();
		n.addAll(requiredModules);
		n.removeAll(discoveredModules);
		return n;
	}

	public Set<String> requiredModules() {
		return Collections.unmodifiableSet(requiredModules);
	}

	public void analyse() throws IOException {
		for (Path dir : files) {
			log.info(UpdaterUtil.indent(indent) + dir);
			indent++;
			AnalysedItem item = new AnalysedItem();
			analysedItems.add(item);
			item.file = dir;
			String includes = "**/*.class";
			try {
				if (Files.isDirectory(dir)) {

					ItemContext ctx = new ItemContext();

					/* Do module-info.class first if it exists to get module context */
					Path modinfo = dir.resolve("module-info.class");

					/* TODO: check MRJAR files too */

					if (Files.exists(modinfo)) {
						ClassParser parser = new ClassParser(modinfo.toAbsolutePath().toString());
						JavaClass javaClass = parser.parse();
						examineModinfo(javaClass, ctx);
					}

					for (File classFile : FileUtils.getFiles(dir.toFile(), includes, "")) {
						ClassParser parser = new ClassParser(classFile.getAbsolutePath());
						JavaClass javaClass = parser.parse();
						doJavaClass(javaClass, ctx);
					}
				} else {
					doJarFile(dir, item);
				}
			} finally {
				indent--;
			}
		}
		for (Artifact artifact : artifacts) {
			log.info(UpdaterUtil.indent(indent) + artifact.getGroupId() + ":" + artifact.getArtifactId() + " [" + artifact.getScope() + "]");
			if (artifact.getFile() == null) {
				log.warn("Skipping, no file found");
				continue;
			}
			indent++;
			Path jar = artifact.getFile().toPath();
			AnalysedItem item = new AnalysedItem();
			analysedItems.add(item);
			item.artifact = artifact;
			doJarFile(jar, item);
		}

		/*
		 * 2nd pass to find out which artifacts to use as modules. It will be used as a
		 * non-module (added to classpath) if it is an automatic module and is not
		 * specified as a dependency in any other module.
		 */
		log.info(UpdaterUtil.indent(indent) + "2nd pass, checking for artifacts used as modules.");
		for (AnalysedItem item : analysedItems) {
			if (item.isGeneratedModuleName()) {
				String moduleName = item.getModule();
				indent++;
				try {
					log.info(UpdaterUtil.indent(indent) + "Checking if automatic module " + moduleName + " is to be used on classpath or modulepath.");
					if(!requiredModules.contains(moduleName)) {
						indent++;
						try {
							log.info(UpdaterUtil.indent(indent) + "Module "  + moduleName + " is not in any other modules meta-data, will be added to classpath.");
							item.useAsModule = false;
						}
						finally {
							indent--;
						}
					}	
				}
				finally {
					indent--;
				}
			}
		}

		/* Load all of the available system modules */
		availableSystemModules = new LinkedHashSet<>();
		for (String line : OSCommand.runCommandAndCaptureOutput(context.getToolExecutable("java"), "--list-modules")) {
			String[] arr = line.split("\\@");
			availableSystemModules.add(arr[0]);
			log.info(UpdaterUtil.indent(indent + 1) + "Found available system module " + arr[0]);
		}
	}

	protected void doJarFile(Path jar, AnalysedItem item) throws IOException {
		try (JarFile jarFile = new JarFile(jar.toFile())) {
			String jarName = jar.toAbsolutePath().toString();

			Manifest mf = jarFile.getManifest();
			if (mf != null) {
				if (mf.getMainAttributes().getValue("Automatic-Module-Name") != null)
					item.module = mf.getMainAttributes().getValue("Automatic-Module-Name");
			}

			ItemContext ctx = new ItemContext();
			Set<JavaClass> javaClasses = collectJavaClasses(jarName, jarFile);

			/* Do module-info.class first if it exists to get module context */
			for (JavaClass javaClass : javaClasses) {
				if (javaClass.getClassName().equals("module-info")) {
					String module = examineModinfo(javaClass, ctx);
					if (module != null) {
						item.module = module;
						item.explicitModule = true;
					}
				}
			}

			for (JavaClass javaClass : javaClasses) {
				doJavaClass(javaClass, ctx);
			}
		} finally {
			indent--;
		}
	}

	private String examineModinfo(JavaClass javaClass, ItemContext ctx) {
		for (Attribute attr : javaClass.getAttributes()) {
			if (attr.getName().equals("Module")) {
				Module module = (Module) attr;

				/*
				 * TODO: Surely a better way? Cant seem to get at moduleNameIndex to get from
				 * ConstantPool
				 */
				try {
					Field fld = Module.class.getDeclaredField("moduleNameIndex");
					fld.setAccessible(true);
					String moduleName = module.getConstantPool()
							.getConstantString(fld.getInt(module), Const.CONSTANT_Module).replace('/', '.');
					ctx.module = moduleName;
					if (!discoveredModules.contains(moduleName)) {
						discoveredModules.add(moduleName);
						log.info(UpdaterUtil.indent(indent) + "Found module " + moduleName);
					}

					ModuleRequires[] requires = module.getRequiresTable();
					for (ModuleRequires r : requires) {

						/*
						 * TODO: Surely a better way? Again cant get at internals, have to use
						 * toString() and parse
						 */

						Field reqfld = ModuleRequires.class.getDeclaredField("requiresIndex");
						reqfld.setAccessible(true);
						final String module_name = module.getConstantPool().constantToString(reqfld.getInt(r),
								Const.CONSTANT_Module);
						String mn = Utility.compactClassName(module_name, false);
						if (!requiredModules.contains(mn)) {
							requiredModules.add(mn);
							log.info(UpdaterUtil.indent(indent) + "Found requirement " + mn);
						}
					}

					return moduleName;

				} catch (Exception e) {
					if (log.isDebugEnabled())
						log.error(UpdaterUtil.indent(indent) + "Skipped " + javaClass.getClassName(), e);
					else
						log.warn(UpdaterUtil.indent(indent) + "Skipped " + javaClass.getClassName() + " because "
								+ e.getMessage());
				}
			}
		}
		return null;

	}

	protected void doJavaClass(JavaClass javaClass, ItemContext ctx) {
		for (Method method : javaClass.getMethods()) {
			if (method.isStatic() && method.getName().equals("main") && method.getArgumentTypes().length == 1
					&& method.getArgumentTypes()[0] instanceof ArrayType
					&& ((ArrayType) method.getArgumentTypes()[0]).getElementType().equals(Type.STRING)) {
				String fullMainClassname = javaClass.getClassName();
				if (context.isModules() && StringUtils.isNotBlank(ctx.module))
					fullMainClassname = ctx.module + "/" + fullMainClassname;
				log.info(UpdaterUtil.indent(indent) + String.format("Found main @%s", fullMainClassname));
				mainClasses.add(fullMainClassname);
			}
		}
	}

	public Set<Artifact> getArtifacts() {
		return artifacts;
	}

	public Set<Path> getFiles() {
		return files;
	}

	private static Set<JavaClass> collectJavaClasses(String jarName, JarFile jarFile)
			throws ClassFormatException, IOException {
		Set<JavaClass> javaClasses = new LinkedHashSet<JavaClass>();
		Enumeration<JarEntry> entries = jarFile.entries();
		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			if (!entry.getName().endsWith(".class")) {
				continue;
			}

			ClassParser parser = new ClassParser(jarName, entry.getName());
			JavaClass javaClass = parser.parse();
			javaClasses.add(javaClass);
		}
		return javaClasses;
	}

	public AnalysedItem analysedItem(Artifact a) {
		for (AnalysedItem item : analysedItems) {
			if (item.getArtifact().equals(a)) {
				return item;
			}
		}
		throw new IllegalArgumentException("The artifact " + a + " not analysed.");
	}
}
