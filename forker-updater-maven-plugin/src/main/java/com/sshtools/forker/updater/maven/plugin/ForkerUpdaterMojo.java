package com.sshtools.forker.updater.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.languages.java.jpms.LocationManager;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import com.sshtools.forker.common.OS;
import com.sshtools.forker.common.Util;
import com.sshtools.forker.updater.AppManifest;
import com.sshtools.forker.updater.AppManifest.Section;
import com.sshtools.forker.updater.AppManifest.Type;
import com.sshtools.forker.updater.Entry;
import com.sun.jna.Platform;

@Mojo(threadSafe = true, name = "updates", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresProject = true)
public class ForkerUpdaterMojo extends AbstractMojo {
	private static final String MAVEN_BASE = "https://repo1.maven.org/maven2";
	
	public enum PackageMode {
		NONE, JPACKAGE, SELF_EXTRACTING
	}
	

	/**
	 * Location of the file.
	 */
	@Parameter(defaultValue = "${project.build.directory}/image", property = "imageDirectory", required = true)
	private File imageDirectory;

	/**
	 * Location of the file.
	 */
	@Parameter(defaultValue = "${project.build.directory}/repository", property = "repositoryDirectory", required = true)
	private File repositoryDirectory;

	/**
	 * Location of the classpath jars.
	 */
	@Parameter(defaultValue = "app/business", property = "businessPath", required = true)
	private String businessPath;

	/**
	 * Location of the classpath jars.
	 */
	@Parameter(defaultValue = "app/bootstrap", property = "bootstrapPath", required = true)
	private String bootstrapPath;

	/**
	 * Location of the classpath jars under business or bootstrap.
	 */
	@Parameter(defaultValue = "classpath", property = "classpath", required = true)
	private String classPath;

	/**
	 * Location of the modulepath jars.
	 */
	@Parameter(defaultValue = "modulepath", property = "modulepath", required = true)
	private String modulePath;

	/**
	 * Location of the remote base.
	 */
	@Parameter(required = true)
	private String remoteBase;

	/**
	 * Location of the remote jars.
	 */
	@Parameter
	private String remoteJars;
	/**
	 * Location of the remote config file.
	 */
	@Parameter
	private String remoteConfig;
	/**
	 * List of system modules (overrides automatic detection of type)
	 */
	@Parameter
	private List<String> systemModules;
	/**
	 * List of classpath jars (overrides automatic detection of type)
	 */
	@Parameter
	private List<String> classpathArtifacts;
	/**
	 * List of automatic modules
	 */
	@Parameter
	private List<String> automaticArtifacts;

	/**
	 * List of bootstrap class path
	 */
	@Parameter
	private List<String> bootstrapArtifacts;
	/**
	 * List of bootstrap only class path
	 */
	@Parameter
	private List<String> businessArtifacts;
	/**
	 * List of bootstrap only modules
	 */
	@Parameter
	private List<String> sharedArtifacts;

	@Parameter
	private Map<String, String> artifactArch = new HashMap<>();

	@Parameter
	private Map<String, String> artifactOS = new HashMap<>();

	@Parameter
	private List<String> updaterArgs;

	@Parameter
	private List<String> vmArgs;

	@Parameter
	private List<String> forkerArgs;

	@Parameter
	private List<BootstrapFile> bootstrapFiles;

	@Parameter
	private List<AppFile> appFiles;

	@Parameter
	private Map<String, String> properties;

	@Parameter(defaultValue = "true", property = "noHeaderFiles")
	private boolean noHeaderFiles;

	@Parameter(defaultValue = "true", property = "noManPages")
	private boolean noManPages;

	@Parameter(defaultValue = "true", property = "updateableBootstrap")
	private boolean updateableBootstrap;

	@Parameter(defaultValue = "true", property = "updateable")
	private boolean updateable;

	@Parameter(defaultValue = "true", property = "include")
	private boolean includeProject;

	@Parameter(defaultValue = "2", property = "compress")
	private int compress;

	@Parameter(defaultValue = "false")
	private boolean includeVersion;

	@Parameter(property = "mainClass", required = true)
	private String mainClass;

	@Parameter(property = "launcherScriptName", defaultValue = "${project.artifactId}", required = true)
	private String launcherScriptName;

	@Parameter(property = "installLocation", defaultValue = "${installer.home}/${project.artifactId}", required = true)
	private String installLocation;

	@Parameter(defaultValue = "true", property = "repository")
	private boolean repository;

	@Parameter(defaultValue = "true", property = "image")
	private boolean image;

	@Parameter(defaultValue = "", property = "splash")
	private String splash;

	@Parameter(defaultValue = "true", property = "link")
	private boolean link;

	@Parameter(defaultValue = "${project.artifactId}", property = "id", required = true)
	private String id;

	@Parameter(defaultValue = "false", property = "remotesFromOriginalSource")
	private boolean remotesFromOriginalSource;

	@Parameter(defaultValue = "true", property = "includeForkerUpdaterRuntimeModules")
	private boolean includeForkerUpdaterRuntimeModules = true;

	@Parameter(defaultValue = "${project.version}-${timestamp}", property = "appVersion")
	private String version;

	@Parameter(defaultValue = "true", property = "removeImageBeforeLink")
	private boolean removeImageBeforeLink = true;

	@Parameter(defaultValue = "true", property = "useArgfile")
	private boolean useArgfile = true;

