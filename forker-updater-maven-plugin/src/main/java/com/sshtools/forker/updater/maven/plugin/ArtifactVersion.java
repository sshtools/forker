package com.sshtools.forker.updater.maven.plugin;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

public class ArtifactVersion {

	private final static Map<String, String> versions = new HashMap<>();

	public static String getVersion(String artifactId) {
		String fakeVersion = Boolean.getBoolean("forker.development")
				? System.getProperty("forker.development.version", System.getProperty("forker.devVersion"))
				: null;
		if (fakeVersion != null) {
			return fakeVersion;
		}

		String detectedVersion = versions.get(artifactId);
		if (detectedVersion != null)
			return detectedVersion;

		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		if (cl == null)
			cl = ArtifactVersion.class.getClassLoader();

		// try to load from maven properties first
		if (detectedVersion == null || detectedVersion.isBlank()) {
			try {
				Properties p = new Properties();
				InputStream is = cl
						.getResourceAsStream("META-INF/maven/" + artifactId + "/pom.properties");
				if (is == null) {
					is = ArtifactVersion.class
							.getResourceAsStream("/META-INF/maven/" + artifactId + "/pom.properties");
				}
				if (is != null) {
					p.load(is);
					detectedVersion = p.getProperty("version", "");
				}
			} catch (Exception e) {
				// ignore
			}
		}

		// fallback to using Java API
		if (detectedVersion == null || detectedVersion.isBlank()) {
			Package aPackage = ArtifactVersion.class.getPackage();
			if (aPackage != null) {
				detectedVersion = aPackage.getImplementationVersion();
				if (detectedVersion == null) {
					detectedVersion = aPackage.getSpecificationVersion();
				}
			}
		}

		if (detectedVersion == null || detectedVersion.isBlank()) {
			try {
				DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
				Document doc = docBuilder.parse(new File("pom.xml"));
				detectedVersion = doc.getDocumentElement().getElementsByTagName("version").item(0).getTextContent();
			} catch (Exception e) {
			}

		}

		if (detectedVersion == null || detectedVersion.isBlank()) {
			detectedVersion = "0.0.0-SNAPSHOT";
		}

		versions.put(artifactId, detectedVersion);

		return detectedVersion;
	}
}
