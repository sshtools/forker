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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sshtools.forker.wrapper.Replace;
import com.sshtools.forker.wrapper.Replace.Replacer;

public class AppManifest {

	public static String APP_TAG = "app";
	public static String APP_FILE_TAG = "appFile";
	public static String BOOTSTRAP_TAG = "bootstrap";
	public static String BOOTSTRAP_FILE_TAG = "bootstrapFile";
	public static String PROPERTIES_TAG = "properties";
	public static String PROPERTY_TAG = "property";
	public static String CONFIGURATION_TAG = "configuration";
	public static String BASE_TAG = "base";

	public enum Type {
		CLASSPATH, MODULEPATH, OTHER
	}

	public enum Section {
		APP, BOOTSTRAP, OTHER
	}

	public static class Entry {
		private URI uri;
		private Path path;
		private long size;
		private long checksum;
		private Type type;
		private Section section = Section.APP;
		private Set<PosixFilePermission> permissions;
		private boolean write;
		private boolean execute;
		private boolean read;
		private Path target;

		public Entry(Path file) throws IOException {
			size = Files.size(file);
			checksum = AppManifest.checksum(file);
			read = Files.isReadable(file);
			write = Files.isWritable(file);
			execute = Files.isExecutable(file);
			try {
				permissions = Files.getPosixFilePermissions(path);
			} catch (Exception e) {
			}
			if(Files.isSymbolicLink(file))
				target = Files.readSymbolicLink(file); 
		}
		
		Entry(Section section, Replace replace, Node file, AppManifest manifest)
				throws IOException, URISyntaxException {
			this.section = section;
			path = Paths.get(getRequiredAttribute(replace, file, "path"));
			String targetStr = getAttribute(replace, file, "target");
			if(StringUtils.isNotBlank(targetStr)) {
				target = Paths.get(targetStr);
			}
			else {
				uri = new URI(getRequiredAttribute(replace, file, "uri"));
				size = Long.parseLong(getRequiredAttribute(replace, file, "size"));
				checksum = Long.parseLong(getRequiredAttribute(replace, file, "checksum"), 16);
				write = !"false".equals(getAttribute(replace, file, "write"));
				execute = !"false".equals(getAttribute(replace, file, "execute"));
				read = !"false".equals(getAttribute(replace, file, "read"));
				String permString = getAttribute(replace, file, "permissions");
				if (permString != null) {
					String[] perms = permString.split(",");
					permissions = new LinkedHashSet<>();
					for (String s : perms) {
						try {
							permissions.add(PosixFilePermission.valueOf(s));
						} catch (Exception e) {
						}
					}
				}
			}
			if ("true".equals(getAttribute(replace, file, "modulepath"))) {
				type = Type.MODULEPATH;
			} else if ("true".equals(getAttribute(replace, file, "classpath"))) {
				type = Type.CLASSPATH;
			} else {
				type = Type.OTHER;
			}
		}
		
		public boolean isLink() {
			return target != null;
		}

		public Path target() {
			return target;
		}

		public Entry target(Path target) {
			this.target = target;
			return this;
		}

		public boolean write() {
			return write;
		}

		public Entry write(boolean write) {
			this.write = write;
			return this;
		}

		public boolean execute() {
			return execute;
		}

		public Entry execute(boolean execute) {
			this.execute = execute;
			return this;
		}

		public boolean read() {
			return read;
		}

		public Entry read(boolean read) {
			this.read = read;
			return this;
		}

		public Set<PosixFilePermission> permissions() {
			return permissions;
		}

		public Entry permissions(Set<PosixFilePermission> permissions) {
			this.permissions = permissions;
			return this;
		}

		public URI uri() {
			return uri;
		}

		public Entry uri(URI uri) {
			this.uri = uri;
			return this;
		}

		public Section section() {
			return section;
		}

		public Entry section(Section section) {
			this.section = section;
			return this;
		}

		public Path path() {
			return path;
		}

