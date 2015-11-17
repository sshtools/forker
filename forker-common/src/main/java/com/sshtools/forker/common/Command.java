package com.sshtools.forker.common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Command {
    private List<String> arguments = new ArrayList<String>();
    private boolean redirectError;
    private File directory;
    private Map<String, String> environment;
    private String runAs = "";

    public Command() {
        environment = new ProcessBuilder("dummy").environment();
    }

    public Command(DataInputStream din) throws IOException {
        int argc = din.readInt();
        for (int i = 0; i < argc; i++) {
            arguments.add(din.readUTF());
        }
        directory = new File(din.readUTF());
        redirectError = din.readBoolean();
        boolean hasEnv = din.readBoolean();
        if (hasEnv) {
            environment = new HashMap<String, String>();
            int envc = din.readInt();
            for (int i = 0; i < envc; i++) {
                environment.put(din.readUTF(), din.readUTF());
            }
        }
        String r = din.readUTF();
        runAs = r.equals("") ? null : r;
    }

    public boolean isRedirectError() {
        return redirectError;
    }

    public void setRedirectError(boolean redirectError) {
        this.redirectError = redirectError;
    }

    public File getDirectory() {
        return directory;
    }

    public void setDirectory(File directory) {
        this.directory = directory;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
    }

    public String getRunAs() {
        return runAs;
    }

    public void setRunAs(String runAs) {
        this.runAs = runAs;
    }

    public List<String> getArguments() {
        return arguments;
    }

    public void write(DataOutputStream dout) throws IOException {
        dout.writeInt(arguments.size());
        for (String s : arguments) {
            dout.writeUTF(s);
        }
        dout.writeUTF(directory == null ? System.getProperty("user.dir") : directory.getAbsolutePath());
        dout.writeBoolean(redirectError);
        dout.writeBoolean(environment != null);
        if (environment != null) {
            dout.writeInt(environment.size());
            for (Map.Entry<String, String> en : environment.entrySet()) {
                dout.writeUTF(en.getKey());
                dout.writeUTF(en.getValue());
            }
        }
        dout.writeUTF(runAs == null ? "" : runAs);
    }

    @Override
    public String toString() {
        return "Command [arguments=" + arguments + ", redirectError=" + redirectError + ", directory=" + directory
                        + ", environment=" + environment + ", runAs=" + runAs + "]";
    }
}