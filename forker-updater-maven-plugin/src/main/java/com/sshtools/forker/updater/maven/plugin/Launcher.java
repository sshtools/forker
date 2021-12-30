package com.sshtools.forker.updater.maven.plugin;

import static com.sshtools.forker.updater.maven.plugin.UpdaterUtil.checkDir;
import static com.sshtools.forker.updater.maven.plugin.UpdaterUtil.escapeSpaces;
import static com.sshtools.forker.updater.maven.plugin.UpdaterUtil.getExtension;
import static com.sshtools.forker.updater.maven.plugin.UpdaterUtil.getFileName;
import static com.sshtools.forker.updater.maven.plugin.UpdaterUtil.normalizeForUri;
import static com.sshtools.forker.updater.maven.plugin.UpdaterUtil.resolvePath;
import static com.sshtools.forker.updater.maven.plugin.UpdaterUtil.resolveUrl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ProcessBuilder.Redirect;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.transfer.dependencies.resolve.DependencyResolverException;

import com.sshtools.forker.common.OS;
import com.sshtools.forker.updater.AppManifest;
import com.sshtools.forker.updater.AppManifest.Section;
import com.sshtools.forker.updater.AppManifest.Type;
import com.sshtools.forker.updater.Entry;
import com.sshtools.forker.updater.maven.plugin.Analyser.AnalysedItem;
import com.sun.jna.Platform;

public class Launcher extends AbstractComponent {

	@Parameter(required = true)
	private String id;

	@Parameter
	private Boolean useArgFile;

	@Parameter
	private boolean skip;

	@Parameter
	private LauncherMode mode = LauncherMode.DIRECT;

	@Parameter
	private LauncherType type = LauncherType.USER_APPLICATION;

	@Parameter(defaultValue = "", property = "splash")
	private String splash;

	@Parameter
	private List<String> vmArgs;

	@Parameter
	private List<String> updaterArgs;

	@Parameter
	private String mainClass;

	@Parameter
	private List<String> forkerArgs;

	@Parameter
	private File forkerArgFile;

	@Parameter
	private Map<String, String> systemProperties;

	@Parameter
	private List<String> linkOptions;

	public Launcher() {
		super(2);
	}

	public List<String> getLinkOptions() {
		return linkOptions;
	}

	public void setLinkOptions(List<String> linkOptions) {
		this.linkOptions = linkOptions;
	}

	public String getMainClass() {
		return mainClass;
	}

	public void setMainClass(String mainClass) {
		this.mainClass = mainClass;
	}

	public boolean isSkip() {
		return skip;
	}

