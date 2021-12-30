package com.sshtools.forker.updater.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;

public class UpdaterUtil {

	public static String indent(int... amounts) {
		int a = 0;
		for (int i : amounts) {
			a += i;
		}
		a *= 4;
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < a; i++)
			b.append(' ');
		return b.toString();
	}
	
	public static String getExtension(String fileName) {
		int idx = fileName.indexOf('.');
		if (idx == -1)
			throw new IllegalArgumentException(String.format("Filename %s must have extension.", fileName));
		return fileName.substring(idx + 1);
	}

	public static String escapeSpaces(String str) {
		return str.replace(" ", "\\\\ ");
	}
	
	public static Path resolvePath(Path pathObj, String path) {
		return path == null || path.equals("") ? pathObj : pathObj.resolve(path);
	}

	public static String mavenUrl(String base, String groupId, String artifactId, String version, String classifier) {
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

	public static Path checkFilesDir(Path resolve) {
		checkDir(resolve.getParent());
		return resolve;
	}

	public static Path checkDir(Path resolve) {
		if (!Files.exists(resolve)) {
			try {
				Files.createDirectories(resolve);
			} catch (IOException e) {
				throw new IllegalStateException(String.format("Failed to create %s.", resolve));
			}
		}
		return resolve;
	}

//	public static boolean isModuleJar(org.eclipse.aether.artifact.Artifact a) throws IOException {
//		if ("jar".equals(a.getExtension())) {
//			try (JarFile jarFile = new JarFile(a.getFile())) {
//				Enumeration<JarEntry> enumOfJar = jarFile.entries();
//				Manifest mf = jarFile.getManifest();
//				if (mf != null) {
//					if (mf.getMainAttributes().getValue("Automatic-Module-Name") != null)
//						return true;
//				}
//				while (enumOfJar.hasMoreElements()) {
//					JarEntry entry = enumOfJar.nextElement();
//					if (entry.getName().equals("module-info.class")
//							|| entry.getName().matches("META-INF/versions/.*/module-info.class")) {
//						return true;
//					}
//				}
//			}
//		}
//		return false;
//	}

	public static String getFileName(boolean includeVersion, Artifact a) {
		return getFileName(includeVersion, a.getArtifactId(), a.getVersion(), a.getClassifier(), a.getType());
	}

	public static String getFileName(boolean includeVersion, org.eclipse.aether.artifact.Artifact a) {
		return getFileName(includeVersion, a.getArtifactId(), a.getVersion(), a.getClassifier(), a.getExtension());
	}

	public static String getFileName(boolean includeVersion, String artifactId, String version, String classifier,
			String type) {
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

	public static boolean isForkerUpdaterBootstrap(org.eclipse.aether.artifact.Artifact a) {
		return a.getArtifactId().equals("forker-updater")
				|| (a.getArtifactId().startsWith("forker-updater-")
						&& !a.getArtifactId().equals("forker-updater-example"))
				|| a.getArtifactId().equals("forker-client") || a.getArtifactId().equals("forker-common")
				|| a.getArtifactId().equals("forker-wrapper") || a.getArtifactId().equals("jna-platform")
				|| a.getArtifactId().equals("jna") || a.getArtifactId().equals("picocli");
	}

	public static boolean isForkerUpdaterRuntime(org.eclipse.aether.artifact.Artifact a) {
		return a.getArtifactId().equals("forker-updater") || a.getArtifactId().equals("forker-common")
				|| a.getArtifactId().equals("forker-client") || a.getArtifactId().equals("forker-wrapped")
				|| a.getArtifactId().equals("jna-platform") || a.getArtifactId().equals("jna")
				|| a.getArtifactId().equals("picocli");
	}

	public static String normalizeForUri(String localPath) {
		return localPath.replace(File.separator, "/");
	}

	public static List<String> concat(List<String> l1, List<String> l2) {
		List<String> l = new ArrayList<>();
		l.addAll(l1);
		l.addAll(l2);
		return l;
	}
	public static Set<String> concat(Set<String> set1, Set<String> set2) {
		Set<String> s = new LinkedHashSet<>();
		s.addAll(set1);
		s.addAll(set2);
		return s;
	}

	public static void copy(int indent, Log log, String reason, Path p1, Path p2, Instant mod) throws IOException {
		log.info(String.format(indent(indent) + "Copy %s - %s to %s", reason, p1.toAbsolutePath(), p2.toAbsolutePath()));
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

	public static boolean isRemote(String path) {
		return path != null && (path.startsWith("http:") || path.startsWith("https:"));
	}

	public static boolean isUrl(String path) {
		return isRemote(path) || (path != null && path.startsWith("file:"));
	}

	public static String resolveUrl(String base, String path) {
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
