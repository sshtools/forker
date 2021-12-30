package com.sshtools.forker.updater.maven.plugin;

import static com.sshtools.forker.updater.maven.plugin.UpdaterUtil.indent;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResult;
import org.apache.maven.shared.transfer.dependencies.DefaultDependableCoordinate;
import org.apache.maven.shared.transfer.dependencies.resolve.DependencyResolverException;

public abstract class AbstractComponent {
	private static final Pattern ALT_REPO_SYNTAX_PATTERN = Pattern.compile("(.+)::(.*)::(.+)");

	@Parameter
	protected List<Dependency> dependencies;

	@Parameter
	protected boolean useRemoteRepositories = true;

	@Parameter
	protected String updatePolicy;

	@Parameter
	protected String checksumPolicy;

	@Parameter
	protected String repositories;

	/**
	 * List of system modules (overrides automatic detection of type)
	 */
	@Parameter
	protected List<String> systemModules;
	/**
	 * Whether to auto detect system modules
	 */
	@Parameter
	protected boolean autoDetectSystemModules = true;
	/**
	 * Whether to auto detect system modules
	 */
	@Parameter
	protected Boolean link;

	protected Context context;
	protected Log log;
	protected Analyser al = new Analyser();
	protected int defaultIndent;

	protected AbstractComponent(int defaultIndent) {
		this.defaultIndent = defaultIndent;
	}

	protected List<Dependency> calcResolvableDependencies() {
		return dependencies == null ? Collections.emptyList() : dependencies;
	}

	public List<Dependency> getDependencies() {
		return calcResolvableDependencies();
	}

	public Analyser getAnalyser() {
		return al;
	}

	public final void postProcess() throws IOException, URISyntaxException {
		info("Post Processing " + description());
		defaultIndent++;
		try {
			onPostProcess();
		} finally {
			defaultIndent--;
		}
	}

	public final void process() throws IOException, MojoExecutionException, DependencyResolverException,
			URISyntaxException, MojoFailureException {
		info("Processing " + description());
		defaultIndent++;
		try {
			onProcess();
		} finally {
			defaultIndent--;
		}
	}

	protected void debug(String message) {
		log.debug(indent(defaultIndent) + message);
	}

	protected void warn(String message) {
		log.warn(indent(defaultIndent) + message);
	}

	protected void info(String message) {
		log.info(indent(defaultIndent) + message);
	}

	protected void error(String message) {
		log.error(indent(defaultIndent) + message);
	}

	protected void error(String message, Throwable ex) {
		log.error(indent(defaultIndent) + message, ex);
	}

	protected void onPostProcess() throws IOException, URISyntaxException {
	}

	protected String description() {
		return getClass().getSimpleName().toLowerCase();
	}

	protected final void init(Context context) {
		if (context == null)
			throw new IllegalArgumentException();
		if (this.context != null)
			throw new IllegalStateException();
		this.context = context;
		this.log = context.getLog();
		info("Initialising " + description());
		al.log(context);
		defaultIndent++;
		try {
			al.indent(defaultIndent + 1);
			onInit();
		} finally {
			defaultIndent--;
		}
	}
	
	protected boolean calcLink() {
		if(link == null) {
			return context.isLink();
		}
		return link;
	}

	public Boolean getLink() {
		return link;
	}

	public void setLink(Boolean link) {
		this.link = link;
	}

	protected abstract void onProcess() throws IOException, MojoExecutionException, DependencyResolverException,
			URISyntaxException, MojoFailureException;

	protected void onInit() {
	}

	protected final void analyse() throws IOException {

		info("Analysing " + description());
		defaultIndent++;
		try {

//		if (calcIncludeProject()) {
//			File dir = new File(context.getProject().getBuild().getOutputDirectory());
//			if (dir.exists())
//				al.getDirectories().add(dir);
//		}
			try {
				for (Artifact artifact : calcDependencies()) {
					al.getArtifacts().add(artifact);
				}
			} catch (DependencyResolverException | MojoFailureException e) {
				throw new IOException("Failed to calculate dependencies.", e);
			}

			info("Anaylsing jars");
			al.analyse();
			info("Anaylsed jars ");

			info("All modules are: " + String.join(",", al.discoveredModules()));
			info("Required modules are: " + String.join(",", al.requiredModules()));
			info("Unresolved modules are: " + String.join(",", al.unresolvedModules()));
			info("System modules are: " + String.join(",", calcSystemModules()));

			onAnalyse();
		} finally {
			defaultIndent--;
		}
	}

