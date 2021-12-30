package com.sshtools.forker.updater.maven.plugin;

import static com.sshtools.forker.updater.maven.plugin.UpdaterUtil.checkDir;
import static com.sshtools.forker.updater.maven.plugin.UpdaterUtil.normalizeForUri;
import static com.sshtools.forker.updater.maven.plugin.UpdaterUtil.resolveUrl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.transfer.dependencies.resolve.DependencyResolver;
import org.apache.maven.shared.transfer.dependencies.resolve.DependencyResolverException;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.languages.java.jpms.LocationManager;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import com.sshtools.forker.common.Util;
import com.sshtools.forker.updater.AppManifest;
import com.sshtools.forker.updater.AppManifest.Section;
import com.sshtools.forker.updater.AppManifest.Type;
import com.sshtools.forker.updater.Entry;
import com.sshtools.forker.updater.Updater;
import com.sun.jna.Platform;

@Mojo(threadSafe = true, name = "updates", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresProject = true)
public class ForkerUpdaterMojo extends AbstractMojo implements Context {
	public enum PackageMode {
		JPACKAGE, NONE, SELF_EXTRACTING
	}

	@Parameter
	protected Map<String, String> jdkToolchain;
	
	@Parameter
	protected Map<String, String> systemProperties;

	@Component
	protected LocationManager locationManager;

	/**
	 * The maven project.
	 */
	@Parameter(required = true, readonly = true, property = "project")
	protected MavenProject project;

	@Parameter(defaultValue = "${session}", readonly = true, required = true)
	protected MavenSession session;

	@Component
	protected ToolchainManager toolchainManager;

	@Component(role = ArtifactRepositoryLayout.class)
	protected Map<String, ArtifactRepositoryLayout> repositoryLayouts;

	@Component
	protected org.apache.maven.repository.RepositorySystem repositorySystem;

	@Component
	private RepositorySystem repoSystem;

	@Component
	protected DependencyResolver dependencyResolver;

	@Component
	private List<RemoteRepository> repos;

	/**
	 * List of automatic modules
	 */
	@Parameter
	private List<String> automaticArtifacts;

	/**
	 * Bootstrap config
	 */
	@Parameter
	private Bootstrap bootstrap = new Bootstrap();

	/**
	 * Business config
	 */
	@Parameter
	private Business business = new Business();

	/**
	 * List of classpath jars (overrides automatic detection of type)
	 */
	@Parameter
	private List<String> classpathArtifacts;

	@Parameter(defaultValue = "2", property = "compress")
	private int compress;

	@Parameter
	private List<String> forkerArgs;

	@Parameter
	private File forkerArgFile;

	@Parameter(defaultValue = "${project.artifactId}", property = "id", required = true)
	private String id;

	@Parameter(defaultValue = "true", property = "image")
	private boolean image;

	/**
	 * Location of the file.
	 */
	@Parameter(defaultValue = "${project.build.directory}/image", property = "imageDirectory", required = true)
	private File imageDirectory;

	@Parameter(defaultValue = "true", property = "includeForkerUpdaterRuntimeModules")
	private boolean includeForkerUpdaterRuntimeModules = true;

	@Parameter
	private Properties installerProperties = new Properties();

	@Parameter(property = "installLocation", defaultValue = "${installer.home}/${project.artifactId}", required = true)
	private String installLocation;

	@Parameter(property = "launcherScriptName", defaultValue = "${project.artifactId}", required = true)
	private String launcherScriptName;

	@Parameter(defaultValue = "true")
	private boolean linkJVM;

	@Parameter(defaultValue = "false")
	private boolean link;

	@Parameter
	private List<String> linkOptions;

	@Parameter(property = "mainClass")
	private String mainClass;

	/**
	 * Location of the modulepath jars.
	 */
	@Parameter(defaultValue = "modulepath", property = "modulepath", required = true)
	private String modulePath;

	/**
	 * Location of the classpath jars under business or bootstrap.
	 */
	@Parameter(defaultValue = "classpath", property = "classpath", required = true)
	private String classPath;

	@Parameter(defaultValue = "true", property = "modules")
	private boolean modules = true;

	/**
	 * Package name.
	 */
	@Parameter(defaultValue = "${project.name}", property = "name", required = true)
	private String name;

	@Parameter(defaultValue = "true", property = "noHeaderFiles")
	private boolean noHeaderFiles;

	@Parameter(defaultValue = "true", property = "noManPages")
	private boolean noManPages;

	@Parameter(defaultValue = "NONE", property = "packageMode")
	private PackageMode packageMode = PackageMode.NONE;

	/**
	 * Package name.
	 */
	@Parameter(defaultValue = "${project.artifactId}", property = "packageName", required = true)
	private String packageName;

