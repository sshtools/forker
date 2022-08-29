package com.sshtools.forker.updater.maven.plugin;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.plugin.logging.Log;

import com.sshtools.forker.client.OSCommand;
import com.sun.jna.Platform;

public class SelfExtractingExecutableBuilder {

	private Path output;
	private Path script;
	private Path image;
	private Log log;

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
		if(!Files.exists(output))
			Files.createDirectories(output);
		
		if (Platform.isWindows() ? OSCommand.hasCommand("makensisw") : OSCommand.hasCommand("makensis")) {
			Path artifactName = output.getFileName();
			Path nsisDir = image;
			Path nsisPath = nsisDir.resolve(artifactName.toString() + ".nsis");
			try {
				try (PrintWriter w = new PrintWriter(
						Files.newBufferedWriter(nsisPath))) {
					w.println("#");
					w.println(String.format("OutFile \"%s\"",
							output.resolve(artifactName.toString() + ".exe")));
					w.println();
					w.println("# set desktop as install directory");
					w.println("InstallDir $TEMP");
					w.println();
					w.println("# default section start; every NSIS script has at least one section.");
					w.println("Section");
					w.println();
					w.println("    SetOutPath $INSTDIR");
					
					try (DirectoryStream<Path> stream = Files.newDirectoryStream(image)) {
				        for (Path path : stream) {
				        	if(path.equals(nsisPath)) {
					            if (Files.isDirectory(path)) {
									w.println(String.format("    File /r \"%s\"", path));
					            }	
					            else {
									w.println(String.format("    File \"%s\"", path));
					            }
				        	}
				        }
				    }

					w.println();
					w.println("# default section end.");
					w.println("SectionEnd");
				}
				
				OSCommand.runCommand(Platform.isWindows() ? "makensisw.exe" : "makensis", 
						nsisPath.toString());
				
				
			}
			finally {
				/* Clean up */
				Files.delete(nsisPath);
			}
			
			return;
		}
		throw new UnsupportedOperationException();
	}

	public Log log() {
		return log;
	}

	public SelfExtractingExecutableBuilder log(Log log) {
		this.log = log;
		return this;
	}
}
