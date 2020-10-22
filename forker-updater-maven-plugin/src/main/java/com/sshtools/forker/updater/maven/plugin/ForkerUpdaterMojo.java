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
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

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
import org.apache.maven.toolchain.java.DefaultJavaToolChain;
import org.codehaus.plexus.languages.java.jpms.LocationManager;
import org.codehaus.plexus.languages.java.jpms.ResolvePathsRequest;
import org.codehaus.plexus.languages.java.jpms.ResolvePathsResult;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import com.sshtools.forker.common.Util;
import com.sshtools.forker.updater.AppManifest;
import com.sshtools.forker.updater.AppManifest.Entry;
import com.sshtools.forker.updater.AppManifest.Section;
import com.sshtools.forker.updater.AppManifest.Type;

@Mojo(name = "updates", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class ForkerUpdaterMojo extends AbstractMojo {
	private static final String MAVEN_BASE = "https://repo1.maven.org/maven2";

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
	@Parameter(defaultValue = "app/business", property = "apppath", required = true)
	private String appPath;

	/**
	 * Location of the classpath jars.
	 */
	@Parameter(defaultValue = "app/bootstrap/classpath", property = "classpath", required = true)
	private String classPath;

	/**
	 * Location of the modulepath jars.
	 */
	@Parameter(defaultValue = "app/bootstrap/modulepath", property = "modulepath", required = true)
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
	private List<String> classpathJars;
	/**
	 * List of automatic modules
	 */
	@Parameter
	private List<String> automaticModules;
	/**
	 * List of bootstrap modules
	 */
	@Parameter
	private List<String> bootstrapModules;
	/**
	 * List of bootstrap class path
	 */
	@Parameter
	private List<String> bootstrapClasspath;
	/**
	 * List of bootstrap only modules
	 */
	@Parameter
	private List<String> bootstrapOnlyModules;
	/**
	 * List of bootstrap only class path
	 */
	@Parameter
	private List<String> bootstrapOnlyClasspath;

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

	@Parameter(defaultValue = "false", property = "forkerDaemon")
	private boolean forkerDaemon;

	@Parameter(defaultValue = "2", property = "compress")
	private int compress;

	@Parameter(defaultValue = "false")
	private boolean includeVersion;

	@Parameter(property = "mainClass", required = true)
	private String mainClass;

	@Parameter(property = "launcherScriptName", required = true)
	private String launcherScriptName;

	@Parameter(defaultValue = "true", property = "repository")
	private boolean repository;

	@Parameter(defaultValue = "true", property = "image")
	private boolean image;

	@Parameter(defaultValue = "true", property = "link")
	private boolean link;

	@Parameter(defaultValue = "${project.artifactId}", property = "id", required = true)
	private String id;

	@Parameter(defaultValue = "false", property = "remotesFromOriginalSource")
	private boolean remotesFromOriginalSource;

	@Parameter(defaultValue = "true", property = "includeForkerUpdaterRuntimeModules")
	private boolean includeForkerUpdaterRuntimeModules = true;

	@Parameter(defaultValue = "${project.version}-${timestamp}", property = "version")
	private String version;

	@Parameter(defaultValue = "true", property = "removeImageBeforeLink")
	private boolean removeImageBeforeLink = true;

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
			manifest.baseUri(new URI(remoteBase));
		} catch (URISyntaxException e1) {
			throw new MojoExecutionException("Invalid remote base.", e1);
		}
		if (!version.equals(""))
			manifest.version(version.replace("${timestamp}", String.valueOf(
					new SimpleDateFormat("yyyyMMddHHmmss").format(new Date(manifest.timestamp().toEpochMilli())))));
		Path appPathObj = Paths.get(appPath);
		manifest.path(appPathObj);

		Path imagePath = imageDirectory.toPath();
		Path classPathObj = imagePath.resolve(classPath);
		Path modulePathObj = imagePath.resolve(modulePath);
		Path relModulePath = imagePath.relativize(modulePathObj);
		Path relClassPath = imagePath.relativize(classPathObj);
		Path repositoryPath = repositoryDirectory.toPath();

		List<String> allBootstrapModules = new ArrayList<>();
		List<String> allBootstrapOnlyModules = new ArrayList<>();
		if (includeForkerUpdaterRuntimeModules) {
			allBootstrapModules.addAll(Arrays.asList("forker-common", "forker-client", "forker-updater", "commons-cli",
					"jna", "jna-platform", "commons-lang3", "commons-io", "forker-wrapped"));
			allBootstrapOnlyModules.addAll(Arrays.asList("forker-wrapper", "forker-daemon"));
		}
		if (bootstrapModules != null)
			allBootstrapModules.addAll(bootstrapModules);
		if (bootstrapOnlyModules != null)
			allBootstrapOnlyModules.addAll(bootstrapOnlyModules);

		try {
			
			if(link) {
				if(removeImageBeforeLink) {
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
				String artifactId = a.getArtifactId();
				org.eclipse.aether.artifact.Artifact aetherArtifact = new DefaultArtifact(a.getGroupId(),
						a.getArtifactId(), a.getClassifier(), a.getType(), a.getVersion());

				ArtifactResult resolutionResult = resolveRemoteArtifact(new HashSet<MavenProject>(), project,
						aetherArtifact, this.repositories);
				if (resolutionResult == null)
					throw new MojoExecutionException("Artifact " + aetherArtifact.getGroupId() + ":"
							+ aetherArtifact.getArtifactId() + " could not be resolved.");

				aetherArtifact = resolutionResult.getArtifact();

				File file = aetherArtifact.getFile();
				if (file == null || !file.exists()) {
					getLog().warn("Artifact " + artifactId
							+ " has no attached file. Its content will not be copied in the target model directory.");
					continue;
				}

				String artifactName = getArtifactName(a);

				if ((bootstrapClasspath != null && bootstrapClasspath.contains(artifactName))
						|| (bootstrapOnlyClasspath != null && bootstrapOnlyClasspath.contains(artifactName))) {
					copy(a.getFile().toPath(), checkDir(classPathObj).resolve(getFileName(a)), manifest.timestamp());
				} else if ((allBootstrapModules.contains(artifactName))
						|| (allBootstrapOnlyModules.contains(artifactName))) {
					copy(a.getFile().toPath(), checkDir(modulePathObj).resolve(getFileName(a)), manifest.timestamp());
				}

				if ((bootstrapOnlyClasspath == null || !bootstrapOnlyClasspath.contains(artifactName))
						&& (!allBootstrapOnlyModules.contains(artifactName))) {
					if (classpathJars != null && classpathJars.contains(artifactName))
						classpath(manifest, a, resolutionResult, file);
					else if (isModule(a) || (automaticModules != null && automaticModules.contains(artifactName))) {
						module(manifest, a, resolutionResult, file);
					} else {
						classpath(manifest, a, resolutionResult, file);
					}
				}
			}

			if (bootstrapFiles != null) {
				for (BootstrapFile file : bootstrapFiles) {
					Path path = Paths.get(file.source);
					Path target = imagePath
							.resolve(file.target.equals(".") ? imagePath : imagePath.resolve(file.target))
							.resolve(path.getFileName());
					copy(path, target, manifest.timestamp());
					if (updateableBootstrap) {
						manifest.entries()
								.add(new Entry(target).section(Section.BOOTSTRAP).path(imagePath.relativize(target))
										.uri(new URI(resolveUrl(remoteBase, imagePath.relativize(target).toString())))
										.type(Type.OTHER));
						target = repositoryPath
								.resolve(file.target.equals(".") ? repositoryPath : repositoryPath.resolve(file.target))
								.resolve(path.getFileName());
						copy(path, target, manifest.timestamp());
					}
				}
			}

			if (appFiles != null) {
				for (AppFile file : appFiles) {
					Path path = Paths.get(file.source);
					Path target = imagePath
							.resolve(file.target.equals(".") ? appPathObj : appPathObj.resolve(file.target))
							.resolve(path.getFileName());
					Path relTarget = imagePath.relativize(target);
					copy(path, target, manifest.timestamp());
					manifest.entries().add(new Entry(target).section(Section.APP).path(relTarget)
							.uri(new URI(resolveUrl(remoteBase, relTarget.toString()))).type(Type.OTHER));
				}
			}

			/* Anything thats not in the app base is a boostrap entry */
			Path mf = imagePath.resolve("manifest.xml");
			if (updateableBootstrap) {
				Files.walk(imagePath).forEach(s -> {
					if (Files.isRegularFile(s) && !mf.equals(s)) {
						Path relPath = imagePath.relativize(s);
						if (!relPath.startsWith(relModulePath) && !relPath.startsWith(relClassPath)
								&& !manifest.hasPath(relPath)) {
							try {
								manifest.entries().add(new Entry(s).section(Section.BOOTSTRAP).type(Type.OTHER)
										.path(relPath).uri(new URI(resolveUrl(remoteBase, relPath.toString()))));
							} catch (IOException | URISyntaxException e) {
								throw new IllegalStateException("Failed to construct bootstrap manifest entry.", e);
							}
							if (repository) {
								if (!Files.isSymbolicLink(s)) {
									Path t = checkFilesDir(repositoryPath.resolve(relPath));
									try {
										copy(s, t, manifest.timestamp());
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

			Path appCfgPath = imagePath.resolve("app.cfg");
			checkDir(imagePath.resolve("app.cfg.d"));
			writeAppCfg(manifest, appCfgPath, imagePath);
			manifest.entries()
					.add(new Entry(appCfgPath).section(Section.BOOTSTRAP).path(imagePath.relativize(appCfgPath))
							.uri(new URI(resolveUrl(remoteBase, imagePath.relativize(appCfgPath).toString())))
							.type(Type.OTHER));
			if (repository) {
				Path repositoryAppCfgPath = repositoryPath.resolve("app.cfg");
				writeAppCfg(manifest, repositoryAppCfgPath, imagePath);
			}

			Path scriptPath = checkDir(imagePath.resolve("bin")).resolve(launcherScriptName);
			writeScript(imagePath, classPathObj, modulePathObj, scriptPath);
			if (updateableBootstrap) {
				writeScript(imagePath, classPathObj, modulePathObj,
						checkDir(repositoryPath.resolve("bin")).resolve(launcherScriptName));
			}

			if (updateableBootstrap) {
				manifest.entries()
						.add(new Entry(scriptPath).section(Section.BOOTSTRAP).path(imagePath.relativize(scriptPath))
								.uri(new URI(resolveUrl(remoteBase, imagePath.relativize(scriptPath).toString())))
								.type(Type.OTHER));
			}

			try (Writer out = Files.newBufferedWriter(checkDir(imagePath).resolve("manifest.xml"))) {
				manifest.save(out);
			}
			if (repository) {
				try (Writer out = Files.newBufferedWriter(checkDir(repositoryPath).resolve("manifest.xml"))) {
					manifest.save(out);
				}
			}
		} catch (IOException | URISyntaxException e) {
			throw new MojoExecutionException("Failed to write configuration.", e);
		}
	}

	private void writeScript(Path imagePath, Path classPathObj, Path modulePathObj, Path scriptPath)
			throws IOException {
		boolean useForkerModules = false;
		try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(scriptPath), true)) {
			out.println("#!/bin/sh");
			out.println("realpath=$(readlink \"$0\")");
			out.println("if [ -z \"${realpath}\" ] ; then realpath=\"$0\" ; fi");
			out.println("cd $(dirname ${realpath})/..");

			StringBuilder vmopts = new StringBuilder();
			List<String> modulePaths = new ArrayList<>();
			Path dir = modulePathObj;
			if (Files.exists(dir)) {
				for (File f : dir.toFile().listFiles()) {
					modulePaths.add(modulePath + "/" + f.getName());
					if (f.getName().startsWith("forker-updater")) {
						useForkerModules = true;
					}
				}
			}
			dir = classPathObj;
			List<String> classPaths = new ArrayList<>();
			if (Files.exists(dir)) {
				for (File f : dir.toFile().listFiles()) {
					classPaths.add(classPath + "/" + f.getName());
				}
			}
			if (!modulePaths.isEmpty()) {
				vmopts.append("-p ");
				vmopts.append(String.join(":", modulePaths));
				vmopts.append(" ");
			}
			if (!classPaths.isEmpty()) {
				vmopts.append("-cp ");
				vmopts.append(String.join(":", classPaths));
				vmopts.append(" ");
			}

			if (systemModules != null && systemModules.size() > 0)
				vmopts.append(" --add-modules " + String.join(",", systemModules));
			vmopts.append(" -Dforker.remoteManifest=" + remoteBase);
			if (vmArgs != null) {
				for (String vmArg : vmArgs) {
					vmopts.append(" " + vmArg);
				}
			}
			out.println(String.format("VM_OPTIONS=\"%s\"", vmopts.toString()));

			StringBuilder updaterArgs = new StringBuilder();
			updaterArgs.append("--configuration ");
			updaterArgs.append("app.cfg");
			if (this.updaterArgs != null) {
				for (String arg : this.updaterArgs) {
					updaterArgs.append(" ");
					updaterArgs.append(escapeSpaces(arg));
				}
			}
			
			if(link) {
				out.println("JAVA_EXE=bin/java");
			}
			else {
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
				out.println("            echo \"$0: Failed to copy update files.\" >&2");
				out.println("            exit 2");
				out.println("        fi");
				out.println("        cd ..");
				out.println("        rm -fr .updates");
				out.println("    fi");
				out.println("done");
			} else {
				if (useForkerModules)
					out.println(
							"${JAVA_EXE} ${VM_OPTIONS} -m com.sshtools.forker.updater/com.sshtools.forker.updater.Updater ${APP_ARGS} $@");
				else
					out.println("${JAVA_EXE} ${VM_OPTIONS} com.sshtools.forker.updater.Updater ${APP_ARGS} $@");
			}
		}
		scriptPath.toFile().setExecutable(true);
	}

	private void writeAppCfg(AppManifest manifest, Path appCfgPath, Path imagePath) throws IOException {
		try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(appCfgPath), true)) {
			out.println("configuration-directory app.cfg.d");
			out.println("local-manifest manifest.xml");
			out.println("default-remote-manifest " + remoteBase + "/manifest.xml");
			List<String> cp = new ArrayList<>();
			List<String> mp = new ArrayList<>();
			for (Entry entry : manifest.entries(Section.APP)) {
				switch (entry.type()) {
				case CLASSPATH:
					cp.add(entry.resolve(manifest.path()).toString());
					break;
				case MODULEPATH:
					mp.add(entry.resolve(manifest.path()).toString());
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
			if (!forkerDaemon)
				out.println("no-forker-daemon");
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

	private String getArtifactName(Artifact a) {
		return a.getArtifactId() + (a.hasClassifier() ? "-" + a.getClassifier() : "");
	}

	private void classpath(AppManifest manifest, Artifact a, ArtifactResult resolutionResult, File file)
			throws IOException, URISyntaxException {
		String finalClassPath = appPath + "/" + getFileName(a);
		getLog().info(String.format("Adding %s classpath jar %s to update4j config.", a.getFile(), finalClassPath));
		if (image)
			copy(a.getFile().toPath(), imageDirectory.toPath().resolve(finalClassPath), manifest.timestamp());
		String remoteUrl = mavenUrl(resolutionResult);
		if (repository) {
			if (remotesFromOriginalSource) {
				if (!isRemote(remoteUrl) && !Files.isSymbolicLink(file.toPath())) {
					copy(a.getFile().toPath(), repositoryDirectory.toPath().resolve(finalClassPath),
							manifest.timestamp());
				}
				if (remoteUrl == null)
					remoteUrl = repositoryUrl(resolutionResult);
				manifest.entries().add(new Entry(file.toPath()).section(Section.APP).path(Paths.get(getFileName(a)))
						.uri(new URI(remoteUrl)).type(Type.CLASSPATH));
			} else {
				manifest.entries().add(new Entry(file.toPath()).section(Section.APP).path(Paths.get(getFileName(a)))
						.uri(new URI(repositoryUrl(resolutionResult))).type(Type.CLASSPATH));
				if (!Files.isSymbolicLink(file.toPath()))
					copy(file.toPath(), repositoryDirectory.toPath().resolve(finalClassPath),
							manifest.timestamp());
			}
		} else
			manifest.entries().add(new Entry(file.toPath()).section(Section.APP).path(Paths.get(getFileName(a)))
					.uri(new URI(remoteUrl)).type(Type.CLASSPATH));
	}

	private void module(AppManifest manifest, Artifact a, ArtifactResult resolutionResult, File file)
			throws IOException, URISyntaxException {
		String finalModulePath = appPath + "/" + getFileName(a);
		getLog().info(String.format("Adding %s module jar %s to updater config.", a.getFile(), finalModulePath));
		if (image)
			copy(file.toPath(), imageDirectory.toPath().resolve(finalModulePath), manifest.timestamp());
		String remoteUrl = mavenUrl(resolutionResult);
		if (repository) {
			if (remotesFromOriginalSource) {
				if (!isRemote(remoteUrl)) {
					copy(file.toPath(), repositoryDirectory.toPath().resolve(finalModulePath),
							manifest.timestamp());
				}
				if (remoteUrl == null)
					remoteUrl = repositoryUrl(resolutionResult);
				manifest.entries().add(new Entry(file.toPath()).section(Section.APP).path(Paths.get(getFileName(a)))
						.uri(new URI(remoteUrl)).type(Type.MODULEPATH));
			} else {
				manifest.entries().add(new Entry(file.toPath()).section(Section.APP).path(Paths.get(getFileName(a)))
						.uri(new URI(repositoryUrl(resolutionResult))).type(Type.MODULEPATH));
				copy(a.getFile().toPath(), repositoryDirectory.toPath().resolve(finalModulePath), manifest.timestamp());
			}
		} else {
			manifest.entries().add(new Entry(file.toPath()).section(Section.APP).path(Paths.get(getFileName(a)))
					.uri(new URI(remoteUrl)).type(Type.MODULEPATH));
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
		final String toolCommand = toolName + (SystemUtils.IS_OS_WINDOWS ? ".exe" : "");

		File toolExe;

		if (StringUtils.isNotEmpty(toolExecutable)) {
			toolExe = new File(toolExecutable);

			if (toolExe.isDirectory()) {
				toolExe = new File(toolExe, toolCommand);
			}

			if (SystemUtils.IS_OS_WINDOWS && toolExe.getName().indexOf('.') < 0) {
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

		// ----------------------------------------------------------------------
		// Try to find javadocExe from JAVA_HOME environment variable
		// ----------------------------------------------------------------------
		if (!toolExe.exists() || !toolExe.isFile()) {
			final Properties env = CommandLineUtils.getSystemEnvVars();
			final String javaHome = env.getProperty("JAVA_HOME");
			if (StringUtils.isEmpty(javaHome)) {
				throw new IOException("The environment variable JAVA_HOME is not correctly set.");
			}
			if (!new File(javaHome).getCanonicalFile().exists() || new File(javaHome).getCanonicalFile().isFile()) {
				throw new IOException("The environment variable JAVA_HOME=" + javaHome
						+ " doesn't exist or is not a valid directory.");
			}

			toolExe = new File(javaHome + File.separator + "bin", toolCommand);
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

	private void copy(Path p1, Path p2, Instant mod) throws IOException {
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

	private boolean isForkerUpdater(Artifact a) throws IOException {
		return a.getArtifactId().equals("forker-updater");
	}

	private boolean isModule(Artifact a) throws IOException {

		final ResolvePathsRequest<File> request = ResolvePathsRequest.ofFiles(a.getFile());

		final Toolchain toolchain = this.getToolchain();
		if ((toolchain != null) && (toolchain instanceof DefaultJavaToolChain)) {
			request.setJdkHome(new File(((DefaultJavaToolChain) toolchain).getJavaHome()));
		}

		final ResolvePathsResult<File> resolvePathsResult = this.locationManager.resolvePaths(request);

		try (JarFile jarFile = new JarFile(a.getFile())) {
			Enumeration<JarEntry> enumOfJar = jarFile.entries();
			Manifest mf = jarFile.getManifest();
			if (mf != null) {
				if (mf.getMainAttributes().getValue("Automatic-Module-Name") != null)
					return true;
			}
			while (enumOfJar.hasMoreElements()) {
				JarEntry entry = enumOfJar.nextElement();
				if (entry.getName().equals("module-info.class")) {
					return true;
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
			for (MavenProject p : project.getCollectedProjects()) {
				for (RemoteRepository r : p.getRemoteProjectRepositories()) {
					if (r.getId().equals(repo.getId())) {
						String url = repo == null ? resolveUrl(remoteBase, remoteJars) : r.getUrl();
						return mavenUrl(url, result.getArtifact().getGroupId(), result.getArtifact().getArtifactId(),
								result.getArtifact().getVersion(), result.getArtifact().getClassifier());
					}
				}
			}
			project = project.getParent();
		}
		return null;
	}

	private String repositoryUrl(String path) {
		return resolveUrl(resolveUrl(remoteBase, appPath), path);
	}

	private String repositoryUrl(ArtifactResult result) {
		return repositoryUrl(getFileName(result.getArtifact()));
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
