package com.sshtools.forker.updater.maven.plugin;

import static com.sshtools.forker.updater.maven.plugin.UpdaterUtil.getFileName;
import static com.sshtools.forker.updater.maven.plugin.UpdaterUtil.isRemote;
import static com.sshtools.forker.updater.maven.plugin.UpdaterUtil.normalizeForUri;
import static com.sshtools.forker.updater.maven.plugin.UpdaterUtil.resolveUrl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.transfer.dependencies.resolve.DependencyResolverException;
import org.eclipse.aether.repository.RemoteRepository;

import com.sshtools.forker.updater.AppManifest.Section;
import com.sshtools.forker.updater.AppManifest.Type;
import com.sshtools.forker.updater.Entry;
import com.sshtools.forker.updater.maven.plugin.Analyser.AnalysedItem;
import com.sun.tools.sjavac.Log;

public abstract class AbstractStage extends AbstractComponent {

	@Parameter
	protected Map<String, String> artifactArch = new HashMap<>();

	@Parameter
	protected Map<String, String> artifactOS = new HashMap<>();

	@Parameter
	protected List<String> artifacts;

	@Parameter
	protected Set<StageFile> files;

	@Parameter
	protected boolean includeVersion;

	@Parameter
	protected String path;

	protected Section section;

	@Parameter
	protected boolean updateable = true;

	public AbstractStage(Section section) {
		super(1);
		this.section = section;
	}

	public List<String> getArtifacts() {
		return artifacts;
	}

	public Set<StageFile> getFiles() {
		return files;
	}

	public String getPath() {
		return path;
	}

	public List<String> getSystemModules() {
		return systemModules;
	}

	public boolean isAutoDetectSystemModules() {
		return autoDetectSystemModules;
	}

	public boolean isIncludeVersion() {
		return includeVersion;
	}

	public boolean isUpdateable() {
		return updateable;
	}

	protected final void onProcess() throws IOException, MojoExecutionException, DependencyResolverException,
			URISyntaxException, MojoFailureException {
		for (Artifact a : calcDependencies()) {
			doArtifact(a);
		}

		/* Check files for validity and/or expand */
		Set<StageFile> filtered = new LinkedHashSet<>();
		Set<StageFile> allFiles = calcFiles();
		info(String.format("Found %d files to include in %s", allFiles.size(), section));
		for (StageFile file : allFiles) {
			if (file.isSkip())
				info(String.format("Explicitly skipping %s file %s", section, file));
			else {
				Path resolved = Paths.get(file.source);
				if (!Files.exists(resolved)) {
					info(String.format("Skipping %s file %s, it doesn't exist.", section, resolved));
				} else {
					if (Files.isDirectory(resolved)) {
						info(String.format("Scanning %s directory files %s.", section, resolved));
						Files.walkFileTree(resolved, new FileVisitor<Path>() {

							@Override
							public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
								return FileVisitResult.CONTINUE;
							}

							@Override
							public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
									throws IOException {
								return FileVisitResult.CONTINUE;
							}

							@Override
							public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
								if (!Files.isDirectory(file)) {
									defaultIndent++;
									try {
										info(String.format("Adding %s", file.toString()));
										filtered.add(new StageFile(file.toString()));
									}
									finally {
										defaultIndent--;
									}
								}
								return FileVisitResult.CONTINUE;
							}

							@Override
							public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
								error("Failed to visit directory.", exc);
								return file.equals(resolved) ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
							}
						});
					}
					else {
						defaultIndent++;
						try {
							info(String.format("Adding %s", file.toString()));
							filtered.add(file);
						}
						finally {
							defaultIndent--;
						}						
					}
				}
			}
		}

		doFiles(filtered);
	}

	public abstract Path resolvePath();

	public abstract Path resolveSrcPath();

	protected Set<StageFile> calcFiles() {
		Set<StageFile> files = new LinkedHashSet<>();
		if (this.files != null)
			files.addAll(this.files);
		files.add(new StageFile(resolveSrcPath().toString()));
		return files;
	}

	protected void doArtifact(Artifact a) throws MojoExecutionException, IOException, URISyntaxException {
		String artifactId = a.getArtifactId();
		AnalysedItem item = al.analysedItem(a);

		Path file = item.getFile();
		if (file == null || !Files.exists(file)) {
			warn("Artifact " + artifactId
					+ " has no attached file. Its content will not be copied in the target model directory.");
			return;
		}

		if (!"jar".equals(a.getType())) {
			info(String.format("Skipping %s, it is not a jar", artifactId));
			return;
		}

		boolean isModule = item.isUseAsModule();

		log.debug(String.format("Adding artifact: %s   Module: %s, Path: %s Section: %s",
				getFileName(includeVersion, a), isModule, resolvePath(), section));

		/* Bootstrap */
		if (isModule && context.isModules()) {
			Path dir = UpdaterUtil.resolvePath(resolvePath(), context.getModulePath());
			module(dir, false, a, item, file);
		} else {
			Path dir = UpdaterUtil.resolvePath(resolvePath(), context.getClassPath());
			classpath(dir, false, a, item, file);
		}

		return;
	}

