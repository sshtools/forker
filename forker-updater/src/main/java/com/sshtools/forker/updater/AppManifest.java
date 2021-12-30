package com.sshtools.forker.updater;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Adler32;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sshtools.forker.updater.Launcher.Scope;
import com.sshtools.forker.wrapper.Replace;
import com.sshtools.forker.wrapper.Replace.Replacer;

public class AppManifest {

	protected Logger logger = Logger.getGlobal();

	public enum ManifestVersion {
		V1, V2
	}

	public static String APP_TAG = "app";
	public static String APP_FILE_TAG = "appFile";
	public static String BOOTSTRAP_TAG = "bootstrap";
	public static String BOOTSTRAP_FILE_TAG = "bootstrapFile";
	public static String PROPERTIES_TAG = "properties";
	public static String PROPERTY_TAG = "property";
	public static String ARGUMENT_TAG = "argument";
	public static String CONFIGURATION_TAG = "configuration";
	public static String LAUNCHERS_TAG = "launchers";
	public static String SERVICE_TAG = "service";
	public static String SHORTCUT_TAG = "shortcut";
	public static String BASE_TAG = "base";

	public enum Type {
		CLASSPATH, MODULEPATH, OTHER
	}

	public enum Section {
		APP, BOOTSTRAP, OTHER
	}

	private Instant timestamp = Instant.now();
	private URI baseUri;
	private Map<String, String> properties = new HashMap<>();
	private List<Entry> entries = new ArrayList<>();
	private String version;
	private String id;
	private ManifestVersion manifestVersion = ManifestVersion.V2;
	private Map<Section, Path> sectionPath = new HashMap<>();
	private Path basePath = Paths.get("/");
	private Path modulePath = Paths.get("modulepath");
	private Path classPath = Paths.get("classpath");
	private List<Launcher> launchers = new ArrayList<>();

	public AppManifest() {
	}

	public AppManifest(Path path) throws IOException {
		try (Reader reader = Files.newBufferedReader(path)) {
			load(reader);
		}
	}

	public ManifestVersion manfifestVersion() {
		return manifestVersion;
	}

	public void load(InputStream in) throws IOException {
		doLoad(new InputSource(in));
	}

	public void load(Reader in) throws IOException {
		doLoad(new InputSource(in));
	}