	@Parameter(defaultValue = "true", property = "modules")
	private boolean modules = true;

	@Parameter(defaultValue = "SELF_EXTRACTING", property = "packageMode")
	private PackageMode packageMode = PackageMode.SELF_EXTRACTING;

	@Parameter(defaultValue = "${project.build.directory}/packages", property = "packagePath")
	private String packagePath;

	/**
	 * The maven project.
	 */
	@Parameter(required = true, readonly = true, property = "project")
	protected MavenProject project;

	@Component
	protected ToolchainManager toolchainManager;

	@Component
	private RepositorySystem repoSystem;

	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
	private RepositorySystemSession repoSession;

	@Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
	private List<RemoteRepository> repositories;

	/**
	 * <p>
	 * Specify the requirements for this jdk toolchain. This overrules the toolchain
	 * selected by the maven-toolchain-plugin.
	 * </p>
	 * <strong>note:</strong> requires at least Maven 3.3.1
	 */
	@Parameter
	protected Map<String, String> jdkToolchain;

	@Parameter(defaultValue = "${session}", readonly = true, required = true)
	protected MavenSession session;

	@Component
	protected LocationManager locationManager;

	public void execute() throws MojoExecutionException {
		AppManifest manifest = new AppManifest();
		manifest.id(id);
		try {
			manifest.baseUri(new URI(normalizeForUri(remoteBase)));
		} catch (URISyntaxException e1) {
			throw new MojoExecutionException("Invalid remote base.", e1);
		}
		if (!version.equals(""))
			manifest.version(version.replace("${timestamp}", String.valueOf(
					new SimpleDateFormat("yyyyMMddHHmmss").format(new Date(manifest.timestamp().toEpochMilli())))));
		Path businessPathObj = Paths.get(businessPath);
		Path bootstrapPathObj = Paths.get(bootstrapPath);
		manifest.sectionPath(Section.APP, businessPathObj);
		manifest.sectionPath(Section.BOOTSTRAP, bootstrapPathObj);

		Path imagePath = imageDirectory.toPath();
		Path repositoryPath = repositoryDirectory.toPath();

		try {

			if (link) {
				if (removeImageBeforeLink) {
					getLog().info("Clearing jlink directory '" + imageDirectory + "'");
					Util.deleteRecursiveIfExists(imageDirectory);
				}
				ProcessBuilder pb = new ProcessBuilder(getJLinkExecutable());
				if (systemModules != null) {
					pb.command().add("--add-modules");
					pb.command().add(String.join(",", systemModules));
				}
				pb.command().add("--output");
				pb.command().add(imageDirectory.getPath());
				pb.redirectErrorStream(true);
				if (noHeaderFiles)
					pb.command().add("--no-header-files");
				if (noManPages)
					pb.command().add("--no-man-pages");
				pb.command().add("--compress=" + compress);
				getLog().info("Running jlink '" + String.join(" ", pb.command()) + "'");
				Process p = pb.start();
				try (InputStream in = p.getInputStream()) {
					IOUtil.copy(in, System.out);
				}
				try {
					if (p.waitFor() != 0)
						throw new MojoExecutionException(String.format("The command '%s' failed with exit value %d",
								String.join(" ", pb.command()), p.exitValue()));
				} catch (InterruptedException e) {
					throw new RuntimeException("Interrupted.", e);
				}
			}
			checkDir(imagePath);

			Set<Artifact> artifacts = project.getArtifacts();
			for (Artifact a : artifacts) {
				doArtifact(manifest, bootstrapPathObj, businessPathObj, a);
			}
			if (includeProject)
				doArtifact(manifest, bootstrapPathObj, businessPathObj, project.getArtifact());

			if (bootstrapFiles != null) {
				for (BootstrapFile file : bootstrapFiles) {
					Path path = Paths.get(file.source);
					Path target = imagePath
							.resolve(file.target.equals(".") ? imagePath : imagePath.resolve(file.target))
							.resolve(path.getFileName());
					copy("Bootstrap File", path, target, manifest.timestamp());
					if (updateableBootstrap) {
						manifest.entries()
								.add(new Entry(target, manifest).section(Section.BOOTSTRAP).path(imagePath.relativize(target))
										.uri(new URI(resolveUrl(remoteBase, imagePath.relativize(target).toString())))
										.type(Type.OTHER));
						target = repositoryPath
								.resolve(file.target.equals(".") ? repositoryPath : repositoryPath.resolve(file.target))
								.resolve(path.getFileName());
						copy("Updateable Bootstrap File", path, target, manifest.timestamp());
					}
				}
			}

			if (appFiles != null) {
				for (AppFile file : appFiles) {
					Path path = Paths.get(file.source);
					Path target = imagePath
							.resolve(file.target.equals(".") ? businessPathObj : businessPathObj.resolve(file.target))
							.resolve(path.getFileName());
					Path relTarget = imagePath.relativize(target);
					copy("App file", path, target, manifest.timestamp());
					manifest.entries()
							.add(new Entry(target, manifest).section(Section.APP).path(relTarget)
									.uri(new URI(resolveUrl(normalizeForUri(remoteBase), relTarget.toString())))
									.type(Type.OTHER));
				}
			}

			/* Anything thats not in the app base is a boostrap entry */
			Path mf = imagePath.resolve("manifest.xml");
			if (updateableBootstrap) {
				Files.walk(imagePath).forEach(s -> {
					if (Files.isRegularFile(s) && !mf.equals(s)) {
						Path relPath = imagePath.relativize(s);
						if (!relPath.startsWith(businessPath) && !relPath.startsWith(bootstrapPath)
								&& !manifest.hasPath(relPath)) {
							try {
								manifest.entries()
										.add(new Entry(s, manifest).section(Section.BOOTSTRAP).type(Type.OTHER).path(Paths.get("/").resolve(relPath))
												.uri(new URI(resolveUrl(normalizeForUri(remoteBase),
														normalizeForUri(relPath.toString())))));
							} catch (IOException | URISyntaxException e) {
								throw new IllegalStateException("Failed to construct bootstrap manifest entry.", e);
							}
							if (repository) {
								if (!Files.isSymbolicLink(s)) {
									Path t = checkFilesDir(repositoryPath.resolve(relPath));
									try {
										copy("Non-app bootstrap file", s, t, manifest.timestamp());
									} catch (IOException e) {
										throw new IllegalStateException(
												String.format("Failed to copy bootstrap file %s to %s.", s, t));
									}
								}
							}
						}
					}
				});
			}

			manifest.getProperties().put("maven.central", MAVEN_BASE);
			manifest.getProperties().put("main", mainClass);
			if (properties != null) {
				for (Map.Entry<String, String> en : properties.entrySet()) {
					manifest.getProperties().put(en.getKey(), en.getValue());
				}
			}

			if (launcherScriptName == null || launcherScriptName.equals("")) {
				launcherScriptName = project.getArtifactId();
			}

			if(StringUtils.isNotBlank(splash)) {
				Path splashPath = imagePath.resolve("splash." + getExtension(splash));
				Files.copy(Paths.get(splash), splashPath);
				manifest.entries().add(new Entry(splashPath, manifest).section(Section.BOOTSTRAP)
						.path(imagePath.relativize(splashPath))
						.uri(new URI(resolveUrl(normalizeForUri(remoteBase), imagePath.relativize(splashPath).toString())))
						.type(Type.OTHER));
				if (repository) {
					Path repositorySplashPath = repositoryPath.resolve("splash." + getExtension(splash));
					Files.copy(Paths.get(splash), repositorySplashPath);
				}
			}

			Path appCfgPath = imagePath.resolve("app.cfg");
			checkDir(imagePath.resolve("app.cfg.d"));
			writeAppCfg(manifest, appCfgPath, imagePath);
			manifest.entries().add(new Entry(appCfgPath, manifest).section(Section.BOOTSTRAP)
					.path(imagePath.relativize(appCfgPath))
					.uri(new URI(resolveUrl(normalizeForUri(remoteBase), imagePath.relativize(appCfgPath).toString())))
					.type(Type.OTHER));
			if (repository) {
				Path repositoryAppCfgPath = repositoryPath.resolve("app.cfg");
				writeAppCfg(manifest, repositoryAppCfgPath, imagePath);
			}
			
			String fullScriptName = launcherScriptName; 
			if(Platform.isWindows() && !fullScriptName.toLowerCase().endsWith(".bat")) {
				fullScriptName += ".bat";
			}

			Path scriptPath = checkDir(imagePath.resolve("bin")).resolve(fullScriptName);
			Path argsPath = useArgfile ? checkDir(imagePath).resolve(launcherScriptName + ".args") : null;
			writeScript(imagePath, scriptPath, argsPath, bootstrapPathObj, businessPathObj);
			if (updateableBootstrap) {
				argsPath = useArgfile ? checkDir(repositoryPath).resolve(launcherScriptName + ".args") : null;
				scriptPath = checkDir(repositoryPath.resolve("bin")).resolve(fullScriptName);
				writeScript(imagePath, scriptPath, argsPath, bootstrapPathObj, businessPathObj);
				manifest.entries()
						.add(new Entry(scriptPath, manifest).section(Section.BOOTSTRAP)
								.path(repositoryPath.relativize(scriptPath))
								.uri(new URI(resolveUrl(normalizeForUri(remoteBase),
										normalizeForUri(repositoryPath.relativize(scriptPath).toString()))))
								.type(Type.OTHER));
				if (useArgfile) {
					manifest.entries()
							.add(new Entry(argsPath, manifest).section(Section.BOOTSTRAP)
									.path(repositoryPath.relativize(argsPath))
									.uri(new URI(resolveUrl(normalizeForUri(remoteBase),
											repositoryPath.relativize(argsPath).toString())))
									.type(Type.OTHER));
				}
			}

			try (Writer out = Files.newBufferedWriter(checkDir(imagePath).resolve("manifest.xml"))) {
				manifest.save(out);
			}
			if (repository) {
				try (Writer out = Files.newBufferedWriter(checkDir(repositoryPath).resolve("manifest.xml"))) {
					manifest.save(out);
				}
			}
			
			switch(packageMode) {
			case SELF_EXTRACTING:
				SelfExtractingExecutableBuilder builder = new SelfExtractingExecutableBuilder();
				builder.image(imagePath);
				builder.output(Paths.get(packagePath));
				builder.log(getLog());
				builder.make();
				break;
			case NONE:
				break;
			default:
				throw new UnsupportedOperationException("Not yet implemented.");
			}
		} catch (IOException | URISyntaxException e) {
			throw new MojoExecutionException("Failed to write configuration.", e);
		}
	}

