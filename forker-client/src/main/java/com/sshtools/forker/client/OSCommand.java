package com.sshtools.forker.client;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import com.sshtools.forker.common.IO;

/**
 * Some helper methods for running commands and doing common things, like
 * capturing the output to a string, elevating privileges using sudo etc.
 */
public class OSCommand {

    final static Logger LOG = Logger.getLogger(OSCommand.class.getSimpleName());

    private static ThreadLocal<Boolean> elevated = new ThreadLocal<Boolean>();

    private static char[] sudoPassword = System.getProperty("vm.sudo") == null ? null : System.getProperty("vm.sudo").toCharArray();

    public static void sudo(char[] password) {
        sudoPassword = password;
    }

    public static void elevate() {
        elevated.set(Boolean.TRUE);
    }

    public static void restrict() {
        elevated.set(Boolean.FALSE);
    }

    public static int runCommandAndOutputToFile(File sqlFile, String... args) throws IOException {
        return runCommandAndOutputToFile(null, sqlFile, args);
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

    public static Collection<String> runCommandAndCaptureOutput(String... sargs) throws IOException {
        return runCommandAndCaptureOutput(null, sargs);
    }

    public static Collection<String> runCommandAndCaptureOutput(File cwd, String... sargs) throws IOException {
        File askPass = null;
        try {
            List<String> args = new ArrayList<String>(Arrays.asList(sargs));
            askPass = getAskPass(args);
            LOG.fine("Running command: " + StringUtils.join(args, " "));
            ForkerBuilder pb = new ForkerBuilder(args);
            pb.io(IO.INPUT);
            if (askPass != null) {
                pb.environment().put("SUDO_ASKPASS", askPass.getAbsolutePath());
            }
            if (cwd != null) {
                pb.directory(cwd);
            }
            Process p = pb.start();
            Collection<String> lines = IOUtils.readLines(p.getInputStream());
            try {
                int ret = p.waitFor();
                if (ret != 0) {
                    throw new IOException("Command '" + StringUtils.join(args, " ") + "' returned non-zero status. Returned " + ret
                                    + ". " + StringUtils.join(lines, "\n"));
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

    public static void run(String... args) throws IOException {
        run(null, args);
    }

    public static void run(File cwd, String... args) throws IOException {
        int ret = runCommand(cwd, args);
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

    public static void run(List<String> args) throws IOException, Exception {
        run((File) null, args);
    }

    public static void run(File cwd, List<String> args) throws IOException, Exception {
        run(cwd, null, args);
    }

    public static void run(OutputStream out, List<String> args) throws IOException, Exception {
        run((File) null, out, args);
    }

    public static void run(File cwd, OutputStream out, List<String> args) throws IOException, Exception {
        Process process = doCommand(cwd, args, out);
        if (process.exitValue() != 0) {
            throw new Exception("Update process exited with status " + process.exitValue() + ". See log for more details.");
        }
    }

    public static int runCommand(OutputStream out, List<String> args) throws IOException {
        return runCommand((File) null, out, (String[]) args.toArray(new String[0]));
    }

    public static int runCommand(List<String> args) throws IOException {
        return runCommand((String[]) args.toArray(new String[0]));
    }

    public static int runCommand(String... args) throws IOException {
        return runCommand(null, args);
    }

    public static int runCommand(File cwd, String... args) throws IOException {
        return runCommand(cwd, System.out, args);
    }

    public static int runCommand(File cwd, OutputStream out, String... sargs) throws IOException {
        LOG.fine("Running command: " + StringUtils.join(sargs, " "));
        List<String> args = new ArrayList<String>(Arrays.asList(sargs));
        File askPass = getAskPass(args);
        ForkerBuilder pb = new ForkerBuilder(args);
        pb.io(IO.INPUT);
        Map<String, String> environment = pb.environment();
        if (askPass != null) {
            environment.put("SUDO_ASKPASS", askPass.getAbsolutePath());
        }
        if (cwd != null) {
            pb.directory(cwd);
        }
        pb.redirectErrorStream(true);
        Process p = pb.start();
        IOUtils.copy(p.getInputStream(), out);
        try {
            return p.waitFor();
        } catch (InterruptedException e) {
        	LOG.log(Level.SEVERE, "Command interrupted.", e);
            return -999;
        }
    }

    public static Process doCommand(List<String> args) throws IOException {
        return doCommand((File) null, args);
    }

    public static Process doCommand(File cwd, List<String> args) throws IOException {
        return doCommand(cwd, args, null);
    }

    public static Process doCommand(List<String> args, OutputStream out) throws IOException {
        return doCommand((File) null, args, out);
    }

    public static Process doCommand(File cwd, List<String> args, OutputStream out) throws IOException {
        File askPass = null;
        try {
            args = new ArrayList<String>(args);
            askPass = getAskPass(args);

            LOG.fine("Running command: " + StringUtils.join(args, " "));
            ForkerBuilder builder = new ForkerBuilder(args);
            builder.io(IO.INPUT);
            Map<String, String> environment = builder.environment();
            // TODO this REALLY should not be here
            environment.put("DEBIAN_FRONTEND", "noninteractive");
            if (askPass != null) {
                environment.put("SUDO_ASKPASS", askPass.getAbsolutePath());
            }
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
        } finally {
            if (askPass != null) {
                askPass.delete();
            }
        }
    }

    private static File getAskPass(List<String> args) throws IOException {
        // TODO this is so incredibly unsafe, but until this is a product who
        // cares
        if (Boolean.TRUE.equals(elevated.get()) && !isSuperUser()) {
            if (sudoPassword != null) {
                args.add(0, "sudo");
                args.add(1, "-A");
                
                // Preserve environment (may have permission implications)
                args.add(1, "-E");
                
                File askPass = File.createTempFile("sudo", "tmp");
                askPass.deleteOnExit();
                PrintWriter pw = new PrintWriter(new FileOutputStream(askPass));
                try {
                    pw.println("#!/bin/sh\necho '" + new String(sudoPassword) + "'");
                } finally {
                    pw.close();
                }
                askPass.setExecutable(true);
                return askPass;
            } else {
                throw new IOException("Command " + args.get(0)
                                + " requires elevated permissions, but vm.sudo system property has not been set.");
            }
        }
        return null;
    }

	private static boolean isSuperUser() {
		return System.getProperty("user.name").equals(System.getProperty("vm.rootUser", "root"));
	}

    private static void readInput(BufferedReader reader) throws IOException {
        String line = null;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
    }

}