	public void setSkip(boolean skip) {
		this.skip = skip;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public LauncherMode getMode() {
		return mode;
	}

	public void setMode(LauncherMode mode) {
		this.mode = mode;
	}

	public LauncherType getType() {
		return type;
	}

	public void setType(LauncherType type) {
		this.type = type;
	}

	@Override
	public void onProcess() throws IOException, MojoExecutionException, DependencyResolverException, URISyntaxException,
			MojoFailureException {
	}

	@Override
	protected String description() {
		return "Launcher " + id + " (" + mode + ") [" + type + "]";
	}

	@Override
	protected void onPostProcess() throws IOException, URISyntaxException {
		AppManifest manifest = context.getManifest();

		AbstractStage stage;
		switch (mode) {
		case UNINSTALLER:
		case UPDATER:
		case WRAPPED:
			stage = context.getBootstrap();
			break;
		default:
			stage = context.getBusiness();
			break;
		}

		String splash = calcSplash();
		if (StringUtils.isNotBlank(splash)) {
			Path splashPath = context.getImagePath().resolve("splash." + getExtension(splash));
			Files.copy(Paths.get(splash), splashPath, StandardCopyOption.REPLACE_EXISTING);
			manifest.entries()
					.add(new Entry(splashPath, manifest).section(stage.getSection())
							.path(context.getImagePath().relativize(splashPath))
							.uri(new URI(resolveUrl(normalizeForUri(context.getRemoteBase()),
									context.getImagePath().relativize(splashPath).toString())))
							.type(Type.OTHER));
			if (context.isRepository()) {
				Path repositorySplashPath = context.getRepositoryPath().resolve("splash." + getExtension(splash));
				Files.copy(Paths.get(splash), repositorySplashPath, StandardCopyOption.REPLACE_EXISTING);
			}
		}

		String mainClass = this.mainClass;
		if(mainClass == null) {
			List<String> mainClasses = calcAnalyserForLauncher(stage).mainClasses();
			if(!mainClasses.isEmpty())
				mainClass = mainClasses.get(0);
		}
		if (mainClass == null)
			throw new IllegalStateException("No main class could be determined for launcher " + id);

		/* App configuration */
		if (mode.isRequiresAppCfg()) {
			Path appCfgPath = context.getImagePath().resolve(id + ".cfg");
			checkDir(context.getImagePath().resolve(id + ".cfg.d"));
			writeAppCfg(stage, manifest, mainClass, appCfgPath, context.getImagePath());
			manifest.entries()
					.add(new Entry(appCfgPath, manifest).section(stage.getSection())
							.path(context.getImagePath().relativize(appCfgPath))
							.uri(new URI(resolveUrl(normalizeForUri(context.getRemoteBase()),
									context.getImagePath().relativize(appCfgPath).toString())))
							.type(Type.OTHER));
			if (context.isRepository()) {
				Path repositoryAppCfgPath = context.getRepositoryPath().resolve(id + ".cfg");
				writeAppCfg(stage, manifest, mainClass, repositoryAppCfgPath, context.getImagePath());
			}
		}

		/* Launcher script */

		if(calcLink()) {
			Path basePath = checkDir(context.getImagePath());
			Path exePath = checkDir(basePath.resolve("bin")).resolve( calcFullScriptName());
			log.info("Linking " + id);
			
			List<String> modulePaths = new ArrayList<>();
			List<String> vmopts = new ArrayList<>();
			List<String> classPaths = new ArrayList<>();
			StringBuilder updaterArgs = new StringBuilder();

			scriptArgs(basePath, modulePaths, vmopts, classPaths, updaterArgs, stage);
			
			List<String> args = new ArrayList<>();
			args.add(context.getToolExecutable("native-image"));
			for(String lopt : calcLinkOptions()) {
				args.add(lopt);				
			}
			args.add("-cp");
			args.add(String.join(File.pathSeparator, UpdaterUtil.concat(classPaths, modulePaths)));
			args.add(calcInitialMainClass(mainClass, false));
			args.add(exePath.toString());
			defaultIndent++;
			try {
				info(String.join(" ", args));
				ProcessBuilder pb = new ProcessBuilder(args);
				pb.redirectError(Redirect.INHERIT);
				pb.redirectInput(Redirect.INHERIT);
				pb.redirectOutput(Redirect.INHERIT);
				pb.directory(basePath.toFile());
				Process pr = pb.start();
				try {
					if (pr.waitFor() != 0)
						throw new IOException("Link failed with exit code " + pr.exitValue());
				} catch (InterruptedException ie) {
					throw new IOException("Interrupted.");
				}
			}
			finally {
				defaultIndent--;
			}
		}
		else {
			Path basePath = checkDir(context.getImagePath());
			Path argsPath = calcArgsPath(basePath);
			Path scriptPath = writeLauncherScript(stage, mainClass, argsPath, basePath);
			if (context.getBootstrap().isUpdateable()) {
				basePath = checkDir(context.getRepositoryPath());
				argsPath = calcArgsPath(basePath);
				scriptPath = writeLauncherScript(stage, mainClass, argsPath, basePath);
				manifest.entries()
						.add(new Entry(scriptPath, manifest).section(Section.BOOTSTRAP)
								.path(context.getRepositoryPath().relativize(scriptPath))
								.uri(new URI(resolveUrl(normalizeForUri(context.getRemoteBase()),
										normalizeForUri(context.getRepositoryPath().relativize(scriptPath).toString()))))
								.type(Type.OTHER));
	
				if (calcUseArgFile()) {
					manifest.entries()
							.add(new Entry(argsPath, manifest).section(Section.BOOTSTRAP)
									.path(context.getRepositoryPath().relativize(argsPath))
									.uri(new URI(resolveUrl(normalizeForUri(context.getRemoteBase()),
											context.getRepositoryPath().relativize(argsPath).toString())))
									.type(Type.OTHER));
				}
			}
		}
	}

	protected boolean calcUseArgFile() {
		return useArgFile == null ? context.isUseArgFile() : useArgFile;
	}

	Path writeLauncherScript(AbstractStage stage, String mainClass, Path argsPath, Path basePath) throws IOException {

		String fullScriptName = calcFullScriptName();

		Path scriptPath = checkDir(basePath.resolve("bin")).resolve(fullScriptName);

		List<String> modulePaths = new ArrayList<>();
		List<String> vmopts = new ArrayList<>();
		List<String> classPaths = new ArrayList<>();
		StringBuilder updaterArgs = new StringBuilder();

		boolean useModules = scriptArgs(basePath, modulePaths, vmopts, classPaths, updaterArgs, stage);

		if (Platform.isWindows()) {
			try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(scriptPath), true)) {
				out.println("@ECHO OFF");
				out.println("CD %~dp0\\..");
				if (argsPath != null) {
					try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(argsPath))) {
						for (String vmopt : vmopts) {
							pw.println(vmopt);
						}
					}
					out.println(String.format("SET VM_OPTIONS=\"@%s\"", argsPath.getFileName().toString()));
				} else
					out.println(String.format("SET VM_OPTIONS=\"%s\"", String.join(" ", vmopts)));

