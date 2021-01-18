package com.sshtools.forker.updater;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.LinkedHashSet;
import java.util.Set;

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
	
	public Entry(Section section, Replace replace, Node file, AppManifest manifest)
			throws IOException, URISyntaxException {
		this.section = section;
		path = Paths.get(AppManifest.getRequiredAttribute(replace, file, "path"));
		String targetStr = AppManifest.getAttribute(replace, file, "target");
		if(StringUtils.isNotBlank(targetStr)) {
			target = Paths.get(targetStr);
		}
		else {
			uri = new URI(AppManifest.getRequiredAttribute(replace, file, "uri"));
			size = Long.parseLong(AppManifest.getRequiredAttribute(replace, file, "size"));
			checksum = Long.parseLong(AppManifest.getRequiredAttribute(replace, file, "checksum"), 16);
			write = !"false".equals(AppManifest.getAttribute(replace, file, "write"));
			execute = !"false".equals(AppManifest.getAttribute(replace, file, "execute"));
			read = !"false".equals(AppManifest.getAttribute(replace, file, "read"));
			String permString = AppManifest.getAttribute(replace, file, "permissions");
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