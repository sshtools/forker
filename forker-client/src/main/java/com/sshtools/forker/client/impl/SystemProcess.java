package com.sshtools.forker.client.impl;

import java.io.InputStream;
import java.io.OutputStream;

import com.sshtools.forker.client.AbstractOSProcess;
import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.common.CSystem;

public class SystemProcess extends AbstractOSProcess {

    private Thread thread;
    private int exitValue;

    public SystemProcess(final ForkerBuilder builder) {
        thread = new Thread("SystemProcess" + builder.command()) {
            public void run() {
                exitValue = CSystem.INSTANCE.system(buildCommand(builder));
            };
        };
        thread.start();
    }

    @Override
    public OutputStream getOutputStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getInputStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getErrorStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int waitFor() throws InterruptedException {
        thread.join();
        return exitValue;
    }

    @Override
    public int exitValue() {
        return exitValue;
    }

    @Override
    public void destroy() {
        throw new UnsupportedOperationException();
    }

}