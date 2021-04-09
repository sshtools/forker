package com.sshtools.forker.pty;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.commons.lang3.SystemUtils;

import com.pty4j.PtyProcess;
import com.pty4j.WinSize;
import com.pty4j.unix.PTYOutputStream;
import com.pty4j.unix.Pty;
import com.pty4j.unix.PtyHelpers;
import com.pty4j.unix.UnixPtyProcess;
import com.pty4j.util.Pair;
import com.pty4j.util.PtyUtil;
import com.pty4j.windows.WinPty;
import com.pty4j.windows.WinPtyProcess;
import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.client.ForkerProcess;
import com.sshtools.forker.client.ForkerProcessListener;
import com.sshtools.forker.common.IO;
import com.sun.jna.Platform;

/**
 * This uses Java's {@link ProcessBuilder} to wrap a standard {@link Process}.
 *
 */
public class PTYProcess extends ForkerProcess {
	
	public interface PTYProcessListener extends ForkerProcessListener {
		void windowSizeChanged(int ptyWidth, int ptyHeight);
	}

	public final static IO PTY = new PTYIO();

	static {
		try {
			/*
			 * HACK! Make sure the classes are loaded now so that there are not problems
			 * when it's forked
			 */
			Class.forName(PtyProcess.class.getName(), true, PTYProcess.class.getClassLoader());
			Class.forName(PTYOutputStream.class.getName(), true, PTYProcess.class.getClassLoader());
			Class.forName(WinSize.class.getName(), true, PTYProcess.class.getClassLoader());
			Class.forName(PtyUtil.class.getName(), true, PTYProcess.class.getClassLoader());
			Class.forName(PtyUtil.class.getName() + "$1", true, PTYProcess.class.getClassLoader());
			Class.forName(Pty.class.getName(), true, PTYProcess.class.getClassLoader());

			if (SystemUtils.IS_OS_UNIX) {
				Class.forName(PtyHelpers.class.getName(), true, PTYProcess.class.getClassLoader());
				Class.forName(UnixPtyProcess.class.getName(), true, PTYProcess.class.getClassLoader());
				Class.forName(UnixPtyProcess.class.getName() + "$Reaper", true, PTYProcess.class.getClassLoader());
			} else if (SystemUtils.IS_OS_WINDOWS) {
				Class.forName(WinPtyProcess.class.getName(), true, PTYProcess.class.getClassLoader());
				Class.forName(WinPty.class.getName(), true, PTYProcess.class.getClassLoader());
			}
			Class.forName(Pair.class.getName(), true, PTYProcess.class.getClassLoader());
		} catch (Exception e) {
			throw new IllegalStateException("Failed to initialize.", e);
		}
	}

	private PtyProcess nativeProcess;

	/**
	 * Constructor
	 * 
	 * @param builder builder
	 * @throws IOException on any error
	 */
	public PTYProcess(ForkerBuilder builder) throws IOException {

//		EffectiveUser effectiveUser = builder.effectiveUser();
//		if (effectiveUser != null) {
//			effectiveUser.elevate(builder, null, builder.getCommand());
//		}
		try {

			// If Windows, and we are starting a shell, strip this commands
			List<String> arguments = builder.getAllArguments();
			if (Platform.isWindows() && arguments.size() > 2 && arguments.get(0).equals("start")
					&& arguments.get(1).equals("/c") && arguments.get(2).equals("CMD.exe")) {
				arguments.remove(0);
				arguments.remove(0);
			}

			nativeProcess = PtyProcess.exec((String[]) arguments.toArray(new String[0]), builder.environment(),
					builder.directory() == null ? System.getProperty("user.dir") :  builder.directory().getAbsolutePath());
		} finally {
//			if (effectiveUser != null) {
//				effectiveUser.descend(builder, null, builder.getCommand());
//			}
		}

	}

	@Override
	public ProcessHandle toHandle() {
		return ProcessHandle.of(nativeProcess.getPid()).get();
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