	@Parameter(defaultValue = "${project.build.directory}/packages", property = "packagePath")
	private String packagePath;

	@Parameter
	private Map<String, String> properties;

	/**
	 * Location of the remote base.
	 */
	@Parameter(required = true, defaultValue = "file://${project.build.directory}/image")
	private String remoteBase;

	/**
	 * Location of the remote config file.
	 */
	@Parameter
	private String remoteConfig;

	/**
	 * Location of the remote jars.
	 */
	@Parameter
	private String remoteJars;

	@Parameter(defaultValue = "false", property = "remotesFromOriginalSource")
	private boolean remotesFromOriginalSource;

	@Parameter(defaultValue = "true", property = "removeImageBeforeLink")
	private boolean removeImageBeforeLink = true;

	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
	private RepositorySystemSession repoSession;

	@Parameter(defaultValue = "${project.remoteArtifactRepositories}", readonly = true, required = true)
	protected List<ArtifactRepository> repositories;

	@Parameter(defaultValue = "true", property = "repository")
	private boolean repository;

	/**
	 * Location of the file.
	 */
	@Parameter(defaultValue = "${project.build.directory}/repository", property = "repositoryDirectory", required = true)
	private File repositoryDirectory;

	/**
	 * List of bootstrap only modules
	 */
	@Parameter
	private List<String> sharedArtifacts;

	@Parameter(defaultValue = "", property = "splash")
	private String splash;

	@Parameter(defaultValue = "true", property = "stripDebug")
	private boolean stripDebug;

	@Parameter
	private Properties uninstallerProperties = new Properties();

	@Parameter
	private List<String> updaterArgs;

	@Parameter(defaultValue = "true", property = "useArgfile")
	private boolean useArgfile = true;

	@Parameter(defaultValue = "${project.version}-${timestamp}", property = "appVersion")
	private String version;

	@Parameter
	private List<String> vmArgs;

	@Parameter
	protected List<Launcher> launchers;

	@Parameter(property = "include")
	protected Boolean includeProject;

	private AppManifest manifest;
	private Launcher defaultLauncher;
	private Launcher defaultInstallerUpdater;
	private Launcher defaultUninstaller;

	private Set<Launcher> allLaunchers;

	public void execute() throws MojoExecutionException {
		manifest = new AppManifest();
		manifest.id(id);
		try {
			manifest.baseUri(new URI(normalizeForUri(remoteBase)));
		} catch (URISyntaxException e1) {
			throw new MojoExecutionException("Invalid remote base.", e1);
		}
		if (!version.equals(""))
			manifest.version(version.replace("${timestamp}", String.valueOf(
					new SimpleDateFormat("yyyyMMddHHmmss").format(new Date(manifest.timestamp().toEpochMilli())))));

		Path imagePath = getImagePath();
		Path repositoryPath = getRepositoryPath();

		try {
			init();
			analyse();

			if (linkJVM) {
				createJRE();
			}
			checkDir(imagePath);

			process();
			postProcess();

			if (launcherScriptName == null || launcherScriptName.equals("")) {
				launcherScriptName = id;
			}

			if (hasAnyLauncherWithMode(LauncherMode.UPDATER)) {
				writeProperties("installer.properties", "Installer Properties", installerProperties, manifest,
						imagePath);
			}
			if (hasAnyLauncherWithMode(LauncherMode.UNINSTALLER)) {
				writeProperties("uninstaller.properties", "Uninstaller Properties", uninstallerProperties, manifest,
						imagePath);
			}

			try (Writer out = Files.newBufferedWriter(checkDir(imagePath).resolve("manifest.xml"))) {
				manifest.save(out);
			}
			if (repository) {
				try (Writer out = Files.newBufferedWriter(checkDir(repositoryPath).resolve("manifest.xml"))) {
					manifest.save(out);
				}
			}

			switch (packageMode) {
			case SELF_EXTRACTING:
				SelfExtractingExecutableBuilder builder = new SelfExtractingExecutableBuilder();
				builder.image(imagePath);
				builder.context(this);
				builder.name(name);
				Launcher l = getLauncherWithMode(LauncherMode.UPDATER);
				if (l == null) {
					l = getLauncherWithMode(LauncherMode.UPDATER);
				}
				if (l != null)
					builder.script(Paths.get("bin").resolve(l.calcFullScriptName()));
				builder.output(Paths.get(packagePath).resolve(packageName));
				builder.make();
				break;
			case NONE:
				break;
			default:
				throw new UnsupportedOperationException("Not yet implemented.");
			}
		} catch (IOException | URISyntaxException | DependencyResolverException | MojoFailureException e) {
			throw new MojoExecutionException("Failed to write configuration.", e);
		}
	}

