package com.sshtools.forker.updater;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Node;

import com.sshtools.forker.updater.AppManifest.Section;
import com.sshtools.forker.updater.AppManifest.Type;
import com.sshtools.forker.wrapper.Replace;

public class Entry {
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
	private Set<String> architecture = new LinkedHashSet<>();
	private Set<String> os = new LinkedHashSet<>();
	private AppManifest manifest;
	
	protected Logger logger = Logger.getGlobal();

	public Entry(Path path, AppManifest manifest) throws IOException {
		this.manifest = manifest;
		size = Files.size(path);
		checksum = AppManifest.checksum(path);
		read = Files.isReadable(path);
		write = Files.isWritable(path);
		execute = Files.isExecutable(path);
		try {
			permissions = Files.getPosixFilePermissions(path);
		} catch (Exception e) {
		}
		if (Files.isSymbolicLink(path))
			target = Files.readSymbolicLink(path);
	}

	public Entry(Section section, Replace replace, Node file, AppManifest manifest)
			throws IOException, URISyntaxException {
		this.manifest = manifest;
		this.section = section;
		
		path = Paths.get(AppManifest.getRequiredAttribute(replace, file, "path"));
		String targetStr = AppManifest.getAttribute(replace, file, "target");
		if (StringUtils.isNotBlank(targetStr)) {
			target = Paths.get(targetStr);
		} else {
			uri = new URI(AppManifest.getRequiredAttribute(replace, file, "uri"));
			size = Long.parseLong(AppManifest.getRequiredAttribute(replace, file, "size"));
			checksum = Long.parseLong(AppManifest.getRequiredAttribute(replace, file, "checksum"), 16);
			read = !"false".equals(AppManifest.getAttribute(replace, file, "read"));
			architecture = toSet(AppManifest.getAttribute(replace, file, "architecture"));
			os = toSet(AppManifest.getAttribute(replace, file, "os"));
			String permString = AppManifest.getAttribute(replace, file, "permissions");
			if (permString != null) {
				if (permString.startsWith("-")) {
					permissions = new LinkedHashSet<>();
					if (permString.length() > 1 && permString.charAt(1) == 'r') {
						permissions.add(PosixFilePermission.OWNER_READ);
					}
					if (permString.length() > 2 && permString.charAt(2) == 'w') {
						permissions.add(PosixFilePermission.OWNER_WRITE);
					}
					if (permString.length() > 3 && permString.charAt(3) == 'x') {
						permissions.add(PosixFilePermission.OWNER_EXECUTE);
					}
					if (permString.length() > 4 && permString.charAt(4) == 'r') {
						permissions.add(PosixFilePermission.GROUP_READ);
					}
					if (permString.length() > 5 && permString.charAt(5) == 'w') {
						permissions.add(PosixFilePermission.GROUP_WRITE);
					}
					if (permString.length() > 6 && permString.charAt(6) == 'x') {
						permissions.add(PosixFilePermission.GROUP_EXECUTE);
					}
					if (permString.length() > 7 && permString.charAt(7) == 'r') {
						permissions.add(PosixFilePermission.OTHERS_READ);
					}
					if (permString.length() > 8 && permString.charAt(8) == 'w') {
						permissions.add(PosixFilePermission.OTHERS_WRITE);
					}
					if (permString.length() > 9 && permString.charAt(9) == 'x') {
						permissions.add(PosixFilePermission.OTHERS_EXECUTE);
					}
				} else {
					String[] perms = permString.split(",");
					permissions = new LinkedHashSet<>();
					for (String s : perms) {
						try {
							permissions.add(PosixFilePermission.valueOf(s));
						} catch (Exception e) {
						}
					}
					write = permissions.contains(PosixFilePermission.OWNER_WRITE);
					execute = permissions.contains(PosixFilePermission.OWNER_EXECUTE);
				}
			} else {
				write = !"false".equals(AppManifest.getAttribute(replace, file, "write"));
				execute = !"false".equals(AppManifest.getAttribute(replace, file, "execute"));
			}
		}
		if ("true".equals(AppManifest.getAttribute(replace, file, "modulepath"))) {
			type = Type.MODULEPATH;
		} else if ("true".equals(AppManifest.getAttribute(replace, file, "classpath"))) {
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

	public Set<String> architecture() {
		return architecture;
	}

	public Set<String> os() {
		return os;
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

	public Entry name(Path path) {
		this.path = path.getFileName();
		return this;
	}

	public Entry path(Path path) {
		if(!path.isAbsolute())
			path =  Paths.get("/").resolve(path);
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
		if (path.isAbsolute()) {
			return localDir.resolve(path.toString().substring(1));
		}
		else {
			Path sectionPath = manifest.resolve(section, localDir);
			if(type == Type.CLASSPATH) {
				return sectionPath.resolve(manifest.classPath()).resolve(path);
			}
			else if(type == Type.MODULEPATH) {
				return sectionPath.resolve(manifest.modulePath()).resolve(path);
			}
			else {
				return sectionPath.resolve(path);
			}
		}
	}

	@Override
	public String toString() {
		return "Entry [uri=" + uri + ", path=" + path + ", size=" + size + ", checksum=" + checksum + ", type=" + type
				+ ", section=" + section + "]";
	}

	public static Set<String> toSet(String attribute) {
		if (attribute == null || attribute.equals(""))
			return Collections.emptySet();
		else
			return new LinkedHashSet<>(Arrays.asList(attribute.split(",")));
	}

}