	protected boolean isForkerUpdaterBootstrap(org.eclipse.aether.artifact.Artifact a) {
		return a.getArtifactId().equals("forker-updater")
				|| (a.getArtifactId().startsWith("forker-updater-")
						&& !a.getArtifactId().equals("forker-updater-example"))
				|| a.getArtifactId().equals("forker-client") || a.getArtifactId().equals("forker-common")
				|| a.getArtifactId().equals("forker-wrapper")
				|| a.getArtifactId().equals("jna-platform") || a.getArtifactId().equals("jna")
				|| a.getArtifactId().equals("picocli");
	}

	protected boolean isForkerUpdaterRuntime(org.eclipse.aether.artifact.Artifact a) {
		if (includeForkerUpdaterRuntimeModules) {
			return a.getArtifactId().equals("forker-updater") || a.getArtifactId().equals("forker-common")
					|| a.getArtifactId().equals("forker-client") || a.getArtifactId().equals("forker-wrapped")
					|| a.getArtifactId().equals("jna-platform") || a.getArtifactId().equals("jna")
					|| a.getArtifactId().equals("picocli");
		} else {
			return false;
		}
	}

	protected boolean isBootstrap(org.eclipse.aether.artifact.Artifact a) {
		return isShared(a) || isForkerUpdaterBootstrap(a) || (!isShared(a) && containsArtifact(bootstrapArtifacts, a));
	}