//	protected void doArtifact(Artifact a) throws MojoExecutionException, IOException, URISyntaxException {
//		String artifactId = a.getArtifactId();
//		org.eclipse.aether.artifact.Artifact aetherArtifact = new DefaultArtifact(a.getGroupId(), a.getArtifactId(),
//				a.getClassifier(), a.getType(), a.getVersion());
//
//		org.eclipse.aether.resolution.ArtifactResult resolutionResult = resolveRemoteArtifact(
//				new HashSet<MavenProject>(), context.getProject(), aetherArtifact, context.getRepos());
//		if (resolutionResult == null)
//			throw new MojoExecutionException("Artifact " + aetherArtifact.getGroupId() + ":"
//					+ aetherArtifact.getArtifactId() + " could not be resolved.");
//
//		aetherArtifact = resolutionResult.getArtifact();
//
//		File file = aetherArtifact.getFile();
//		if (file == null || !file.exists()) {
//			warn("Artifact " + artifactId
//					+ " has no attached file. Its content will not be copied in the target model directory.");
//			return;
//		}
//
//		if (!"jar".equals(aetherArtifact.getExtension())) {
//			info(String.format("Skipping %s, it is not a jar", artifactId));
//			return;
//		}
//
//		boolean isModule = isModule(aetherArtifact);
//
//		log.debug(String.format("Adding artifact: %s   Module: %s, Path: %s Section: %s",
//				getFileName(includeVersion, a), isModule, resolvePath(), section));
//
//		/* Bootstrap */
//		if (isModule) {
//			Path dir = UpdaterUtil.resolvePath(resolvePath(), context.getModulePath());
//			module(dir, false, a, resolutionResult, file);
//		} else {
//			Path dir = UpdaterUtil.resolvePath(resolvePath(), context.getClassPath());
//			classpath(dir, false, a, resolutionResult, file);
//		}
//
//		return;
//	}

	protected abstract void doFiles(Set<StageFile> files) throws IOException;

//	private boolean isBootstrap(org.eclipse.aether.artifact.Artifact a) {
//		return isShared(a) || (context.isIncludeForkerUpdaterRuntimeModules() && UpdaterUtil.isForkerUpdaterBootstrap(a))
//				|| (!isShared(a) && containsArtifact(bootstrap.getArtifacts(), a));
//	}
//
//	private boolean isBusiness(org.eclipse.aether.artifact.Artifact a) {
//		return (!isBootstrap(a) && !isShared(a)) || artifactEquals(a, project.getArtifact()) || isShared(a)
//				|| ((context.isIncludeForkerUpdaterRuntimeModules() && UpdaterUtil.isForkerUpdaterRuntime(a)))
//				|| (!isShared(a) && (containsArtifact(business.getArtifacts(), a)));
//	}