	private void doLoad(InputSource source) throws IOException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Replace replace = new Replace();
		replace.pattern(Replace.DEFAULT_VARIABLE_REPLACEMENT, new Replacer() {

			@Override
			public String replace(Pattern pattern, Matcher matcher, String replacementPattern) throws Exception {
				String match = matcher.group();
				String key = match.substring(2, match.length() - 1);

				String val = properties.get(key);
				if (val == null)
					val = System.getProperty(key);
				return val;
			}
		});
		try {
			dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			dbf.setValidating(false);
			dbf.setNamespaceAware(true);
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document document = db.parse(source);

			Node root = document.getFirstChild();
			if (!root.getNodeName().equalsIgnoreCase(CONFIGURATION_TAG)) {
				throw new IOException("The root should be a configuration element.");
			}
			timestamp = Instant.parse(getRequiredAttribute(replace, root, "timestamp"));
			version = getAttribute(replace, root, "version");
			id = getRequiredAttribute(replace, root, "id");
			String manififestVerString = getAttribute(replace, root, "manifest");
			if (manififestVerString != null && manififestVerString.length() > 0) {
				try {
					manifestVersion = ManifestVersion.valueOf("V" + manififestVerString);
				} catch (Exception e) {
					/* Assume default */
					manifestVersion = ManifestVersion.values()[ManifestVersion.values().length - 1];
				}
			}

			/* Properties */
			NodeList properties = document.getElementsByTagName(PROPERTIES_TAG);
			if (properties.getLength() == 1) {
				properties = properties.item(0).getChildNodes();
				for (int i = 0; i < properties.getLength(); i++) {
					Node property = properties.item(i);
					if (property instanceof Element) {
						if (((Element) property).getTagName().equals(PROPERTY_TAG))
							this.properties.put(getRequiredAttribute(replace, property, "key"),
									getRequiredAttribute(replace, property, "value"));
						else
							throw new IOException("The properties tag can only contain property tags, not "
									+ ((Element) property).getTagName() + ".");
					}
				}
			} else if (properties.getLength() > 1)
				throw new IOException("Should only be a single properties element if it exists.");

			/* Launchers */
			NodeList launchers = document.getElementsByTagName(LAUNCHERS_TAG);
			if (launchers.getLength() == 1) {
				launchers = launchers.item(0).getChildNodes();
				for (int i = 0; i < launchers.getLength(); i++) {
					Node property = launchers.item(i);
					if (property instanceof Element) {
						Launcher launcher = null;

						if (((Element) property).getTagName().equals(SERVICE_TAG)) {
							Service srv = new Service(getRequiredAttribute(replace, property, "id"));
							launcher = srv;
						} else if (((Element) property).getTagName().equals(SHORTCUT_TAG)) {
							Shortcut shortcut = new Shortcut(getRequiredAttribute(replace, property, "id"));
							launcher = shortcut;
						} else
							throw new IOException("The launchers tag can only contain service tags, not "
									+ ((Element) property).getTagName() + ".");

						launcher.scope(Scope.valueOf(getAttribute(replace, property, "scope", Scope.USER.name())));
						launcher.description(getAttribute(replace, property, "description"));
						launcher.executable(getAttribute(replace, property, "executable"));
						
						NodeList arguments = property.getChildNodes();
						List<String> args = new ArrayList<>();
						for (int j = 0; j < arguments.getLength(); j++) {
							Node argument = arguments.item(j);
							if (argument instanceof Element) {
								Element argumentElement = (Element) argument;
								if (argumentElement.getTagName().equals(ARGUMENT_TAG))
									args.add(getTextContent(replace, argumentElement));
								else
									throw new IOException("A launcher tag can only contain argument tags, not "
											+ ((Element) property).getTagName() + ".");
							}
						}
						launcher.arguments(args);

						this.launchers.add(launcher);
					}
				}
			} else if (launchers.getLength() > 1)
				throw new IOException("Should only be a single properties element if it exists.");

			/* Base */
			NodeList base = document.getElementsByTagName(BASE_TAG);
			if (base.getLength() == 1) {
				baseUri = new URI(getRequiredAttribute(replace, base.item(0), "uri"));

				/*
				 * An attempt to be backward and forward compatible. For releases AFTER
				 * 1.0-SNAPSHOT-24, the manifest was made a bit more consistent, but this breaks
				 * backward compatibility so an update would not be possible.
				 * 
				 * The manifest will now contain the new attributes and the old attributes. The
				 * old attributes will be removed at some point in the future.
				 */
				String pathStr = getAttribute(replace, base.item(0), "path", null);
				String basePathStr = getAttribute(replace, base.item(0), "basepath", null);
				if (basePathStr == null) {
					basePath = Paths.get("/");
					if (pathStr != null) {
						sectionPath(Section.APP, Paths.get(pathStr));
						sectionPath(Section.BOOTSTRAP, basePath);
					}
				} else {
					basePath = Paths.get(basePathStr);
				}

				modulePath = Paths.get(getAttribute(replace, base.item(0), "modulepath", "modulepath"));
				classPath = Paths.get(getAttribute(replace, base.item(0), "classpath", "classpath"));
			} else
				throw new IOException("Should only be a single base element.");

			/* Files */
			NodeList files = document.getElementsByTagName(APP_TAG);
			if (files.getLength() == 1) {
				Node app = files.item(0);
				String path = getAttribute(replace, app, "path");
				if (path != null)
					sectionPath.put(Section.APP, Paths.get(path));
				files = app.getChildNodes();
				for (int i = 0; i < files.getLength(); i++) {
					Node file = files.item(i);
					if (file instanceof Element) {
						if (((Element) file).getTagName().equals(APP_FILE_TAG))
							entries.add(new Entry(Section.APP, replace, file, this));
						else
							throw new IOException("The app tag can only contain appFile tags.");
					}
				}
			} else if (files.getLength() > 1)
				throw new IOException("Should only be a single app element if it exists.");

			/* Bootstrap Files */
			NodeList bootstrapFiles = document.getElementsByTagName(BOOTSTRAP_TAG);
			if (bootstrapFiles.getLength() == 1) {
				Node bootstrap = bootstrapFiles.item(0);
				String path = getAttribute(replace, bootstrap, "path");
				if (path != null)
					sectionPath.put(Section.BOOTSTRAP, Paths.get(path));
				bootstrapFiles = bootstrap.getChildNodes();
				for (int i = 0; i < bootstrapFiles.getLength(); i++) {
					Node bootstrapFile = bootstrapFiles.item(i);
					if (bootstrapFile instanceof Element) {
						if (((Element) bootstrapFile).getTagName().equals(BOOTSTRAP_FILE_TAG))
							entries.add(new Entry(Section.BOOTSTRAP, replace, bootstrapFile, this));
						else
							throw new IOException("The bootstrap tag can only contain bootstrapFile tags.");
					}
				}
			} else if (bootstrapFiles.getLength() > 1)
				throw new IOException("Should only be a single bootstrap element if it exists.");
		} catch (URISyntaxException | ParserConfigurationException | SAXException e) {
			throw new IOException("Failed to load manifiest.", e);
		}
	}

	static String getRequiredAttribute(Replace replace, Node item, String name) throws IOException {
		String val = getAttribute(replace, item, name);
		if (val == null)
			throw new IOException(String.format("Attribute %s is required.", name));
		return val;
	}

	static String getAttribute(Replace replace, Node item, String name) {
		return getAttribute(replace, item, name, null);
	}

	static String getAttribute(Replace replace, Node item, String name, String defaultValue) {
		Node attr = item.getAttributes().getNamedItem(name);
		return attr == null ? defaultValue : replace.replace(attr.getNodeValue());
	}

	static String getTextContent(Replace replace, Element item) {
		return getTextContent(replace, item, "");
	}
	static String getTextContent(Replace replace, Element item, String defaultValue) {
		return replace.replace(( item.getTextContent() == null ? defaultValue : item.getTextContent() ) );
	}

	public Path modulePath() {
		return modulePath;
	}

	public AppManifest modulePath(Path modulePath) {
		this.modulePath = modulePath;
		return this;
	}

	public Path classPath() {
		return classPath;
	}

	public AppManifest classPath(Path classPath) {
		this.classPath = classPath;
		return this;
	}

	public String id() {
		return id;
	}

	public AppManifest id(String id) {
		this.id = id;
		return this;
	}

	public List<Entry> entries() {
		return entries;
	}

	public List<Entry> entries(Section section) {
		List<Entry> l = new ArrayList<>();
		for (Entry e : entries) {
			if (e.section().equals(section)) {
				l.add(e);
			}
		}
		return Collections.unmodifiableList(l);
	}

	public String version() {
		return version;
	}

	public AppManifest version(String version) {
		this.version = version;
		return this;
	}

	public Instant timestamp() {
		return timestamp;
	}

	public AppManifest timestamp(Instant timestamp) {
		this.timestamp = timestamp;
		return this;
	}

	public URI baseUri() {
		return baseUri;
	}

	public AppManifest baseUri(URI baseUri) {
		this.baseUri = baseUri;
		return this;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public AppManifest basePath(Path basePath) {
		this.basePath = basePath;
		return this;
	}

	public void save(OutputStream out) throws IOException {
		save(new OutputStreamWriter(out, "UTF-8"));
	}

	public void save(Writer out) throws IOException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer;
		try {
			transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			DOMSource source = new DOMSource(getManifestAsDocument());
			StreamResult result = new StreamResult(out);
			transformer.transform(source, result);
		} catch (TransformerException e) {
			throw new IOException("Failed to save manifest.", e);
		}
	}

	private Document getManifestAsDocument() throws IOException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.newDocument();
			Element rootElement = doc.createElement(CONFIGURATION_TAG);
			rootElement.setAttribute("timestamp", timestamp.toString());
			rootElement.setAttribute("id", id);
			if (version != null && !version.equals(""))
				rootElement.setAttribute("version", version);
			rootElement.setAttribute("manifest", manifestVersion.name().substring(1));

			Element baseElement = doc.createElement(BASE_TAG);
			if (baseUri != null)
				baseElement.setAttribute("uri", baseUri.toString());

			baseElement.setAttribute("path",
					sectionPath.getOrDefault(Section.APP, Paths.get("app/business")).toString());
			if (basePath != null)
				baseElement.setAttribute("basepath", basePath.toString());
			if (modulePath != null)
				baseElement.setAttribute("modulepath", modulePath.toString());
			if (classPath != null)
				baseElement.setAttribute("classpath", classPath.toString());
			rootElement.appendChild(baseElement);

			if (properties != null && properties.size() > 0) {
				Element propertiesElement = doc.createElement(PROPERTIES_TAG);
				for (String key : properties.keySet()) {
					Element propElement = doc.createElement(PROPERTY_TAG);
					propElement.setAttribute("key", key);
					propElement.setAttribute("value", properties.get(key));
					propertiesElement.appendChild(propElement);
				}
				rootElement.appendChild(propertiesElement);
			}

			if (launchers != null && launchers.size() > 0) {
				Element launchersElement = doc.createElement(LAUNCHERS_TAG);
				for (Launcher launcher : launchers) {
					Element launcherElement;
					if (launcher instanceof Service) {
						launcherElement = doc.createElement(SERVICE_TAG);
					} else if (launcher instanceof Shortcut) {
						launcherElement = doc.createElement(SHORTCUT_TAG);
					} else {
						throw new UnsupportedOperationException(
								"Unsupported launcher type " + launcher.getClass().getName());
					}
					launcherElement.setAttribute("id", launcher.id());
					launcherElement.setAttribute("executable", launcher.executable());
					if (launcher.description() != null)
						launcherElement.setAttribute("description", launcher.description());
					if (launcher.getArguments() != null) {
						for (String arg : launcher.getArguments()) {
							Element launcherArgElement = doc.createElement(ARGUMENT_TAG);
							launcherElement.setAttribute("value", arg);
							launcherElement.appendChild(launcherArgElement);
						}
					}
					launchersElement.appendChild(launcherElement);
				}
				rootElement.appendChild(launchersElement);
			}

			Element filesElement = doc.createElement(APP_TAG);
			Path path = sectionPath(Section.APP);
			if (path != null)
				filesElement.setAttribute("path", path.toString());
			for (Entry e : entries(Section.APP)) {
				addFileElements(doc, filesElement, e, APP_FILE_TAG);
			}
			rootElement.appendChild(filesElement);

			Element bootstrapsElement = doc.createElement(BOOTSTRAP_TAG);
			path = sectionPath(Section.BOOTSTRAP);
			if (path != null)
				bootstrapsElement.setAttribute("path", path.toString());
			for (Entry e : entries(Section.BOOTSTRAP)) {
				addFileElements(doc, bootstrapsElement, e, BOOTSTRAP_FILE_TAG);
			}
			rootElement.appendChild(bootstrapsElement);

			doc.appendChild(rootElement);

			return doc;
		} catch (ParserConfigurationException e) {
			throw new IOException("Failed to create manifest.", e);
		}

	}

	private void addFileElements(Document doc, Element filesElement, Entry e, String name) {
		Element fileElement = doc.createElement(name);
		if (e.section() == Section.BOOTSTRAP && (e.type() == Type.CLASSPATH || e.type() == Type.MODULEPATH)) {
			switch (e.type()) {
			case CLASSPATH:
				fileElement.setAttribute("path",
						sectionPath(Section.BOOTSTRAP).resolve(classPath).resolve(e.path()).toString());
				break;
			default:
				fileElement.setAttribute("path",
						sectionPath(Section.BOOTSTRAP).resolve(modulePath).resolve(e.path()).toString());
				break;
			}
		} else if (e.section() == Section.APP && (e.type() == Type.CLASSPATH || e.type() == Type.MODULEPATH)) {
			switch (e.type()) {
			case CLASSPATH:
				fileElement.setAttribute("path", classPath.resolve(e.path()).toString());
				break;
			default:
				fileElement.setAttribute("path", modulePath.resolve(e.path()).toString());
				break;
			}
		} else
			fileElement.setAttribute("path", e.path().toString());
		fileElement.setAttribute("filepath", e.path().toString());
		switch (e.type()) {
		case CLASSPATH:
			fileElement.setAttribute("classpath", "true");
			break;
		case MODULEPATH:
			fileElement.setAttribute("modulepath", "true");
			break;
		default:
			break;
		}
		if (e.target() != null)
			fileElement.setAttribute("target", e.target().toString());
		else {
			if (!e.architecture().isEmpty()) {
				fileElement.setAttribute("architecture", String.join(",", e.architecture()));
			}
			if (!e.os().isEmpty()) {
				fileElement.setAttribute("os", String.join(",", e.os()));
			}
			fileElement.setAttribute("uri", baseUri.relativize(e.uri()).toString());
			fileElement.setAttribute("size", String.valueOf(e.size()));
			if (!e.read())
				fileElement.setAttribute("read", "false");
			if (e.permissions() != null) {
				StringBuilder b = new StringBuilder("-");
				if (e.permissions().contains(PosixFilePermission.OWNER_READ))
					b.append("r");
				else
					b.append("-");
				if (e.permissions().contains(PosixFilePermission.OWNER_WRITE))
					b.append("w");
				else
					b.append("-");
				if (e.permissions().contains(PosixFilePermission.OWNER_EXECUTE))
					b.append("x");
				else
					b.append("-");
				if (e.permissions().contains(PosixFilePermission.GROUP_READ))
					b.append("r");
				else
					b.append("-");
				if (e.permissions().contains(PosixFilePermission.GROUP_WRITE))
					b.append("w");
				else
					b.append("-");
				if (e.permissions().contains(PosixFilePermission.GROUP_EXECUTE))
					b.append("x");
				else
					b.append("-");
				if (e.permissions().contains(PosixFilePermission.OTHERS_READ))
					b.append("r");
				else
					b.append("-");
				if (e.permissions().contains(PosixFilePermission.OTHERS_WRITE))
					b.append("w");
				else
					b.append("-");
				if (e.permissions().contains(PosixFilePermission.OTHERS_EXECUTE))
					b.append("x");
				else
					b.append("-");
				fileElement.setAttribute("rwx", b.toString());
			}

			/*
			 * DEPRECATED: Remove/Move when the new manifest handling makes it out into the
			 * wild
			 */
			fileElement.setAttribute("execute", String.valueOf(e.execute()));
			fileElement.setAttribute("write", String.valueOf(e.write()));

			fileElement.setAttribute("checksum", Long.toHexString(e.checksum()));
		}
		filesElement.appendChild(fileElement);

	}

	public static URI concat(URI uri, URI other) {
		try {
			if (other.isAbsolute())
				return other;
			else if (uri.getPath() == null || other.getPath().startsWith("/")) {
				return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), other.getPath(),
						other.getQuery(), other.getFragment());
			} else {
				return new URI(
						uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath()
								+ (uri.getPath() != null && uri.getPath().endsWith("/") ? "" : "/") + other.getPath(),
						other.getQuery(), other.getFragment());
			}
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Failed to construct URI.", e);
		}
	}

	public static long checksum(Path path) throws IOException {
		try (InputStream input = Files.newInputStream(path)) {
			Adler32 checksum = new Adler32();
			byte[] buf = new byte[1024 * 8];

			int read;
			while ((read = input.read(buf, 0, buf.length)) > -1)
				checksum.update(buf, 0, read);

			return checksum.getValue();
		}
	}

	public boolean hasVersion() {
		return version != null && version.length() > 0;
	}

	public boolean hasPath(Path fileName) {
		for (Entry e : entries)
			if (e.path().equals(fileName))
				return true;
		return false;
	}

	public Path sectionPath(Section section) {
		return sectionPath.get(section);
	}

	public AppManifest sectionPath(Section section, Path path) {
		sectionPath.put(section, path);
		return this;
	}

	public Path basePath() {
		return basePath;
	}

	public Path resolveBasePath(Path localDir) {
		return basePath == null || basePath.toString().equals("/") ? localDir : localDir.resolve(basePath);
	}

	public Path resolve(Section section, Path localDir) {
		if (basePath != null) {
			localDir = resolveBasePath(localDir);
		}
		if (sectionPath.containsKey(section)) {
			localDir = localDir.resolve(sectionPath.get(section));
		}
		return localDir;
	}

	public static void main(String[] args) {
		AppManifest mf = new AppManifest();
		Path p1 = Paths.get("/home/tanktarta");
		Path p2 = Paths.get("/another/path");
		mf.basePath = p1;
		System.out.println("p1: " + p1 + " p2: " + p2 + " = " + mf.resolveBasePath(p1));
	}

	public Map<Section, Path> sectionPaths() {
		return sectionPath;
	}

}
