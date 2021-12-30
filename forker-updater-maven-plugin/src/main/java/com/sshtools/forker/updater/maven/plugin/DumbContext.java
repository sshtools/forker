package com.sshtools.forker.updater.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.transfer.dependencies.resolve.DependencyResolver;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import com.sshtools.forker.updater.AppManifest;

final class DumbContext implements Context {

	private Log log;

	{
		log = new SystemStreamLog();
	}

	@Override
	public Log getLog() {
		return log;
	}

	@Override
	public Business getBusiness() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Bootstrap getBootstrap() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AppManifest getManifest() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Path getImagePath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRemoteBase() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Path getRepositoryPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isRepository() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isImage() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isRemotesFromOriginalSource() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getMainClass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, String> getProperties() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isModules() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getRemoteJars() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ArtifactRepository> getRepositories() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RepositorySystemSession getRepoSession() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RepositorySystem getRepoSystem() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isIncludeForkerUpdaterRuntimeModules() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getClassPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getModulePath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MavenProject getProject() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MavenSession getSession() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, ArtifactRepositoryLayout> getRepositoryLayouts() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DependencyResolver getDependencyResolver() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public org.apache.maven.repository.RepositorySystem getRepositorySystem() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<RemoteRepository> getRepos() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isLinkJVM() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getSplash() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getVmArgs() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getUpdaterArgs() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isUseArgFile() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getInstallLocation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getForkerArgs() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getLauncherScriptName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getToolExecutable(String toolName) throws IOException {
		return toolName;
	}

	@Override
	public boolean calcIncludeProject() {
		return false;
	}

	@Override
	public Set<Launcher> getAllLaunchers() {
		return null;
	}

	@Override
	public File getForkerArgsFile() {
		return null;
	}

	@Override
	public Path resolveLaunchersSrcPath() {
		return null;
	}

	@Override
	public Map<String, String> getSystemProperties() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isLink() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<String> getLinkOptions() {
		return null;
	}
}