//	private boolean isShared(org.eclipse.aether.artifact.Artifact a) {
//		return containsArtifact(sharedArtifacts, a);
//	}

	@Override
	protected final void onAnalyse() throws IOException {
	}

	@Override
	protected void onInit() {
		context.getManifest().sectionPath(section, resolvePath());
	}

//	private boolean artifactEquals(org.eclipse.aether.artifact.Artifact a, Artifact artifact) {
//		return a.getArtifactId().equals(artifact.getArtifactId()) && a.getGroupId().equals(artifact.getGroupId());
//	}

	private void classpath(Path path, boolean business, Artifact a, AnalysedItem resolutionResult, Path file)
			throws IOException, URISyntaxException {
		String finalClassPath = path.resolve(getFileName(includeVersion, a)).toString();
		info(String.format("Adding %s classpath jar %s to updater config.", a.getFile(), finalClassPath));
		if (context.isImage())
			UpdaterUtil.copy(defaultIndent, log, "Image classpath jar", a.getFile().toPath(),
					context.getImagePath().resolve(finalClassPath), context.getManifest().timestamp());
		String remoteUrl = mavenUrl(resolutionResult);
		Entry entry;
		Path entryPath = Paths.get(getFileName(includeVersion, a));
		if (context.isRepository()) {
			if (context.isRemotesFromOriginalSource()) {
				if (!isRemote(remoteUrl) && !Files.isSymbolicLink(file)) {
					UpdaterUtil.copy(defaultIndent, log, "Classpath jar from Maven", a.getFile().toPath(),
							context.getRepositoryPath().resolve(finalClassPath), context.getManifest().timestamp());
				}
				if (remoteUrl == null)
					remoteUrl = repositoryUrl(resolutionResult, path);
				entry = new Entry(file, context.getManifest()).section(section).name(entryPath).uri(new URI(remoteUrl))
						.type(Type.CLASSPATH);
			} else {
				entry = new Entry(file, context.getManifest()).section(section).name(entryPath)
						.uri(new URI(repositoryUrl(resolutionResult, path))).type(Type.CLASSPATH);
				if (!Files.isSymbolicLink(file))
					UpdaterUtil.copy(defaultIndent, log, "Classpath jar from Local", file,
							context.getRepositoryPath().resolve(finalClassPath), context.getManifest().timestamp());
			}
		} else {
			entry = new Entry(file, context.getManifest()).section(section).name(entryPath).uri(new URI(remoteUrl))
					.type(Type.CLASSPATH);
		}
		setArchitectures(a, entry);
		setOS(a, entry);
		context.getManifest().entries().add(entry);
	}

