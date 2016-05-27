package com.sshtools.forker.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang.StringUtils;

import com.sshtools.forker.client.EffectiveUserFactory.SudoFixedPasswordAdministrator;
import com.sshtools.forker.common.IO;

/**
 * Some helper methods for running commands and doing common things, like
 * capturing the output to a string, elevating privileges using sudo etc.
 */
public class OSCommand {

	final static Logger LOG = Logger.getLogger(OSCommand.class.getSimpleName());

	private static ThreadLocal<Boolean> elevated = new ThreadLocal<Boolean>();
	private static ThreadLocal<Map<String, String>> environment = new ThreadLocal<Map<String, String>>();

	private static char[] sudoPassword = null;

	public static void sudo(char[] password) {
		sudoPassword = password;
	}

	public static void elevate() {
		elevated.set(Boolean.TRUE);
	}

	public static Map<String, String> environment() {
		Map<String, String> env = environment.get();
		if (env == null) {
			env = new HashMap<>();
			environment.set(env);
		}
		return env;
	}

	public static void environment(Map<String, String> env) {
		environment.set(env);
	}

	public static void restrict() {
		elevated.set(Boolean.FALSE);
	}

	public static int adminCommandAndOutputToFile(File sqlFile, String... args) throws IOException {
		return adminCommandAndOutputToFile(null, sqlFile, args);
	}

	public static int runCommandAndOutputToFile(File sqlFile, String... args) throws IOException {
		return runCommandAndOutputToFile(null, sqlFile, args);
	}

	public static int adminCommandAndOutputToFile(File cwd, File sqlFile, String... args) throws IOException {
		elevate();
		try {
			return runCommandAndOutputToFile(cwd, sqlFile, args);
		} finally {
			restrict();
		}
	}

	public static int runCommandAndOutputToFile(File cwd, File sqlFile, String... args) throws IOException {
		LOG.fine("Running command: " + StringUtils.join(args, " ") + " > " + sqlFile);
		FileOutputStream fos = new FileOutputStream(sqlFile);
		try {
			ForkerBuilder pb = new ForkerBuilder(args);
			pb.io(IO.INPUT);
			if (cwd != null) {
				pb.directory(cwd);
			}
			checkElevationAndEnvironment(pb);
			Process p = pb.start();
			IOUtils.copy(p.getInputStream(), fos);
			try {
				return p.waitFor();
			} catch (InterruptedException e) {
				LOG.log(Level.SEVERE, "Command interrupted.", e);
				throw new IOException(e);
			}
		} finally {
			fos.close();
		}
	}

	public static Collection<String> adminCommandAndCaptureOutput(String... sargs) throws IOException {
		return adminCommandAndCaptureOutput(null, sargs);
	}

	public static Collection<String> runCommandAndCaptureOutput(String... sargs) throws IOException {
		return runCommandAndCaptureOutput(null, sargs);
	}

	public static Collection<String> adminCommandAndCaptureOutput(File cwd, String... sargs) throws IOException {
		elevate();
		try {
			return runCommandAndCaptureOutput(cwd, sargs);
		} finally {
			restrict();
		}
	}

	public static Collection<String> runCommandAndCaptureOutput(File cwd, String... sargs) throws IOException {
		File askPass = null;
		try {
			List<String> args = new ArrayList<String>(Arrays.asList(sargs));
			LOG.fine("Running command: " + StringUtils.join(args, " "));
			ForkerBuilder pb = new ForkerBuilder(args);
			if(pb.io() == null)
				pb.io(IO.INPUT);
			checkElevationAndEnvironment(pb);
			if (cwd != null) {
				pb.directory(cwd);
			}
			Process p = pb.start();
			Collection<String> lines = IOUtils.readLines(p.getInputStream());
			try {
				int ret = p.waitFor();
				if (ret != 0) {
					throw new IOException("Command '" + StringUtils.join(args, " ")
							+ "' returned non-zero status. Returned " + ret + ". " + StringUtils.join(lines, "\n"));
				}
			} catch (InterruptedException e) {
				LOG.log(Level.SEVERE, "Command interrupted.", e);
				throw new IOException(e);
			}
			return lines;
		} finally {
			if (askPass != null) {
				askPass.delete();
			}
		}
	}

	private static void checkElevationAndEnvironment(ForkerBuilder pb) {
		Map<String, String> env = environment.get();
		if (env != null) {
			pb.environment().putAll(env);
		}

		if (Boolean.TRUE.equals(elevated.get())) {
			if (Forker.isDaemonRunning()) {
				pb.effectiveUser(new EffectiveUserFactory.POSIXUIDEffectiveUser(0));
			} else {
				pb.effectiveUser(sudoPassword == null ? EffectiveUserFactory.getDefault().administrator()
						: new SudoFixedPasswordAdministrator(sudoPassword));
			}
		}
		else {
			if(Forker.isDaemonRunningAsAdministrator())
				pb.effectiveUser(new EffectiveUserFactory.POSIXUsernameEffectiveUser(System.getProperty("user.name")));
		}
	}

	public static void admin(String... args) throws IOException {
		admin(null, args);
	}

	public static void run(String... args) throws IOException {
		run(null, args);
	}

	public static void admin(File cwd, String... args) throws IOException {
		int ret = adminCommand(cwd, args);
		if (ret != 0) {
			throw new IOException("Command returned non-zero status '" + ret + "'.");
		}
		;
	}

	public static void run(File cwd, String... args) throws IOException {
		int ret = runCommand(cwd, args);
		if (ret != 0) {
			throw new IOException("Command returned non-zero status '" + ret + "'.");
		}
		;
	}

	public static void admin(File cwd, OutputStream out, String... sargs) throws IOException {
		int ret = adminCommand(cwd, out, sargs);
		if (ret != 0) {
			throw new IOException("Command returned non-zero status '" + ret + "'.");
		}
		;
	}