	protected boolean isShared(org.eclipse.aether.artifact.Artifact a) {
		return containsArtifact(sharedArtifacts, a);
	}

	protected boolean isBusiness(org.eclipse.aether.artifact.Artifact a) {
		return (!isBootstrap(a) && !isShared(a)) || artifactEquals(a, project.getArtifact()) || isShared(a) || (isForkerUpdaterRuntime(a))
				|| (!isShared(a) && (containsArtifact(businessArtifacts, a)));
	}

	protected boolean artifactEquals(org.eclipse.aether.artifact.Artifact a, Artifact artifact) {
		return a.getArtifactId().equals(artifact.getArtifactId()) && a.getGroupId().equals(artifact.getGroupId());
	}

	protected void doArtifact(AppManifest manifest, Path bootstrapPathObj, Path businessPathObj,

			Artifact a) throws MojoExecutionException, IOException, URISyntaxException {
		String artifactId = a.getArtifactId();
		org.eclipse.aether.artifact.Artifact aetherArtifact = new DefaultArtifact(a.getGroupId(), a.getArtifactId(),
				a.getClassifier(), a.getType(), a.getVersion());

		ArtifactResult resolutionResult = resolveRemoteArtifact(new HashSet<MavenProject>(), project, aetherArtifact,
				this.repositories);
		if (resolutionResult == null)
			throw new MojoExecutionException("Artifact " + aetherArtifact.getGroupId() + ":"
					+ aetherArtifact.getArtifactId() + " could not be resolved.");

		aetherArtifact = resolutionResult.getArtifact();

		File file = aetherArtifact.getFile();
		if (file == null || !file.exists()) {
			getLog().warn("Artifact " + artifactId
					+ " has no attached file. Its content will not be copied in the target model directory.");
			return;
		}
		
		if(!"jar".equals(aetherArtifact.getExtension())) {
			getLog().info(String.format("Skipping %s, it is not a jar", artifactId));
			return;
		}

		boolean isBootstrap = isBootstrap(aetherArtifact);
		boolean isBusiness = isBusiness(aetherArtifact);
		boolean isModule = isModule(aetherArtifact);

		getLog().debug(String.format(
				"Adding artifact: %s   Module: %s, Bootstrap: %s, Business: %s Bootstrap Path: %s Bus. Path: %s",
				getFileName(a), isModule, isBootstrap, isBusiness, bootstrapPathObj, businessPathObj));

		if (isBootstrap) {
			/* Bootstrap */
			if (isModule) {
				Path dir = resolvePath(bootstrapPathObj, modulePath);
				module(manifest, dir, false, a, resolutionResult, file, Section.BOOTSTRAP);
			} else {
				Path dir = resolvePath(bootstrapPathObj, classPath);
				classpath(manifest, dir, false, a, resolutionResult, file, Section.BOOTSTRAP);
			}
		}

		if (isBusiness) {
			/* App */
			if (isModule) {
				Path dir = resolvePath(businessPathObj, modulePath);
				module(manifest, dir, true, a, resolutionResult, file, Section.APP);
			} else {
				Path dir = resolvePath(businessPathObj, classPath);
				classpath(manifest, dir, true, a, resolutionResult, file, Section.APP);
			}
		}

		return;
	}

	private Path resolvePath(Path pathObj, String path) {
		return path == null || path.equals("") ? pathObj : pathObj.resolve(path);
	}

	private boolean containsArtifact(Collection<String> artifactNames, org.eclipse.aether.artifact.Artifact artifact) {
		if (artifactNames == null)
			return false;
		String k = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getClassifier();
		if (artifactNames.contains(k))
			return true;
		k = artifact.getGroupId() + ":" + artifact.getArtifactId();
		if (artifactNames.contains(k))
			return true;
		k = artifact.getArtifactId();
		if (artifactNames.contains(k))
			return true;
		return false;
	}