//	private boolean containsArtifact(Collection<String> artifactNames, org.eclipse.aether.artifact.Artifact artifact) {
//		if (artifactNames == null)
//			return false;
//		String k = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getClassifier();
//		if (artifactNames.contains(k))
//			return true;
//		k = artifact.getGroupId() + ":" + artifact.getArtifactId();
//		if (artifactNames.contains(k))
//			return true;
//		k = artifact.getArtifactId();
//		if (artifactNames.contains(k))
//			return true;
//		return false;
//	}

	private String mavenUrl(AnalysedItem result) {
		ArtifactRepository repo = result.getRepository();
		if(repo == null) {
			log.warn("No repo for " + result.toString());
			return null;
		}
		MavenProject project = context.getProject();
		while (project != null) {
			List<MavenProject> collectedProjects = project.getCollectedProjects();
			if (collectedProjects != null) {
				for (MavenProject p : collectedProjects) {
					for (RemoteRepository r : p.getRemoteProjectRepositories()) {
						if (r.getId().equals(repo.getId())) {
							String url = repo == null
									? resolveUrl(normalizeForUri(context.getRemoteBase()), context.getRemoteJars())
									: r.getUrl();
							return UpdaterUtil.mavenUrl(url, result.getArtifact().getGroupId(),
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

	private void module(Path path, boolean business, Artifact a, AnalysedItem resolutionResult, Path file)
			throws IOException, URISyntaxException {
		String finalModulePath = path.resolve(getFileName(includeVersion, a)).toString();
		info(String.format("Adding %s module jar %s to updater config.", a.getFile(), finalModulePath));
		if (context.isImage())
			UpdaterUtil.copy(defaultIndent, log, "Modulepath image jar", file,
					context.getImagePath().resolve(finalModulePath), context.getManifest().timestamp());
		String remoteUrl = mavenUrl(resolutionResult);
		Entry entry;
		Path entryPath = Paths.get(getFileName(includeVersion, a));
		if (context.isRepository()) {
			if (context.isRemotesFromOriginalSource()) {
				if (!isRemote(remoteUrl)) {
					UpdaterUtil.copy(defaultIndent, log, "Modulepath jar from Maven", file,
							context.getRepositoryPath().resolve(finalModulePath), context.getManifest().timestamp());
				}
				if (remoteUrl == null)
					remoteUrl = repositoryUrl(resolutionResult, path);
				entry = new Entry(file, context.getManifest()).section(section).name(entryPath).uri(new URI(remoteUrl))
						.type(Type.MODULEPATH);
			} else {
				entry = new Entry(file, context.getManifest()).section(section).name(entryPath)
						.uri(new URI(repositoryUrl(resolutionResult, path))).type(Type.MODULEPATH);
				UpdaterUtil.copy(defaultIndent, log, "Modulepath jar from Local", a.getFile().toPath(),
						context.getRepositoryPath().resolve(finalModulePath), context.getManifest().timestamp());
			}
		} else {
			entry = new Entry(file, context.getManifest()).section(section).name(entryPath).uri(new URI(remoteUrl))
					.type(Type.MODULEPATH);
		}

		setArchitectures(a, entry);
		setOS(a, entry);

		context.getManifest().entries().add(entry);
	}

	private String repositoryUrl(AnalysedItem result, Path sectionPath) {
		return repositoryUrl(getFileName(includeVersion, result.getArtifact()), sectionPath);
	}

	private String repositoryUrl(String path, Path sectionPath) {
		return resolveUrl(resolveUrl(normalizeForUri(context.getRemoteBase()), normalizeForUri(sectionPath.toString())),
				normalizeForUri(path));
	}

//	private org.eclipse.aether.resolution.ArtifactResult resolveRemoteArtifact(Set<MavenProject> visitedProjects,
//			MavenProject project, org.eclipse.aether.artifact.Artifact aetherArtifact, List<RemoteRepository> repos)
//			throws MojoExecutionException {
//		ArtifactRequest req = new ArtifactRequest().setRepositories(repos).setArtifact(aetherArtifact);
//		org.eclipse.aether.resolution.ArtifactResult resolutionResult = null;
//		visitedProjects.add(project);
//		try {
//			resolutionResult = context.getRepoSystem().resolveArtifact(context.getRepoSession(), req);
//
//		} catch (ArtifactResolutionException e) {
//			if (project.getParent() == null) {
//				/* Reached the root (reactor), now look in child module repositories too */
//				for (MavenProject p : context.getSession().getAllProjects()) {
//					if (!visitedProjects.contains(p)) {
//						try {
//							resolutionResult = resolveRemoteArtifact(visitedProjects, p, aetherArtifact,
//									p.getRemoteProjectRepositories());
//							if (resolutionResult != null)
//								break;
//						} catch (MojoExecutionException mee) {
//						}
//					}
//				}
//			} else if (!visitedProjects.contains(project.getParent()))
//				return resolveRemoteArtifact(visitedProjects, project.getParent(), aetherArtifact,
//						project.getParent().getRemoteProjectRepositories());
//		}
//		return resolutionResult;
//	}

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

	public Section getSection() {
		return section;
	}
}
