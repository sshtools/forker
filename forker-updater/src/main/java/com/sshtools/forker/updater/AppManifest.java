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
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import com.sshtools.forker.updater.Replace.Replacer;

public class AppManifest {

	public enum Type {
		CLASSPATH, MODULEPATH, OTHER
	}

	public static class Entry {
		private URI uri;
		private Path path;
		private long size;
		private long checksum;
		private Type type;

		public Entry(Path file) throws IOException {
			size = Files.size(file);
			checksum = AppManifest.checksum(file);
		}

		Entry(Replace replace, Node file, AppManifest manifest) throws IOException, URISyntaxException {
			uri = new URI(getRequiredAttribute(replace, file, "uri"));
			path = manifest.path().resolve(getRequiredAttribute(replace, file, "path"));
			size = Long.parseLong(getRequiredAttribute(replace, file, "size"));
			checksum = Long.parseLong(getRequiredAttribute(replace, file, "checksum"), 16);
			if ("true".equals(getAttribute(replace, file, "modulepath"))) {
				type = Type.MODULEPATH;
			} else if ("true".equals(getAttribute(replace, file, "classpath"))) {
				type = Type.CLASSPATH;
			} else {
				type = Type.OTHER;
			}
		}

		public URI uri() {
			return uri;
		}

		public Entry uri(URI uri) {
			this.uri = uri;
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

	}

	private Instant timestamp = Instant.now();
	private URI baseUri;
	private Map<String, String> properties = new HashMap<>();
	private Path path;
	private List<Entry> entries = new ArrayList<>();

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
			if (!root.getNodeName().equalsIgnoreCase("configuration")) {
				throw new IOException("The root should be a configuration element.");
			}
			timestamp = Instant.parse(getRequiredAttribute(replace, root, "timestamp"));

			/* Properties */
			NodeList properties = document.getElementsByTagName("properties");
			if (properties.getLength() == 1) {
				properties = properties.item(0).getChildNodes();
				for (int i = 0; i < properties.getLength(); i++) {
					Node property = properties.item(i);
					if (property instanceof Element) {
						if (((Element) property).getTagName().equals("property"))
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
			NodeList base = document.getElementsByTagName("base");
			if (base.getLength() == 1) {
				baseUri = new URI(getRequiredAttribute(replace, base.item(0), "uri"));
				path = Paths.get(getRequiredAttribute(replace, base.item(0), "path"));
			} else
				throw new IOException("Should only be a single base element.");

			/* Files */
			NodeList files = document.getElementsByTagName("files");
			if (files.getLength() == 1) {
				files = files.item(0).getChildNodes();
				for (int i = 0; i < files.getLength(); i++) {
					Node file = files.item(i);
					if (file instanceof Element) {
						if (((Element) file).getTagName().equals("file"))
							entries.add(new Entry(replace, file, this));
						else
							throw new IOException("The files tag can only contain file tags.");
					}
				}
			} else if (files.getLength() > 1)
				throw new IOException("Should only be a single properties element if it exists.");
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

	public List<Entry> entries() {
		return entries;
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
			Element rootElement = doc.createElement("configuration");
			rootElement.setAttribute("timestamp", timestamp.toString());

			Element baseElement = doc.createElement("base");
			if (baseUri != null)
				baseElement.setAttribute("uri", baseUri.toString());
			if (path != null)
				baseElement.setAttribute("path", path.toString());
			rootElement.appendChild(baseElement);

			if (properties != null && properties.size() > 0) {
				Element propertiesElement = doc.createElement("properties");
				for (String key : properties.keySet()) {
					Element propElement = doc.createElement("property");
					propElement.setAttribute("key", key);
					propElement.setAttribute("value", properties.get(key));
					propertiesElement.appendChild(propElement);
				}
				rootElement.appendChild(propertiesElement);
			}

			Element filesElement = doc.createElement("files");
			for (Entry e : entries) {
				Element fileElement = doc.createElement("file");
				fileElement.setAttribute("uri", baseUri.relativize(e.uri).toString());
				fileElement.setAttribute("path", e.path().toString());
				fileElement.setAttribute("size", String.valueOf(e.size()));
				fileElement.setAttribute("checksum", Long.toHexString(e.checksum()));
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
				filesElement.appendChild(fileElement);
			}
			rootElement.appendChild(filesElement);
			doc.appendChild(rootElement);

			return doc;
		} catch (ParserConfigurationException e) {
			throw new IOException("Failed to create manifest.", e);
		}

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

}