				if (context.isLinkJVM()) {
					out.println("SET JAVA_EXE=bin\\java.exe");
				} else {
					out.println(String.format("IF \"%JAVA_HOME%\"==\"\" ( "));
					out.println("    SET JAVA_EXE=${JAVA_HOME}\\bin\\java.exe");
					out.println(") ELSE (");
					out.println("    SET JAVA_EXE=java");
					out.println(")");
				}

				out.println(String.format("SET APP_ARGS=\"%s\"", updaterArgs.toString()));
				if (context.getBootstrap().isUpdateable() && mode == LauncherMode.UPDATER) {
					// out.println("while : ; do");
					if (useModules)
						out.println(
								"    %JAVA_EXE% %VM_OPTIONS% -m com.sshtools.forker.updater/com.sshtools.forker.updater.Updater %APP_ARGS% %*");
					else
						out.println("    %JAVA_EXE% %VM_OPTIONS% com.sshtools.forker.updater.Updater %APP_ARGS% %*");
//					out.println("    ret=$?");
//					out.println("    if [ \"${ret}\" != 9 -o ! -d .updates ]; then");
//					out.println("        exit $ret");
//					out.println("    else");
//					out.println("        echo Updating bootstrap ....");
//					out.println("        cd .updates");
//					out.println("        if ! find . -type d -exec mkdir -p ../\\{} \\; ; then");
//					out.println("            echo \"$0: Failed to recreate directory structure.\" >&2");
//					out.println("            exit 2");
//					out.println("        fi");
//					out.println("        if ! find . -type f -exec mv -f \\{} ../\\{} \\; ; then");
//					out.println("            echo \"$0: Failed to move update files.\" >&2");
//					out.println("            exit 2");
//					out.println("        fi");
//					out.println("        if ! find . -type l -exec mv -f \\{} ../\\{} \\; ; then");
//					out.println("            echo \"$0: Failed to move link files.\" >&2");
//					out.println("            exit 2");
//					out.println("        fi");
//					out.println("        cd ..");
//					out.println("        rm -fr .updates");
//					out.println("    fi");
//					out.println("done");
				} else {
					if(useModules)
						out.println("%JAVA_EXE% %VM_OPTIONS% -m " + calcInitialMainClass(mainClass, useModules) + " %APP_ARGS% %*");
					else
						out.println("%JAVA_EXE% %VM_OPTIONS% " + calcInitialMainClass(mainClass, useModules) + " %APP_ARGS% %*");
				}
			}
		} else if (OS.isUnix()) {
			try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(scriptPath), true)) {
				out.println("#!/bin/sh");
				out.println("realpath=$(readlink \"$0\")");
				out.println("if [ -z \"${realpath}\" ] ; then realpath=\"$0\" ; fi");
				out.println("cd $(dirname ${realpath})/..");

				if (argsPath != null) {
					try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(argsPath))) {
						for (String vmopt : vmopts) {
							pw.println(vmopt);
						}
					}
					out.println(String.format("VM_OPTIONS=\"@%s\"", argsPath.getFileName().toString()));
				} else
					out.println(String.format("VM_OPTIONS=\"%s\"", String.join(" ", vmopts)));

				if (context.isLinkJVM()) {
					out.println("JAVA_EXE=bin/java");
				} else {
					out.println(String.format("if [ -n \"${JAVA_HOME}\" ] ; then "));
					out.println("    JAVA_EXE=${JAVA_HOME}/bin/java");
					out.println("else");
					out.println("    JAVA_EXE=java");
					out.println("fi");
				}

				out.println(String.format("APP_ARGS=\"%s\"", updaterArgs.toString()));
				if (context.getBootstrap().isUpdateable() && mode == LauncherMode.UPDATER) {
					out.println("while : ; do");
					if (useModules)
						out.println(
								"    ${JAVA_EXE} ${VM_OPTIONS} -m com.sshtools.forker.updater/com.sshtools.forker.updater.Updater ${APP_ARGS} $@");
					else
						out.println("    ${JAVA_EXE} ${VM_OPTIONS} com.sshtools.forker.updater.Updater ${APP_ARGS} $@");
					out.println("    ret=$?");
					out.println("    if [ \"${ret}\" != 9 -o ! -d .updates ]; then");
					out.println("        rm -fr .updates");
					out.println("        exit $ret");
					out.println("    else");
					out.println("        echo Updating bootstrap ....");
					out.println("        cd .updates");
					out.println("        if ! find . -type d -exec mkdir -p ../\\{} \\; ; then");
					out.println("            echo \"$0: Failed to recreate directory structure.\" >&2");
					out.println("            exit 2");
					out.println("        fi");
					out.println("        if ! find . -type f -exec mv -f \\{} ../\\{} \\; ; then");
					out.println("            echo \"$0: Failed to move update files.\" >&2");
					out.println("            exit 2");
					out.println("        fi");
					out.println("        if ! find . -type l -exec mv -f \\{} ../\\{} \\; ; then");
					out.println("            echo \"$0: Failed to move link files.\" >&2");
					out.println("            exit 2");
					out.println("        fi");
					out.println("        cd ..");
					out.println("        rm -fr .updates");
					out.println("    fi");
					out.println("done");
				} else {
					if(useModules)
						out.println("${JAVA_EXE} ${VM_OPTIONS} -m " + calcInitialMainClass(mainClass, useModules) + " ${APP_ARGS} $@");
					else
						out.println("${JAVA_EXE} ${VM_OPTIONS} " + calcInitialMainClass(mainClass, useModules) + " ${APP_ARGS} $@");
				}
			}
		} else
			throw new UnsupportedOperationException("Cannot create launch script for this platform.");
		scriptPath.toFile().setExecutable(true);
		return scriptPath;
	}

	public String calcFullScriptName() {
		return calcFullName("bat");
	}

	public String calcFullExeName() {
		return calcFullName("exe");
	}

	protected String calcFullName(String ext) {
		String fullName = id;
		if (Platform.isWindows() && !fullName.toLowerCase().endsWith("." + ext)) {
			fullName += "." + ext;
		}
		return fullName;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Launcher other = (Launcher) obj;
		return Objects.equals(id, other.id);
	}
	
	protected String calcInitialMainClass(String mainClass, boolean modules) {
		if (mode == LauncherMode.UNINSTALLER) {
			if (modules)
				return "com.sshtools.forker.updater/com.sshtools.forker.updater.Uninstaller";
			else
				return "com.sshtools.forker.updater.Uninstaller";
		} else if (context.getBusiness().isUpdateable() && mode == LauncherMode.UPDATER) {
			if (modules)
				return "com.sshtools.forker.updater/com.sshtools.forker.updater.Updater";
			else
				return "com.sshtools.forker.updater.Updater";
		} else {
			if (mode == LauncherMode.WRAPPED) {
				if (modules)
					return "com.sshtools.forker.wrapper/com.sshtools.forker.wrapper.ForkerWrapper";
				else
					return "com.sshtools.forker.wrapper.ForkerWrapper";
			} else if (mode == LauncherMode.DIRECT) {
				return mainClass;
			} else
				throw new UnsupportedOperationException();
		}
	}

	protected Map<String, String> calcSystemProperties() {
		Map<String, String> s = new LinkedHashMap<>();
		if (context.getSystemProperties() != null)
			s.putAll(context.getSystemProperties());
		if (systemProperties != null)
			s.putAll(systemProperties);
		return s;
	}

	protected List<String> calcVmArgs() {
		List<String> s = new ArrayList<>();
		if (vmArgs != null)
			s.addAll(vmArgs);
		if (context.getVmArgs() != null)
			s.addAll(context.getVmArgs());
		return s;
	}

	protected List<String> calcLinkOptions() {
		List<String> s = new ArrayList<>();
		if (linkOptions != null)
			s.addAll(linkOptions);
		if (context.getLinkOptions() != null)
			s.addAll(context.getLinkOptions());
		return s;
	}

	protected Path calcArgsPath(Path binPath) {
		return calcUseArgFile() ? checkDir(binPath).resolve(id + ".args") : null;
	}

	protected Analyser calcAnalyserForLauncher(AbstractStage stage) {
		Analyser newAl = new Analyser();
		newAl.add(al);
		if(newAl.analysedItems().isEmpty() || mode.isBootstrap()) {
			newAl.add(stage.al);
		}
		return newAl;
	}

	protected String calcSplash() {
		if (splash != null) {
			return splash;
		}
		return context.getSplash() == null ? "" : context.getSplash();
	}

	protected String resolveUserConfigPath(String path) {
		switch (type) {
		case USER_APPLICATION:
		case USER_SERVICE:
			return "${user.home}/." + context.getLauncherScriptName() + "/" + path;
		default:
			return path;
		}
	}

	private void writeAppCfg(AbstractStage stage, AppManifest manifest, String mainClass, Path appCfgPath,
			Path imagePath) throws IOException {
		try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(appCfgPath), true)) {
			out.println("configuration-directory " + id + ".cfg.d");
			switch (mode) {
			case UPDATER:
				info("Writing updater configuration");
				out.println("local-manifest manifest.xml");
				out.println("default-remote-manifest " + normalizeForUri(context.getRemoteBase()) + "/manifest.xml");
				if (context.getInstallLocation() != null && !context.getInstallLocation().equals(""))
					out.println("install-location " + context.getInstallLocation());
				break;
			default:
				break;
			}

			info("Writing file paths");
			out.println("pidfile " + resolveUserConfigPath(id + ".pid"));
			out.println("argfile " + resolveUserConfigPath(id + "-app.args"));
			out.println("log " + resolveUserConfigPath(id + ".log"));

			info("Writing VM arguments");
			for (String vmArg : calcVmArgs()) {
				out.println("jvmarg " + vmArg);
			}

			info("Writing system properties");
			for (Map.Entry<String, String> en : calcSystemProperties().entrySet()) {
				out.println(en.getKey() + " " + en.getValue());
			}

			info("Writing common configuration");
			Set<String> cp = new LinkedHashSet<>();
			Set<String> mp = new LinkedHashSet<>();
			Path root = Paths.get("/");
			for (Entry entry : manifest.entries(Section.APP)) {
				switch (entry.type()) {
				case CLASSPATH:
					cp.add(root.relativize(entry.resolve(root)).toString());
					break;
				case MODULEPATH:
					mp.add(root.relativize(entry.resolve(root)).toString());
					break;
				default:
					break;
				}
			}
			if (cp.size() > 0) {
				out.println("classpath " + String.join(File.pathSeparator, cp));
			}
			if (mp.size() > 0) {
				out.println("modulepath " + String.join(File.pathSeparator, mp));
			}

			/* Forker configuration from various places. */
			if (context.getForkerArgsFile() != null) {
				info("Appending global forkerArgFile " + forkerArgFile);
				try (BufferedReader reader = Files.newBufferedReader(context.getForkerArgsFile().toPath())) {
					String line = null;
					while ((line = reader.readLine()) != null)
						out.println(line);
				}
			}
			if (context.getForkerArgs() != null) {
				info("Appending global forkerArgs");
				for (String forkerArg : context.getForkerArgs())
					out.println(forkerArg.trim());
			}
			Path template = context.resolveLaunchersSrcPath().resolve(id + ".cfg");
			info("Looking for template " + template);
			if (Files.exists(template)) {
				info("Appending template " + template);
				try (BufferedReader reader = Files.newBufferedReader(template)) {
					String line = null;
					while ((line = reader.readLine()) != null)
						out.println(line);
				}
			}
			if (forkerArgs != null) {
				info("Appending launcher forkerArgs");
				for (String forkerArg : forkerArgs)
					out.println(forkerArg.trim());
			}
			if (forkerArgFile != null) {
				info("Appending launcher forkerArgFile " + forkerArgFile);
				try (BufferedReader reader = Files.newBufferedReader(forkerArgFile.toPath())) {
					String line = null;
					while ((line = reader.readLine()) != null)
						out.println(line);
				}
			}
			if (mainClass != null)
				out.println("main " + mainClass);
		}
	}

	private boolean scriptArgs(Path basePath, List<String> modulePaths, List<String> vmopts, List<String> classPaths,
			StringBuilder updaterArgs, AbstractStage stage) {

		Path stagePath = stage.resolvePath();

		Analyser al = calcAnalyserForLauncher(stage);

		Set<String> allSystemModules = al.systemModules();

		final boolean useModules = al.forkerModules() && context.isModules();

		Path moduleDir = resolvePath(stagePath, context.getModulePath());
		Path classesDir = resolvePath(stagePath, context.getClassPath());

		if (mode.isRequiresAppCfg()) {
			for (AnalysedItem item : context.getBootstrap().getAnalyser().analysedItems()) {
				if (context.isModules() && item.isUseAsModule()) {
					modulePaths.add(moduleDir + File.separator
							+ getFileName(context.getBootstrap().includeVersion, item.getArtifact()));
				} else {
					classPaths.add(classesDir + File.separator
							+ getFileName(context.getBootstrap().includeVersion, item.getArtifact()));
				}
			}
		} else {
			for (AnalysedItem item : al.analysedItems()) {
				if (context.isModules() && item.isUseAsModule()) {
					modulePaths.add(moduleDir + File.separator + getFileName(stage.includeVersion, item.getArtifact()));
				} else {
					classPaths.add(classesDir + File.separator + getFileName(stage.includeVersion, item.getArtifact()));
				}
			}
			for (String vmArg : calcVmArgs()) {
				vmopts.add(vmArg);
			}
			for (Map.Entry<String, String> en : calcSystemProperties().entrySet()) {
				vmopts.add("-D" + en.getKey() + "=" + en.getValue());
			}
		}

		String splash = calcSplash();
		if (!splash.isEmpty())
			vmopts.add("-splash:splash." + getExtension(splash));

		if (!modulePaths.isEmpty()) {
			vmopts.add("-p");
			vmopts.add(String.join(File.pathSeparator, modulePaths));
		}
		if (!classPaths.isEmpty()) {
			vmopts.add("-cp");
			vmopts.add(String.join(File.pathSeparator, classPaths));
		}
		if (!allSystemModules.isEmpty()) {
			vmopts.add("--add-modules");
			vmopts.add(String.join(",", allSystemModules));
		}
		vmopts.add("-Dforker.remoteManifest=" + normalizeForUri(context.getRemoteBase()));

		if (mode.isRequiresAppCfg()) {
			updaterArgs.append("--configuration=");
			updaterArgs.append(id + ".cfg");
		}

		if (this.updaterArgs != null) {
			for (String arg : this.updaterArgs) {
				updaterArgs.append(" ");
				updaterArgs.append(escapeSpaces(arg));
			}
		}
		if (context.getUpdaterArgs() != null) {
			for (String arg : context.getUpdaterArgs()) {
				updaterArgs.append(" ");
				updaterArgs.append(escapeSpaces(arg));
			}
		}

		return useModules;
	}
}