	protected void onAnalyse() throws IOException {
	}

	public Set<String> calcSystemModules() {
		Set<String> allSystemModules = new LinkedHashSet<>();
		if (autoDetectSystemModules) {
			allSystemModules.addAll(al.systemModules());
		}
		if (systemModules != null && systemModules.size() > 0) {
			allSystemModules.addAll(systemModules);
		}
		return allSystemModules;
	}

	Set<Artifact> calcStageDependencies() throws DependencyResolverException, MojoFailureException {

		if (calcResolvableDependencies() == null)
			return Collections.emptySet();

		ArtifactRepositoryPolicy always = new ArtifactRepositoryPolicy(true,
				updatePolicy == null ? ArtifactRepositoryPolicy.UPDATE_POLICY_DAILY : updatePolicy,
				checksumPolicy == null ? ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN : checksumPolicy);

		List<ArtifactRepository> repoList = new ArrayList<>();
		List<ArtifactRepository> pomRemoteRepositories = context.getRepositories();
		if (pomRemoteRepositories != null && useRemoteRepositories) {
			repoList.addAll(pomRemoteRepositories);
		}

		if (repositories != null) {
			// Use the same format as in the deploy plugin id::layout::url
			String[] repos = repositories.split(",");
			for (String repo : repos) {
				repoList.add(parseRepository(repo, always));
			}
		}

		ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(
				context.getSession().getProjectBuildingRequest());

		Settings settings = context.getSession().getSettings();
		context.getRepositorySystem().injectMirror(repoList, settings.getMirrors());
		context.getRepositorySystem().injectProxy(repoList, settings.getProxies());
		context.getRepositorySystem().injectAuthentication(repoList, settings.getServers());

		buildingRequest.setRemoteRepositories(repoList);

		Set<Artifact> artifacts = new LinkedHashSet<>();
		for (Dependency dep : calcResolvableDependencies()) {
			DefaultDependableCoordinate coordinate = new DefaultDependableCoordinate();
			coordinate.setArtifactId(dep.getArtifactId());
			coordinate.setGroupId(dep.getGroupId());
			coordinate.setClassifier(dep.getClassifier());
			coordinate.setType(dep.getType());
			coordinate.setVersion(dep.getVersion());
			debug("Resolving " + coordinate + " with transitive dependencies");
			for (ArtifactResult result : context.getDependencyResolver().resolveDependencies(buildingRequest,
					coordinate, null)) {
				artifacts.add(result.getArtifact());
			}
		}
		return artifacts;
	}

	Set<Artifact> calcDependencies() throws DependencyResolverException, MojoFailureException {
		Set<Artifact> allArtifacts = new LinkedHashSet<>(calcStageDependencies());
		if (context.calcIncludeProject())
			allArtifacts.add(context.getProject().getArtifact());
		return allArtifacts;
	}

	private ArtifactRepository parseRepository(String repo, ArtifactRepositoryPolicy policy)
			throws MojoFailureException {
		// if it's a simple url
		String id = "temp";
		ArtifactRepositoryLayout layout = getLayout("default");
		String url = repo;

		// if it's an extended repo URL of the form id::layout::url
		if (repo.contains("::")) {
			Matcher matcher = ALT_REPO_SYNTAX_PATTERN.matcher(repo);
			if (!matcher.matches()) {
				throw new MojoFailureException(repo, "Invalid syntax for repository: " + repo,
						"Invalid syntax for repository. Use \"id::layout::url\" or \"URL\".");
			}

			id = matcher.group(1).trim();
			if (!StringUtils.isEmpty(matcher.group(2))) {
				layout = getLayout(matcher.group(2).trim());
			}
			url = matcher.group(3).trim();
		}
		return new MavenArtifactRepository(id, url, layout, policy, policy);
	}

	private ArtifactRepositoryLayout getLayout(String id) throws MojoFailureException {
		ArtifactRepositoryLayout layout = context.getRepositoryLayouts().get(id);

		if (layout == null) {
			throw new MojoFailureException(id, "Invalid repository layout", "Invalid repository layout: " + id);
		}

		return layout;
	}
}