	public boolean hasAnyLauncherWithMode(LauncherMode mode) {
		return getLauncherWithMode(mode) != null;
	}

	@Override
	public Bootstrap getBootstrap() {
		return bootstrap;
	}

	@Override
	public Business getBusiness() {
		return business;
	}

	@Override
	public Path getImagePath() {
		return imageDirectory.toPath();
	}

	@Override
	public AppManifest getManifest() {
		return manifest;
	}

	@Override
	public String getRemoteBase() {
		return remoteBase;
	}

	@Override
	public Path getRepositoryPath() {
		return repositoryDirectory.toPath();
	}

	@Override
	public boolean isImage() {
		return image;
	}

	@Override
	public boolean isRemotesFromOriginalSource() {
		return remotesFromOriginalSource;
	}

	@Override
	public boolean isRepository() {
		return repository;
	}

	public void init() {

		getLog().info("Initialising");

		defaultLauncher = new Launcher();
		defaultLauncher.setId(getLauncherScriptName());
		defaultLauncher.setMode(LauncherMode.UPDATER);
		defaultInstallerUpdater = new Launcher();
		defaultInstallerUpdater.setId("updater");
		defaultInstallerUpdater.setMainClass(Updater.class.getName());
		defaultInstallerUpdater.setMode(LauncherMode.UPDATER);
		defaultUninstaller = new Launcher();
		defaultUninstaller.setId("uninstaller");
		defaultUninstaller.setMode(LauncherMode.UNINSTALLER);

		bootstrap.init(this);
		business.init(this);

		for (Launcher launcher : getAllLaunchers()) {
			launcher.init(this);
		}
	}

	public void analyse() throws IOException {
		getLog().info("Analyse");
		bootstrap.analyse();
		business.analyse();
		for (Launcher launcher : getAllLaunchers()) {
			launcher.analyse();
		}
	}

	public void process() throws MojoExecutionException, MojoFailureException, IOException, DependencyResolverException,
			URISyntaxException {
		getLog().info("Process");
		bootstrap.process();
		business.process();
		for (Launcher launcher : getAllLaunchers()) {
			launcher.process();
		}
	}

	public void postProcess() throws IOException, URISyntaxException {
		getLog().info("Post Process");
		bootstrap.postProcess();
		business.postProcess();
		for (Launcher launcher : getAllLaunchers()) {
			launcher.postProcess();
		}
	}

	public Launcher getLauncherWithMode(LauncherMode mode) {
		return findLauncherWithMode(mode, getAllLaunchers());
	}

	protected Launcher findLauncherWithMode(LauncherMode mode, Collection<Launcher> launchers) {
		for (Launcher l : launchers) {
			if (l.getMode() == mode) {
				return l;
			}
		}
		return null;
	}

	public boolean isIncludeProject() {
		return includeProject;
	}

	@Override
	public boolean calcIncludeProject() {
		return (includeProject == null && !"pom".equals(project.getPackaging())) || Boolean.TRUE.equals(includeProject);
	}

	@Override
	public final Set<Launcher> getAllLaunchers() {
		if(allLaunchers == null) {
			allLaunchers = calcLaunchers();
		}
		return allLaunchers;
	}
	
	protected final Set<Launcher> calcLaunchers() {
		Set<Launcher> ll = new LinkedHashSet<>();
		for (Launcher l : calcUnfilteredLaunchers()) {
			if (!l.isSkip())
				ll.add(l);
		}
		return ll;
	}

	protected Set<Launcher> calcUnfilteredLaunchers() {
		Set<Launcher> l = new LinkedHashSet<>();

		/* Now overrides and extras */
		if (launchers != null)
			l.addAll(launchers);

		/* Default installer,updater and defaultLauncher */
		if (calcIncludeProject())
			l.add(defaultLauncher);

		/* Default defaultUninstaller */
		l.add(defaultUninstaller);

		/*
		 * If there are no UPDATER type launchers, and this build is supposed to be
		 * updateable, then create an 'updater'
		 */
		if ( findLauncherWithMode(LauncherMode.UPDATER, l) == null
			    && findLauncherWithMode(LauncherMode.WRAPPED, l) == null
				&& (bootstrap.isUpdateable() || business.isUpdateable())) {
			l.add(defaultInstallerUpdater);
		}

		return l;
	}

	private void addDefaultProperties(Properties allProperties) {
		allProperties.put("title", project.getName());
		allProperties.put("description", project.getDescription() == null ? "" : project.getDescription());
		allProperties.put("version", project.getVersion());
	}

