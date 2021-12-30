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
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.transfer.dependencies.resolve.DependencyResolver;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import com.sshtools.forker.updater.AppManifest;

public interface Context {

	Log getLog();

	Business getBusiness();

	Bootstrap getBootstrap();

	AppManifest getManifest();

	Path getImagePath();

	String getRemoteBase();

	Path getRepositoryPath();

	boolean isRepository();

	boolean isImage();

	boolean isRemotesFromOriginalSource();

	String getMainClass();

	Map<String, String> getProperties();

	boolean isModules();

	String getRemoteJars();

	List<ArtifactRepository> getRepositories();

	RepositorySystemSession getRepoSession();

	RepositorySystem getRepoSystem();

	boolean isIncludeForkerUpdaterRuntimeModules();

	String getClassPath();

	String getModulePath();

	MavenProject getProject();

	MavenSession getSession();

	Map<String, ArtifactRepositoryLayout> getRepositoryLayouts();

	DependencyResolver getDependencyResolver();

	org.apache.maven.repository.RepositorySystem getRepositorySystem();

	List<RemoteRepository> getRepos();

	boolean isLinkJVM();

	String getSplash();

	List<String> getVmArgs();

	List<String> getUpdaterArgs();

	boolean isUseArgFile();

	String getInstallLocation();

	List<String> getForkerArgs();

	String getLauncherScriptName();
	
	String getToolExecutable(final String toolName) throws IOException;

	boolean calcIncludeProject();

	Set<Launcher> getAllLaunchers();

	File getForkerArgsFile();

	Path resolveLaunchersSrcPath();

	Map<String, String> getSystemProperties();

	boolean isLink();

	List<String> getLinkOptions();
}
