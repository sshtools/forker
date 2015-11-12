package com.sshtools.forker.client;

import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;

import com.sshtools.forker.common.IO;

/**
 * Replacement for {@link Runtime#exec(String)} and friends.
 * 
 * @see ForkerBuilder
 *
 */
public class Forker {

    private final static Forker INSTANCE = new Forker();

    public static Forker get() {
        return INSTANCE;
    }

    public Process exec(IO io, String command) throws IOException {
        return exec(io, command, null, null);
    }

    public Process exec(IO io, String command, String[] envp) throws IOException {
        return exec(io, command, envp, null);
    }

    public Process exec(IO io, String command, String[] envp, File dir) throws IOException {
        if (command.length() == 0)
            throw new IllegalArgumentException("Empty command");

        StringTokenizer st = new StringTokenizer(command);
        String[] cmdarray = new String[st.countTokens()];
        for (int i = 0; st.hasMoreTokens(); i++)
            cmdarray[i] = st.nextToken();
        return exec(io, cmdarray, envp, dir);
    }

    public Process exec(IO io, String... cmdarray) throws IOException {
        return exec(io, cmdarray, null, null);
    }

    public Process exec(IO io, String[] cmdarray, String[] envp) throws IOException {
        return exec(io, cmdarray, envp, null);
    }

    public Process exec(IO io, String[] cmdarray, String[] envp, File dir) throws IOException {
        return new ForkerBuilder(cmdarray).io(io).environment(envp).directory(dir).start();
    }

}