	private void writeScript(Path imagePath, Path scriptPath, Path argsPath, Path bootstrapPath, Path businessPath)
			throws IOException {
		
		List<String> modulePaths = new ArrayList<>();
		List<String> vmopts = new ArrayList<>();
		List<String> classPaths = new ArrayList<>();
		StringBuilder updaterArgs = new StringBuilder();
		
		boolean useForkerModules = scriptArgs(imagePath, bootstrapPath, modulePaths, vmopts,
				classPaths, updaterArgs);
		
		if(Platform.isWindows()) {
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
	
				if (link) {
					out.println("SET JAVA_EXE=bin\\java.exe");
				} else {
					out.println(String.format("IF \"%JAVA_HOME%\"==\"\" ( "));
					out.println("    SET JAVA_EXE=${JAVA_HOME}\\bin\\java.exe");
					out.println(") ELSE (");
					out.println("    SET JAVA_EXE=java");
					out.println(")");
				}
	
				out.println(String.format("SET APP_ARGS=\"%s\"", updaterArgs.toString()));
				if (updateableBootstrap) {
					//out.println("while : ; do");
					if (useForkerModules)
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
					if(updateable) {
						if (useForkerModules)
							out.println(
									"%JAVA_EXE% %VM_OPTIONS% -m com.sshtools.forker.updater/com.sshtools.forker.updater.Updater %APP_ARGS% %*");
						else
							out.println("%JAVA_EXE% %VM_OPTIONS% com.sshtools.forker.updater.Updater %APP_ARGS% %*");
					}
					else {
						if (useForkerModules)
							out.println("%JAVA_EXE% %VM_OPTIONS% -m com.sshtools.forker.wrapper/com.sshtools.forker.wrapper.ForkerWrapper %APP_ARGS% %*");
						else
							out.println("%JAVA_EXE% %VM_OPTIONS% com.sshtools.forker.wrapper.ForkerWrapper %APP_ARGS% %*");
					}
				}
			}
		}
		else if(OS.isUnix()) {
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
	
				if (link) {
					out.println("JAVA_EXE=bin/java");
				} else {
					out.println(String.format("if [ -n \"${JAVA_HOME}\" ] ; then "));
					out.println("    JAVA_EXE=${JAVA_HOME}/bin/java");
					out.println("else");
					out.println("    JAVA_EXE=java");
					out.println("fi");
				}
	
				out.println(String.format("APP_ARGS=\"%s\"", updaterArgs.toString()));
				if (updateableBootstrap) {
					out.println("while : ; do");
					if (useForkerModules)
						out.println(
								"    ${JAVA_EXE} ${VM_OPTIONS} -m com.sshtools.forker.updater/com.sshtools.forker.updater.Updater ${APP_ARGS} $@");
					else
						out.println("    ${JAVA_EXE} ${VM_OPTIONS} com.sshtools.forker.updater.Updater ${APP_ARGS} $@");
					out.println("    ret=$?");
					out.println("    if [ \"${ret}\" != 9 -o ! -d .updates ]; then");
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
					if(updateable) {
						if (useForkerModules)
							out.println(
									"${JAVA_EXE} ${VM_OPTIONS} -m com.sshtools.forker.updater/com.sshtools.forker.updater.Updater ${APP_ARGS} $@");
						else
							out.println("${JAVA_EXE} ${VM_OPTIONS} com.sshtools.forker.updater.Updater ${APP_ARGS} $@");
					}
					else {
						if (useForkerModules)
							out.println("${JAVA_EXE} ${VM_OPTIONS} -m com.sshtools.forker.wrapper/com.sshtools.forker.wrapper.ForkerWrapper ${APP_ARGS} $@");
						else
							out.println("${JAVA_EXE} ${VM_OPTIONS} com.sshtools.forker.wrapper.ForkerWrapper ${APP_ARGS} $@");
					}
				}
			}
		}
		else
			throw new UnsupportedOperationException("Cannot create launch script for this platform.");
		scriptPath.toFile().setExecutable(true);
	}

	protected boolean scriptArgs(Path imagePath, Path bootstrapPath, List<String> modulePaths,
			List<String> vmopts, List<String> classPaths, StringBuilder updaterArgs) {

		boolean useForkerModules = false;
		Path localDir = resolvePath(imagePath.resolve(bootstrapPath), modulePath);
		Path dir = resolvePath(bootstrapPath, modulePath);
		if (Files.exists(localDir)) {
			for (File f : localDir.toFile().listFiles()) {
				modulePaths.add(dir + File.separator + f.getName());
				if (f.getName().startsWith("forker-updater")) {
					useForkerModules = true;
				}
			}
		}

		localDir = resolvePath(imagePath.resolve(bootstrapPath), classPath);
		dir = resolvePath(bootstrapPath, classPath);
		if (Files.exists(localDir)) {
			for (File f : localDir.toFile().listFiles()) {
				classPaths.add(dir + File.separator + f.getName());
			}
		}
		if(org.apache.commons.lang3.StringUtils.isNotBlank(splash)) {
			vmopts.add("-splash:splash." + getExtension(splash));
		}
		if (!modulePaths.isEmpty()) {
			vmopts.add("-p");
			vmopts.add(String.join(File.pathSeparator, modulePaths));
		}
		if (!classPaths.isEmpty()) {
			vmopts.add("-cp");
			vmopts.add(String.join(File.pathSeparator, classPaths));
		}

		if (systemModules != null && systemModules.size() > 0) {
			vmopts.add("--add-modules");
			vmopts.add(String.join(",", systemModules));
		}
		vmopts.add("-Dforker.remoteManifest=" + normalizeForUri(remoteBase));
		if (vmArgs != null) {
			for (String vmArg : vmArgs) {
				vmopts.add(vmArg);
			}
		}
		
		updaterArgs.append("--configuration=");
		updaterArgs.append("app.cfg");
		if (this.updaterArgs != null) {
			for (String arg : this.updaterArgs) {
				updaterArgs.append(" ");
				updaterArgs.append(escapeSpaces(arg));
			}
		}
		
		return useForkerModules;
	}

	private String getExtension(String fileName) {
		int idx = fileName.indexOf('.');
		if(idx == -1)
			throw new IllegalArgumentException(String.format("Filename %s must have extension.", fileName));
		return fileName.substring(idx + 1);
	}

	private void writeAppCfg(AppManifest manifest, Path appCfgPath, Path imagePath) throws IOException {
		try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(appCfgPath), true)) {
			out.println("configuration-directory app.cfg.d");
			out.println("local-manifest manifest.xml");
			out.println("default-remote-manifest " + normalizeForUri(remoteBase) + "/manifest.xml");
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
			if (installLocation != null && !installLocation.equals(""))
				out.println("install-location " + installLocation);
			if (forkerArgs != null) {
				for (String forkerArg : forkerArgs)
					out.println(forkerArg.trim());
			}
		}
	}

	private Path checkFilesDir(Path resolve) {
		checkDir(resolve.getParent());
		return resolve;
	}

	private Path checkDir(Path resolve) {
		if (!Files.exists(resolve)) {
			try {
				Files.createDirectories(resolve);
			} catch (IOException e) {
				throw new IllegalStateException(String.format("Failed to create %s.", resolve));
			}
		}
		return resolve;
	}

	private String escapeSpaces(String str) {
		return str.replace(" ", "\\\\ ");
	}

	private void classpath(AppManifest manifest, Path path, boolean business, Artifact a,
			ArtifactResult resolutionResult, File file, Section section) throws IOException, URISyntaxException {
		String finalClassPath = path.resolve(getFileName(a)).toString();
		getLog().info(String.format("Adding %s classpath jar %s to updater config.", a.getFile(), finalClassPath));
		if (image)
			copy("Image classpath jar", a.getFile().toPath(), imageDirectory.toPath().resolve(finalClassPath), manifest.timestamp());
		String remoteUrl = mavenUrl(resolutionResult);
		Entry entry;
		Path entryPath = Paths.get(getFileName(a));
		if (repository) {
			if (remotesFromOriginalSource) {
				if (!isRemote(remoteUrl) && !Files.isSymbolicLink(file.toPath())) {
					copy("Classpath jar from Maven", a.getFile().toPath(), repositoryDirectory.toPath().resolve(finalClassPath),
							manifest.timestamp());
				}
				if (remoteUrl == null)
					remoteUrl = repositoryUrl(resolutionResult, path);
				entry = new Entry(file.toPath(), manifest).section(section).name(entryPath).uri(new URI(remoteUrl))
						.type(Type.CLASSPATH);
			} else {
				entry = new Entry(file.toPath(), manifest).section(section).name(entryPath)
						.uri(new URI(repositoryUrl(resolutionResult, path))).type(Type.CLASSPATH);
				if (!Files.isSymbolicLink(file.toPath()))
					copy("Classpath jar from Local", file.toPath(), repositoryDirectory.toPath().resolve(finalClassPath), manifest.timestamp());
			}
		} else {
			entry = new Entry(file.toPath(), manifest).section(section).name(entryPath).uri(new URI(remoteUrl))
					.type(Type.CLASSPATH);
		}
		setArchitectures(a, entry);
		setOS(a, entry);
		manifest.entries().add(entry);
	}

	private void module(AppManifest manifest, Path path, boolean business, Artifact a, ArtifactResult resolutionResult,
			File file, Section section) throws IOException, URISyntaxException {
		String finalModulePath = path.resolve(getFileName(a)).toString();
		getLog().info(String.format("Adding %s module jar %s to updater config.", a.getFile(), finalModulePath));
		if (image)
			copy("Modulepath image jar", file.toPath(), imageDirectory.toPath().resolve(finalModulePath), manifest.timestamp());
		String remoteUrl = mavenUrl(resolutionResult);
		Entry entry;
		Path entryPath =Paths.get(getFileName(a));
		if (repository) {
			if (remotesFromOriginalSource) {
				if (!isRemote(remoteUrl)) {
					copy("Modulepath jar from Maven", file.toPath(), repositoryDirectory.toPath().resolve(finalModulePath), manifest.timestamp());
				}
				if (remoteUrl == null)
					remoteUrl = repositoryUrl(resolutionResult, path);
				entry = new Entry(file.toPath(), manifest).section(section).name(entryPath).uri(new URI(remoteUrl))
						.type(Type.MODULEPATH);
			} else {
				entry = new Entry(file.toPath(), manifest).section(section).name(entryPath)
						.uri(new URI(repositoryUrl(resolutionResult, path))).type(Type.MODULEPATH);
				copy("Modulepath jar from Local", a.getFile().toPath(), repositoryDirectory.toPath().resolve(finalModulePath), manifest.timestamp());
			}
		} else {
			entry = new Entry(file.toPath(), manifest).section(section).name(entryPath).uri(new URI(remoteUrl))
					.type(Type.MODULEPATH);
		}

		setArchitectures(a, entry);
		setOS(a, entry);

		manifest.entries().add(entry);
	}

	private void setArchitectures(Artifact a, Entry entry) {
		for (Map.Entry<String, String> en : artifactArch.entrySet()) {
			if (Entry.toSet(en.getValue()).contains(a.getArtifactId())) {
				entry.architecture().add(en.getKey());
			}
		}
	}

	private void setOS(Artifact a, Entry entry) {
		for (Map.Entry<String, String> en : artifactOS.entrySet()) {
			if (Entry.toSet(en.getValue()).contains(a.getArtifactId())) {
				entry.os().add(en.getKey());
			}
		}
	}

	private ArtifactResult resolveRemoteArtifact(Set<MavenProject> visitedProjects, MavenProject project,
			org.eclipse.aether.artifact.Artifact aetherArtifact, List<RemoteRepository> repos)
			throws MojoExecutionException {
		ArtifactRequest req = new ArtifactRequest().setRepositories(repos).setArtifact(aetherArtifact);
		ArtifactResult resolutionResult = null;
		visitedProjects.add(project);
		try {
			resolutionResult = this.repoSystem.resolveArtifact(this.repoSession, req);

		} catch (ArtifactResolutionException e) {
			if (project.getParent() == null) {
				/* Reached the root (reactor), now look in child module repositories too */
				for (MavenProject p : session.getAllProjects()) {
					if (!visitedProjects.contains(p)) {
						try {
							resolutionResult = resolveRemoteArtifact(visitedProjects, p, aetherArtifact,
									p.getRemoteProjectRepositories());
							if (resolutionResult != null)
								break;
						} catch (MojoExecutionException mee) {
						}
					}
				}
			} else if (!visitedProjects.contains(project.getParent()))
				return resolveRemoteArtifact(visitedProjects, project.getParent(), aetherArtifact,
						project.getParent().getRemoteProjectRepositories());
		}
		return resolutionResult;
	}

	protected String getJLinkExecutable() throws IOException {
		return this.getToolExecutable("jlink");
	}

	protected String getToolExecutable(final String toolName) throws IOException {
		final Toolchain tc = this.getToolchain();

		String toolExecutable = null;
		if (tc != null) {
			toolExecutable = tc.findTool(toolName);
		}

		// TODO: Check if there exist a more elegant way?
		final String toolCommand = toolName + (Platform.isWindows() ? ".exe" : "");

		File toolExe;

		if (StringUtils.isNotEmpty(toolExecutable)) {
			toolExe = new File(toolExecutable);

			if (toolExe.isDirectory()) {
				toolExe = new File(toolExe, toolCommand);
			}

			if (Platform.isWindows() && toolExe.getName().indexOf('.') < 0) {
				toolExe = new File(toolExe.getPath() + ".exe");
			}

			if (!toolExe.isFile()) {
				throw new IOException(
						"The " + toolName + " executable '" + toolExe + "' doesn't exist or is not a file.");
			}
			return toolExe.getAbsolutePath();
		}

		// ----------------------------------------------------------------------
		// Try to find tool from System.getProperty( "java.home" )
		// By default, System.getProperty( "java.home" ) = JRE_HOME and JRE_HOME
		// should be in the JDK_HOME
		// ----------------------------------------------------------------------
		toolExe = new File(SystemUtils.getJavaHome() + File.separator + ".." + File.separator + "bin", toolCommand);

		if (!toolExe.exists() || !toolExe.isFile()) {
			toolExe = new File(SystemUtils.getJavaHome() + File.separator + "bin", toolCommand);
			if (!toolExe.exists() || !toolExe.isFile()) {
				// ----------------------------------------------------------------------
				// Try to find javadocExe from JAVA_HOME environment variable
				// ----------------------------------------------------------------------
				final Properties env = CommandLineUtils.getSystemEnvVars();
				final String javaHome = env.getProperty("JAVA_HOME");
				if (StringUtils.isEmpty(javaHome)) {
					throw new IOException("The environment variable JAVA_HOME is not correctly set."
							+ SystemUtils.getJavaHome() + " : " + toolExe);
				}
				if (!new File(javaHome).getCanonicalFile().exists() || new File(javaHome).getCanonicalFile().isFile()) {
					throw new IOException("The environment variable JAVA_HOME=" + javaHome
							+ " doesn't exist or is not a valid directory.");
				}

				toolExe = new File(javaHome + File.separator + "bin", toolCommand);
			}
		}
		if (!toolExe.getCanonicalFile().exists() || !toolExe.getCanonicalFile().isFile()) {
			throw new IOException("The " + toolName + " executable '" + toolExe
					+ "' doesn't exist or is not a file. Verify the JAVA_HOME environment variable.");
		}

		return toolExe.getAbsolutePath();
	}

	public Toolchain getToolchain() {
		Toolchain tc = null;

		if (this.jdkToolchain != null) {
			// Maven 3.3.1 has plugin execution scoped Toolchain Support
			try {
				final Method getToolchainsMethod = this.toolchainManager.getClass().getMethod("getToolchains",
						MavenSession.class, String.class, Map.class);

				@SuppressWarnings("unchecked")
				final List<Toolchain> tcs = (List<Toolchain>) getToolchainsMethod.invoke(this.toolchainManager,
						this.session, "jdk", this.jdkToolchain);

				if (tcs != null && tcs.size() > 0) {
					tc = tcs.get(0);
				}
			} catch (final ReflectiveOperationException e) {
				// ignore
			} catch (final SecurityException e) {
				// ignore
			} catch (final IllegalArgumentException e) {
				// ignore
			}
		}

		if (tc == null) {
			// TODO: Check if we should make the type configurable?
			tc = this.toolchainManager.getToolchainFromBuildContext("jdk", this.session);
		}

		return tc;
	}

	private void copy(String reason, Path p1, Path p2, Instant mod) throws IOException {
		getLog().info(String.format("Copy %s - %s to %s", reason, p1.toAbsolutePath(), p2.toAbsolutePath()));
		Files.createDirectories(p2.getParent());
		if (Files.isSymbolicLink(p1)) {
			Files.createSymbolicLink(p2, Files.readSymbolicLink(p1));
		} else {
			try (OutputStream out = Files.newOutputStream(p2)) {
				Files.copy(p1, out);
			}
		}
		Files.setLastModifiedTime(p2, FileTime.from(mod));
	}

	private String getFileName(Artifact a) {
		return getFileName(a.getArtifactId(), a.getVersion(), a.getClassifier(), a.getType());
	}

	private String getFileName(org.eclipse.aether.artifact.Artifact a) {
		return getFileName(a.getArtifactId(), a.getVersion(), a.getClassifier(), a.getExtension());
	}

	private String getFileName(String artifactId, String version, String classifier, String type) {
		StringBuilder fn = new StringBuilder();
		fn.append(artifactId);
		if (includeVersion) {
			fn.append("-");
			fn.append(version);
		}
		if (classifier != null && classifier.length() > 0) {
			fn.append("-");
			fn.append(classifier);
		}
		fn.append(".");
		fn.append(type);
		return fn.toString();
	}

	private boolean isModule(org.eclipse.aether.artifact.Artifact a) throws IOException {
		if (!modules)
			return false;

		if (automaticArtifacts != null && containsArtifact(new LinkedHashSet<>(automaticArtifacts), a))
			return true;

		if (classpathArtifacts != null && containsArtifact(new LinkedHashSet<>(classpathArtifacts), a))
			return false;

		/* Detect */
		return isModuleJar(a);
	}

	private boolean isModuleJar(org.eclipse.aether.artifact.Artifact a) throws IOException {
		if (a.getFile() == null) {
			getLog().warn(String.format("%s has a null file?", a));
		} else {
			if ("jar".equals(a.getExtension())) {
				try (JarFile jarFile = new JarFile(a.getFile())) {
					Enumeration<JarEntry> enumOfJar = jarFile.entries();
					Manifest mf = jarFile.getManifest();
					if (mf != null) {
						if (mf.getMainAttributes().getValue("Automatic-Module-Name") != null)
							return true;
					}
					while (enumOfJar.hasMoreElements()) {
						JarEntry entry = enumOfJar.nextElement();
						if (entry.getName().equals("module-info.class")
								|| entry.getName().matches("META-INF/versions/.*/module-info.class")) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private String mavenUrl(String base, String groupId, String artifactId, String version, String classifier) {
		StringBuilder builder = new StringBuilder();
		builder.append(base + '/');
		builder.append(groupId.replace('.', '/') + "/");
		builder.append(artifactId.replace('.', '-') + "/");
		builder.append(version + "/");
		builder.append(artifactId.replace('.', '-') + "-" + version);

		if (classifier != null && classifier.length() > 0) {
			builder.append('-' + classifier);
		}

		builder.append(".jar");

		return builder.toString();
	}

	private String mavenUrl(ArtifactResult result) {
		org.eclipse.aether.repository.ArtifactRepository repo = result.getRepository();
		MavenProject project = this.project;
		while (project != null) {
			List<MavenProject> collectedProjects = project.getCollectedProjects();
			if (collectedProjects != null) {
				for (MavenProject p : collectedProjects) {
					for (RemoteRepository r : p.getRemoteProjectRepositories()) {
						if (r.getId().equals(repo.getId())) {
							String url = repo == null ? resolveUrl(normalizeForUri(remoteBase), remoteJars)
									: r.getUrl();
							return mavenUrl(url, result.getArtifact().getGroupId(),
									result.getArtifact().getArtifactId(), result.getArtifact().getVersion(),
									result.getArtifact().getClassifier());
						}
					}
				}
			}
			project = project.getParent();
		}
		return null;
	}

	private String normalizeForUri(String localPath) {
		return localPath.replace(File.separator, "/");
	}

	private String repositoryUrl(String path, Path sectionPath) {
		return resolveUrl(resolveUrl(normalizeForUri(remoteBase), normalizeForUri(sectionPath.toString())),
				normalizeForUri(path));
	}

	private String repositoryUrl(ArtifactResult result, Path sectionPath) {
		return repositoryUrl(getFileName(result.getArtifact()), sectionPath);
	}

	private boolean isRemote(String path) {
		return path != null && (path.startsWith("http:") || path.startsWith("https:"));
	}

	private boolean isUrl(String path) {
		return isRemote(path) || (path != null && path.startsWith("file:"));
	}

	private String resolveUrl(String base, String path) {
		if (isUrl(path))
			return path;
		else if (path != null && path.startsWith("/")) {
			int idx = base.indexOf('/');
			return idx == -1 ? base + path : base.substring(0, idx) + path;
		} else if (path != null) {
			return base.endsWith("/") ? base + path : base + "/" + path;
		} else {
			return base;
		}
	}

}