	public static void run(File cwd, OutputStream out, String... sargs) throws IOException {
		int ret = runCommand(cwd, out, sargs);
		if (ret != 0) {
			throw new IOException("Command returned non-zero status '" + ret + "'.");
		}
		;
	}

	public static void admin(List<String> args) throws IOException, Exception {
		admin((File) null, args);
	}

	public static void run(List<String> args) throws IOException, Exception {
		run((File) null, args);
	}

	public static void admin(File cwd, List<String> args) throws IOException, Exception {
		admin(cwd, null, args);
	}

	public static void run(File cwd, List<String> args) throws IOException, Exception {
		run(cwd, null, args);
	}

	public static void admin(OutputStream out, List<String> args) throws IOException, Exception {
		admin((File) null, out, args);
	}

	public static void run(OutputStream out, List<String> args) throws IOException, Exception {
		run((File) null, out, args);
	}

	public static void admin(File cwd, OutputStream out, List<String> args) throws IOException, Exception {
		Process process = doAdminCommand(cwd, args, out);
		if (process.exitValue() != 0) {
			throw new Exception(
					"Update process exited with status " + process.exitValue() + ". See log for more details.");
		}
	}

	public static void run(File cwd, OutputStream out, List<String> args) throws IOException, Exception {
		Process process = doCommand(cwd, args, out);
		if (process.exitValue() != 0) {
			throw new Exception(
					"Update process exited with status " + process.exitValue() + ". See log for more details.");
		}
	}

	public static int adminCommand(OutputStream out, List<String> args) throws IOException {
		return runCommand((File) null, out, (String[]) args.toArray(new String[0]));
	}

	public static int runCommand(OutputStream out, List<String> args) throws IOException {
		return runCommand((File) null, out, (String[]) args.toArray(new String[0]));
	}

	public static int adminCommand(List<String> args) throws IOException {
		return runCommand((String[]) args.toArray(new String[0]));
	}

	public static int runCommand(List<String> args) throws IOException {
		return runCommand((String[]) args.toArray(new String[0]));
	}

	public static int adminCommand(String... args) throws IOException {
		return adminCommand(null, args);
	}

	public static int runCommand(String... args) throws IOException {
		return runCommand(null, args);
	}

	public static int adminCommand(File cwd, String... args) throws IOException {
		return adminCommand(cwd, System.out, args);
	}

	public static int runCommand(File cwd, String... args) throws IOException {
		return runCommand(cwd, System.out, args);
	}

	public static int adminCommand(File cwd, OutputStream out, String... sargs) throws IOException {
		elevate();
		try {
			return runCommand(cwd, out, sargs);
		} finally {
			restrict();
		}
	}

	public static int runCommand(File cwd, OutputStream out, String... sargs) throws IOException {
		LOG.fine("Running command: " + StringUtils.join(sargs, " "));
		List<String> args = new ArrayList<String>(Arrays.asList(sargs));
		ForkerBuilder pb = new ForkerBuilder(args);
		if(pb.io() == null)
			pb.io(IO.INPUT);
		checkElevationAndEnvironment(pb);
		if (cwd != null) {
			pb.directory(cwd);
		}
		pb.redirectErrorStream(true);
		Process p = pb.start();
		IOUtils.copy(p.getInputStream(), out == null ? new NullOutputStream() : out);
		try {
			return p.waitFor();
		} catch (InterruptedException e) {
			LOG.log(Level.SEVERE, "Command interrupted.", e);
			return -999;
		}
	}

	public static Process doAdminCommand(List<String> args) throws IOException {
		return doAdminCommand((File) null, args);
	}

	public static Process doCommand(List<String> args) throws IOException {
		return doCommand((File) null, args);
	}

	public static Process doAdminCommand(File cwd, List<String> args) throws IOException {
		return doAdminCommand(cwd, args, null);
	}

	public static Process doCommand(File cwd, List<String> args) throws IOException {
		return doCommand(cwd, args, null);
	}

	public static Process doAdminCommand(List<String> args, OutputStream out) throws IOException {
		return doAdminCommand((File) null, args, out);
	}

	public static Process doCommand(List<String> args, OutputStream out) throws IOException {
		return doCommand((File) null, args, out);
	}

	public static Process doAdminCommand(File cwd, List<String> args, OutputStream out) throws IOException {
		elevate();
		try {
			return doCommand(cwd, args, out);
		} finally {
			restrict();
		}
	}

	public static Process doCommand(File cwd, List<String> args, OutputStream out) throws IOException {
		args = new ArrayList<String>(args);

		LOG.fine("Running command: " + StringUtils.join(args, " "));
		ForkerBuilder builder = new ForkerBuilder(args);
		if(builder.io() == null)
			builder.io(IO.INPUT);
		checkElevationAndEnvironment(builder);
		if (cwd != null) {
			builder.directory(cwd);
		}
		builder.redirectErrorStream(true);
		Process process = builder.start();
		InputStream inputStream = process.getInputStream();
		try {
			if (out == null) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
				readInput(reader);
			} else {
				out = new FilterOutputStream(out) {
					@Override
					public void write(int b) throws IOException {
						super.write(b);
						System.out.print((char) b);
					}

					@Override
					public void write(byte[] b, int off, int len) throws IOException {
						super.write(b, off, len);
						System.out.print(new String(b, off, len));
					}

				};
				IOUtils.copy(inputStream, out);
			}
		} finally {
			try {
				process.waitFor();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			inputStream.close();
		}
		return process;
	}

	private static void readInput(BufferedReader reader) throws IOException {
		String line = null;
		while ((line = reader.readLine()) != null) {
			System.out.println(line);
		}
	}

}