	private Set<String> getAllSystemModules() {
		Set<String> allSystemModules = UpdaterUtil.concat(bootstrap.calcSystemModules(), business.calcSystemModules());
		for (Launcher l : getAllLaunchers()) {
			allSystemModules.addAll(l.calcSystemModules());
		}
		return allSystemModules;
	}

	private void createJRE() throws IOException, MojoExecutionException {
		if (removeImageBeforeLink) {
			getLog().info("Clearing jlink directory '" + imageDirectory + "'");
			Util.deleteRecursiveIfExists(imageDirectory);
		}
		ProcessBuilder pb = new ProcessBuilder(getJLinkExecutable());
		Set<String> allSystemModules = getAllSystemModules();
		if (!allSystemModules.isEmpty()) {
			pb.command().add("--add-modules");
			pb.command().add(String.join(",", allSystemModules));
		}
		pb.command().add("--output");
		pb.command().add(imageDirectory.getPath());
		pb.redirectErrorStream(true);
		if (noHeaderFiles)
			pb.command().add("--no-header-files");
		if (noManPages)
			pb.command().add("--no-man-pages");
		pb.command().add("--compress=" + compress);
		if (stripDebug) {
			pb.command().add("--strip-debug");
		}
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

	private String getJLinkExecutable() throws IOException {
		return this.getToolExecutable("jlink");
	}

	private Toolchain getToolchain() {
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

	public String getToolExecutable(final String toolName) throws IOException {
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

	private void writeProperties(String name, String description, Properties properties, AppManifest manifest,
			Path imagePath) throws IOException, URISyntaxException {
		Properties allProperties = new Properties();
		addDefaultProperties(allProperties);
		if (properties != null) {
			allProperties.putAll(properties);
		}
		Path propertiesPath = checkDir(imagePath).resolve(name);
		try (Writer out = Files.newBufferedWriter(propertiesPath)) {
			allProperties.store(out, description);
		}
		manifest.entries().add(new Entry(propertiesPath, manifest).section(Section.BOOTSTRAP)
				.path(imagePath.relativize(propertiesPath))
				.uri(new URI(resolveUrl(normalizeForUri(remoteBase), imagePath.relativize(propertiesPath).toString())))
				.type(Type.OTHER));
	}

	@Override
	public String getMainClass() {
		return mainClass;
	}

	@Override
	public Map<String, String> getProperties() {
		return properties;
	}

	@Override
	public boolean isModules() {
		return modules;
	}

	@Override
	public String getRemoteJars() {
		return remoteJars;
	}

	@Override
	public List<ArtifactRepository> getRepositories() {
		return repositories;
	}

	@Override
	public RepositorySystemSession getRepoSession() {
		return repoSession;
	}

	@Override
	public RepositorySystem getRepoSystem() {
		return repoSystem;
	}

	@Override
	public org.apache.maven.repository.RepositorySystem getRepositorySystem() {
		return repositorySystem;
	}

	@Override
	public boolean isIncludeForkerUpdaterRuntimeModules() {
		return includeForkerUpdaterRuntimeModules;
	}

	@Override
	public String getClassPath() {
		return classPath;
	}

	@Override
	public String getModulePath() {
		return modulePath;
	}

	@Override
	public MavenProject getProject() {
		return project;
	}

	@Override
	public MavenSession getSession() {
		return session;
	}

	@Override
	public Map<String, ArtifactRepositoryLayout> getRepositoryLayouts() {
		return repositoryLayouts;
	}

	@Override
	public DependencyResolver getDependencyResolver() {
		return dependencyResolver;
	}

	@Override
	public List<RemoteRepository> getRepos() {
		return repos;
	}

	@Override
	public boolean isLinkJVM() {
		return linkJVM;
	}

	@Override
	public boolean isLink() {
		return link;
	}

	@Override
	public String getSplash() {
		return splash;
	}

	@Override
	public List<String> getVmArgs() {
		return vmArgs;
	}

	@Override
	public List<String> getUpdaterArgs() {
		return updaterArgs;
	}

	@Override
	public boolean isUseArgFile() {
		return useArgfile;
	}

	@Override
	public String getInstallLocation() {
		return installLocation;
	}

	@Override
	public List<String> getForkerArgs() {
		return forkerArgs;
	}

	@Override
	public String getLauncherScriptName() {
		return launcherScriptName;
	}

	@Override
	public File getForkerArgsFile() {
		return forkerArgFile;
	}

	@Override
	public Path resolveLaunchersSrcPath() {
		return Paths.get("src/main/launchers");
	}

	@Override
	public Map<String, String> getSystemProperties() {
		return systemProperties;
	}

	@Override
	public List<String> getLinkOptions() {
		return linkOptions;
	}

}
