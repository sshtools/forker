package com.sshtools.forker.updater.maven.plugin;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.shared.transfer.dependencies.resolve.DependencyResolverException;

import com.sshtools.forker.updater.AppManifest.Section;
import com.sshtools.forker.updater.AppManifest.Type;
import com.sshtools.forker.updater.Entry;

public class Business extends AbstractStage {

	private static final String MAVEN_BASE = "https://repo1.maven.org/maven2";

	public Business() {
		super(Section.APP);
	}

	@Override
	Set<Artifact> calcDependencies() throws DependencyResolverException, MojoFailureException {
		Set<Artifact> allArtifacts = new LinkedHashSet<>(context.getProject().getArtifacts());
		allArtifacts.addAll(super.calcDependencies());
		return allArtifacts;
	}

	@Override
	public Path resolvePath() {
		return Paths.get(path == null ? "app/business" : path);
	}

	@Override
	public Path resolveSrcPath() {
		return Paths.get("src/main/business");
	}


	@Override
	protected List<Dependency> calcResolvableDependencies() {
		List<Dependency> allResolvable = new ArrayList<>();
		allResolvable.addAll(super.calcResolvableDependencies());
		for(Launcher l : context.getAllLaunchers()) {
			allResolvable.addAll(l.calcResolvableDependencies());
		}
		return allResolvable;
	}
	
	@Override
	public void onPostProcess() throws IOException, URISyntaxException {

		context.getManifest().getProperties().put("maven.central", MAVEN_BASE);
//		String mainClass = context.getMainClass();
//		if (StringUtils.isBlank(mainClass)) {
//			if (getAnalyser().mainClasses().isEmpty()) {
//				throw new IOException(
//						"mainClass cannot be discovered. Try specifying it in plugin configuration (fully qualified if modules are in use).");
//			}
//			mainClass = getAnalyser().mainClasses().get(0);
//		}
//		info(String.format("Using mainClass of %s", mainClass));
//		context.getManifest().getProperties().put("main", mainClass);
		
		if (context.getProperties() != null) {
			for (Map.Entry<String, String> en : context.getProperties().entrySet()) {
				context.getManifest().getProperties().put(en.getKey(), en.getValue());
			}
		}
	}

	@Override
	protected void doFiles(Set<StageFile> files) throws IOException {

		for (StageFile file : files) {
			Path path = Paths.get(file.source);
			Path target = context.getImagePath()
					.resolve(file.target.equals(".") ? resolvePath() : resolvePath().resolve(file.target))
					.resolve(path.getFileName());
			Path relTarget = context.getImagePath().relativize(target);
			UpdaterUtil.copy(defaultIndent, log, "Business file", path, target, context.getManifest().timestamp());
			try {
				context.getManifest().entries()
						.add(new Entry(target, context.getManifest()).section(Section.APP).path(relTarget)
								.uri(new URI(UpdaterUtil.resolveUrl(
										UpdaterUtil.normalizeForUri(context.getRemoteBase()), relTarget.toString())))
								.type(Type.OTHER));
			} catch (URISyntaxException e) {
				throw new IOException("URI syntax.", e);
			}

		}

	}

}
