package com.sshtools.forker.updater.maven.plugin;

import static com.sshtools.forker.updater.maven.plugin.UpdaterUtil.checkFilesDir;
import static com.sshtools.forker.updater.maven.plugin.UpdaterUtil.copy;
import static com.sshtools.forker.updater.maven.plugin.UpdaterUtil.normalizeForUri;
import static com.sshtools.forker.updater.maven.plugin.UpdaterUtil.resolveUrl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugins.annotations.Parameter;

import com.sshtools.forker.updater.AppManifest.Section;
import com.sshtools.forker.updater.AppManifest.Type;
import com.sshtools.forker.updater.Entry;

public class Bootstrap extends AbstractStage {

	final static List<String> DEFAULT_TOOLKITS = Arrays.asList("console", "swing");

	@Parameter
	protected List<String> toolkits;

	public Bootstrap() {
		super(Section.BOOTSTRAP);
	}

	@Override
	public void onPostProcess() throws IOException, URISyntaxException {

		/* Anything thats not in the business base is a boostrap entry */
		if (isUpdateable()) {
			Path mf = context.getImagePath().resolve("manifest.xml");
			Files.walk(context.getImagePath()).forEach(s -> {
				if (Files.isRegularFile(s) && !mf.equals(s)) {
					Path relPath = context.getImagePath().relativize(s);
					if (!relPath.startsWith(context.getBusiness().resolvePath()) && !relPath.startsWith(resolvePath())
							&& !context.getManifest().hasPath(relPath)) {
						try {
							context.getManifest().entries()
									.add(new Entry(s, context.getManifest()).section(Section.BOOTSTRAP).type(Type.OTHER)
											.path(Paths.get("/").resolve(relPath))
											.uri(new URI(resolveUrl(normalizeForUri(context.getRemoteBase()),
													normalizeForUri(relPath.toString())))));
						} catch (IOException | URISyntaxException e) {
							throw new IllegalStateException("Failed to construct bootstrap manifest entry.", e);
						}
						if (context.isRepository()) {
							if (!Files.isSymbolicLink(s)) {
								Path t = checkFilesDir(context.getRepositoryPath().resolve(relPath));
								try {
									copy(defaultIndent, log, "Non-business bootstrap file", s, t, context.getManifest().timestamp());
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
	}

	@Override
	protected List<Dependency> calcResolvableDependencies() {
		List<Dependency> allResolvable = new ArrayList<>();
		allResolvable.addAll(super.calcResolvableDependencies());
		List<String> tk = toolkits == null ? DEFAULT_TOOLKITS : toolkits;
		for (String toolkit : tk) {
			Dependency dep = new Dependency();
			dep.setGroupId("com.sshtools");
			dep.setArtifactId("forker-updater-" + toolkit);
			dep.setVersion(ArtifactVersion.getVersion("com.sshtools/forker-updater-maven-plugin"));
			allResolvable.add(dep);
		}
		return allResolvable;
	}

	@Override
	public Path resolvePath() {
		return Paths.get(path == null ? "app/bootstrap" : path);
	}

	@Override
	public Path resolveSrcPath() {
		return Paths.get("src/main/bootstrap");
	}

	@Override
	protected void doFiles(Set<StageFile> files) throws IOException {

		Path installFileSrc = Paths.get(context.getProject().getFile().getParentFile().getAbsolutePath());
		for (StageFile file : files) {
			Path path = installFileSrc.resolve(file.source);
			Path target = context.getImagePath().resolve(
					file.target.equals(".") ? context.getImagePath() : context.getImagePath().resolve(file.target))
					.resolve(path.getFileName());
			copy(defaultIndent, log, "Bootstrap File", path, target, context.getManifest().timestamp());
			if (updateable) {
				try {
					context.getManifest().entries()
							.add(new Entry(target, context.getManifest()).section(Section.BOOTSTRAP)
									.path(context.getImagePath().relativize(target))
									.uri(new URI(UpdaterUtil.resolveUrl(context.getRemoteBase(),
											context.getImagePath().relativize(target).toString())))
									.type(Type.OTHER));
				} catch (URISyntaxException e) {
					throw new IOException("URI syntax.", e);
				}
				target = context.getRepositoryPath().resolve(file.target.equals(".") ? context.getRepositoryPath()
						: context.getRepositoryPath().resolve(file.target)).resolve(path.getFileName());
				copy(defaultIndent, log, "Updateable Bootstrap File", path, target, context.getManifest().timestamp());
			}
		}

	}
}
