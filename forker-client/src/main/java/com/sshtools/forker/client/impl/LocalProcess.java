package com.sshtools.forker.client.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import com.sshtools.forker.client.EffectiveUser;
import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.client.ForkerProcess;
import com.sshtools.forker.common.IO;

/**
 * This uses Java's {@link ProcessBuilder} to wrap a standard {@link Process}.
 *
 */
public class LocalProcess extends ForkerProcess {

	private Process nativeProcess;

	/**
	 * Constructor
	 * 
	 * @param builder
	 *            builder
	 * @throws IOException on any error
	 */
	public LocalProcess(ForkerBuilder builder) throws IOException {
		IO io = builder.getCommand().getIO();
		if (!io.isLocal())
			throw new IOException(String.format("IO mode %s currently requires the daemon.", io));
		EffectiveUser effectiveUser = builder.effectiveUser();
		if (effectiveUser != null) {
			effectiveUser.elevate(builder, null, builder.getCommand());
		}
		try {
			List<String> allArguments = builder.getCommand().getAllArguments();
			ProcessBuilder pb = new ProcessBuilder(allArguments);
			if (builder.getCommand().isRedirectError()) {
				pb.redirectErrorStream(true);
			}
			if (builder.getCommand().getDirectory() != null) {
				pb.directory(builder.getCommand().getDirectory());
			}
			if (builder.getCommand().getEnvironment() != null) {
				pb.environment().putAll(builder.getCommand().getEnvironment());
			}
			nativeProcess = pb.start();
		} finally {
			if (effectiveUser != null) {
				effectiveUser.descend(builder, null, builder.getCommand());
			}
		}
	}

	@Override
	public ProcessHandle toHandle() {
		return nativeProcess.toHandle();
	}

	@Override
	public Process destroyForcibly() {
		return nativeProcess.destroyForcibly();
	}

	@Override
	public boolean supportsNormalTermination() {
		return nativeProcess.supportsNormalTermination();
	}

	@Override
	public OutputStream getOutputStream() {
		return nativeProcess.getOutputStream();
	}

	@Override
	public InputStream getInputStream() {
		return nativeProcess.getInputStream();
	}

	@Override
	public InputStream getErrorStream() {
		return nativeProcess.getErrorStream();
	}

	@Override
	public int waitFor() throws InterruptedException {
		return nativeProcess.waitFor();
	}

	@Override
	public int exitValue() {
		return nativeProcess.exitValue();
	}

	@Override
	public void destroy() {
		nativeProcess.destroy();
	}

}
