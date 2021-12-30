package com.sshtools.forker.updater.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;
import org.apache.maven.plugin.logging.Log;

public class SelfExtractingExecutableBuilder {

	private Path output; 
	private Path script;
	private Path image;
	private String name;
	private Context context;
	
	public Context context() {
		return context;
	}
	
	public SelfExtractingExecutableBuilder context(Context context) {
		this.context = context;
		return this;
	}

	public String name() {
		return name;
	}

	public SelfExtractingExecutableBuilder name(String name) {
		this.name = name;
		return this;
	}

	public Path output() {
		return output;
	}

	public SelfExtractingExecutableBuilder output(Path output) {
		this.output = output;
		return this;
	}

	public Path script() {
		return script;
	}

	public SelfExtractingExecutableBuilder script(Path script) {
		this.script = script;
		return this;
	}

	public Path image() {
		return image;
	}

	public SelfExtractingExecutableBuilder image(Path image) {
		this.image = image;
		return this;
	}

	public void make() throws IOException {
		if (!Files.exists(output.getParent()))
			Files.createDirectories(output.getParent());
		
		Log log = context.getLog();

		Path tmp = Files.createTempDirectory("frk");
		log.info(String.format("Create temporary directory for Graal Self Extracting launcher at %s.", tmp));

		/* Zip of image */
		Path data = tmp.resolve("data.zip");
		try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(data))) {
			for (Path path : Files.list(image).collect(Collectors.toList())) {
				zipFile(path, path.getFileName().toString(), out);
			}
		}

		/* Properties containing launcher script */
		log.info("Adding launcher properties.");
		Properties p = new Properties();
		if(script != null)
			p.setProperty(SelfExtractor.STARTUP_SCRIPT, script.toString());
		try (Writer w = Files.newBufferedWriter(tmp.resolve("data.properties"))) {
			p.store(w, "ForkerUpdater");
		}

		/* Copy the self extractor too */
		log.info("Copying self extractor.");
		Path sxClassPath = tmp.resolve(SelfExtractor.class.getName().replace(".", "/") + ".class");
		Files.createDirectories(sxClassPath.getParent());
		try (OutputStream out = Files.newOutputStream(sxClassPath)) {
			JavaClass sx = Repository.lookupClass(SelfExtractor.class);
			sx.dump(out);
		} catch (ClassNotFoundException e) {
			throw new IOException("Failed to find self extractor.", e);
		}

		/*
		 * Now use Graal to generative a native executable embedding the zip archive,
		 * properties and the extractor.
		 */
		log.info("Linking self extractor.");
		ProcessBuilder pb = new ProcessBuilder(context.getToolExecutable("native-image"), SelfExtractor.class.getName(),
				output.toString(), "-H:IncludeResources=data\\.properties", "-H:IncludeResources=data\\.zip");
		pb.redirectError(Redirect.INHERIT);
		pb.redirectInput(Redirect.INHERIT);
		pb.redirectOutput(Redirect.INHERIT);
		pb.directory(tmp.toFile());
		Process pr = pb.start();
		try {
			if (pr.waitFor() != 0)
				throw new IOException("Link failed with exit code " + pr.exitValue());
		} catch (InterruptedException ie) {
			throw new IOException("Interrupted.");
		}

		/* Clean up tmp */
		log.info("Cleaning up temporary files.");
		try (Stream<Path> walk = Files.walk(tmp)) {
			walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
		}
	}

//	private String locateGraalCommand(String cmd) {
//		String graal = System.getenv("GRAAL_HOME");
//		if (StringUtils.isNotBlank(graal)) {
//			Path cmdPath = Paths.get(graal).resolve("bin").resolve(SystemUtils.IS_OS_WINDOWS ? cmd + ".exe" : cmd);
//			if (Files.exists(cmdPath)) {
//				return cmdPath.toString();
//			}
//		}
//		String path = System.getenv("PATH");
//		if (StringUtils.isNotBlank(path)) {
//			for (String p : path.split(File.pathSeparator)) {
//				Path dp = Paths.get(p);
//				Path cmdPath = dp.resolve(SystemUtils.IS_OS_WINDOWS ? cmd + ".exe" : cmd);
//				if (Files.exists(cmdPath)) {
//					return cmdPath.toString();
//				}
//			}
//		}
//		throw new IllegalStateException("Cannot find Graal command " + cmd + ". Either set GRAAL_HOME or add to PATH.");
//	}

	private static void zipFile(Path fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
		if (Files.isHidden(fileToZip)) {
			return;
		}
		if (Files.isDirectory(fileToZip)) {
			if (fileName.endsWith("/")) {
				putNextEntry(zipOut, new ZipEntry(fileName), fileToZip);
				zipOut.closeEntry();
			} else {
				putNextEntry(zipOut, new ZipEntry(fileName + "/"), fileToZip);
				zipOut.closeEntry();
			}
			for (Path path : Files.list(fileToZip).collect(Collectors.toList())) {
				zipFile(path, fileName + "/" + path.getFileName(), zipOut);
			}
			return;
		}
		try (InputStream fis = Files.newInputStream(fileToZip)) {
			ZipEntry zipEntry = new ZipEntry(fileName);
			putNextEntry(zipOut, zipEntry, fileToZip);
			fis.transferTo(zipOut);
		}
	}

	private static void putNextEntry(ZipOutputStream zipOut, ZipEntry zipEntry, Path path) throws IOException {
		StringBuilder extra = new StringBuilder();
		extra.append(Files.isReadable(path) ? "R" : "r");
		extra.append(Files.isWritable(path) ? "W" : "w");
		extra.append(Files.isExecutable(path) ? "X" : "x");
		if(Files.isSymbolicLink(path)) {
			Path lpath = Files.readSymbolicLink(path);
			String lpathstr = lpath.toString();
			extra.append("L");
			extra.append(String.format("%04d", lpathstr.length()));
			extra.append(lpathstr);
		}
		zipEntry.setExtra(extra.toString().getBytes("UTF-8"));
		zipOut.putNextEntry(zipEntry);		
	}

}