		public Entry path(Path path) {
			this.path = path;
			return this;
		}

		public long size() {
			return size;
		}

		public Entry size(long size) {
			this.size = size;
			return this;
		}

		public long checksum() {
			return checksum;
		}

		public Entry checksum(long checksum) {
			this.checksum = checksum;
			return this;
		}

		public Type type() {
			return type;
		}

		public Entry type(Type type) {
			this.type = type;
			return this;
		}

		public Path resolve(Path localDir) {
			return resolve(localDir, null);
		}

		public Path resolve(Path localDir, Path basePath) {
			if (path.isAbsolute())
				return localDir.resolve(path.toString().substring(1));
			else if (basePath == null)
				return localDir.resolve(path);
			else
				return localDir.resolve(basePath).resolve(path.toString());
		}

		@Override
		public String toString() {
			return "Entry [uri=" + uri + ", path=" + path + ", size=" + size + ", checksum=" + checksum + ", type="
					+ type + ", section=" + section + "]";
		}

	}

	private Instant timestamp = Instant.now();
	private URI baseUri;
	private Map<String, String> properties = new HashMap<>();
	private Path path;
	private List<Entry> entries = new ArrayList<>();
	private String version;
	private String id;

	public AppManifest() {
	}

	public AppManifest(Path path) throws IOException {
		try (Reader reader = Files.newBufferedReader(path)) {
			load(reader);
		}
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

			/* Base */
			NodeList base = document.getElementsByTagName(BASE_TAG);
			if (base.getLength() == 1) {
				baseUri = new URI(getRequiredAttribute(replace, base.item(0), "uri"));
				path = Paths.get(getRequiredAttribute(replace, base.item(0), "path"));
			} else
				throw new IOException("Should only be a single base element.");

			/* Files */
			NodeList files = document.getElementsByTagName(APP_TAG);
			if (files.getLength() == 1) {
				files = files.item(0).getChildNodes();
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
				bootstrapFiles = bootstrapFiles.item(0).getChildNodes();
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

	private static String getRequiredAttribute(Replace replace, Node item, String name) throws IOException {
		String val = getAttribute(replace, item, name);
		if (val == null)
			throw new IOException(String.format("Attribute %s is required.", name));
		return val;
	}

	private static String getAttribute(Replace replace, Node item, String name) {
		Node attr = item.getAttributes().getNamedItem(name);
		return attr == null ? null : replace.replace(attr.getNodeValue());
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
		return l;
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

	public Path path() {
		return path;
	}

	public AppManifest path(Path path) {
		this.path = path;
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

			Element baseElement = doc.createElement(BASE_TAG);
			if (baseUri != null)
				baseElement.setAttribute("uri", baseUri.toString());
			if (path != null)
				baseElement.setAttribute("path", path.toString());
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

			Element filesElement = doc.createElement(APP_TAG);
			for (Entry e : entries(Section.APP)) {
				addFileElements(doc, filesElement, e, APP_FILE_TAG);
			}
			rootElement.appendChild(filesElement);

			Element bootstrapsElement = doc.createElement(BOOTSTRAP_TAG);
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
		fileElement.setAttribute("path", e.path().toString());
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
		if(e.target() != null)
			fileElement.setAttribute("target", e.target().toString());
		else {
			fileElement.setAttribute("uri", baseUri.relativize(e.uri).toString());
			fileElement.setAttribute("size", String.valueOf(e.size()));
			if (!e.read)
				fileElement.setAttribute("read", "false");
			fileElement.setAttribute("write", String.valueOf(e.write));
			if (e.permissions() != null) {
				StringBuilder b = new StringBuilder();
				for (PosixFilePermission p : e.permissions()) {
					if (b.length() > 0)
						b.append(",");
					b.append(p.name());
				}
				fileElement.setAttribute("permissions", b.toString());
			}
			fileElement.setAttribute("execute", String.valueOf(e.execute));
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
			if (e.path.equals(fileName))
				return true;
		return false;
	}

}
