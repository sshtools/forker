package com.sshtools.forker.pty;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;

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
import com.sshtools.forker.common.Command;
import com.sshtools.forker.common.IO;
import com.sshtools.forker.common.IO.DefaultIO;
import com.sshtools.forker.common.States;
import com.sshtools.forker.daemon.CommandExecutor;
import com.sshtools.forker.daemon.ExecuteCheckResult;
import com.sshtools.forker.daemon.Forker;
import com.sshtools.forker.daemon.InputThread;
import com.sshtools.forker.daemon.OutputThread;
import com.sun.jna.Platform;

/**
 * A {@link CommandExecutor} that can execute shells and other commands with a
 * Pseudo Terminal, or PTY. This uses a modified version of the
 * <a href="https://github.com/traff/pty4j">Pty4J</a> project available
 * <a href="https://github.com/brett-smith/pty4j">here</a>.
 * <p>
 * You would not need to use this class directly, to create a PTY for your
 * process, use ForkerBuilder with an I/O mode of {@link #PTY}.
 *
 */
public class PTYExecutor implements CommandExecutor {

	public final static class PTYIO extends DefaultIO {
		public PTYIO() {
			super("PTY", false, true, true, true);
		}
	}

	public final static IO PTY = new PTYIO();

	public PTYExecutor() throws ClassNotFoundException {
		/*
		 * HACK! Make sure the classes are loaded now so that there are not
		 * problems when it's forked
		 */
		Class.forName(PtyProcess.class.getName(), true, PTYExecutor.class.getClassLoader());
		Class.forName(OutputThread.class.getName(), true, PTYExecutor.class.getClassLoader());
		Class.forName(InputThread.class.getName(), true, PTYExecutor.class.getClassLoader());
		Class.forName(PTYOutputStream.class.getName(), true, PTYExecutor.class.getClassLoader());
		Class.forName(WinSize.class.getName(), true, PTYExecutor.class.getClassLoader());
		Class.forName(PtyUtil.class.getName(), true, PTYExecutor.class.getClassLoader());
		Class.forName(PtyUtil.class.getName() + "$1", true, PTYExecutor.class.getClassLoader());
		Class.forName(Pty.class.getName(), true, PTYExecutor.class.getClassLoader());

		if (SystemUtils.IS_OS_UNIX) {
			Class.forName(PtyHelpers.class.getName(), true, PTYExecutor.class.getClassLoader());
			Class.forName(UnixPtyProcess.class.getName(), true, PTYExecutor.class.getClassLoader());
			Class.forName(UnixPtyProcess.class.getName() + "$Reaper", true, PTYExecutor.class.getClassLoader());
		} else if (SystemUtils.IS_OS_WINDOWS) {
			Class.forName(WinPtyProcess.class.getName(), true, PTYExecutor.class.getClassLoader());
			Class.forName(WinPty.class.getName(), true, PTYExecutor.class.getClassLoader());
		}
		Class.forName(Pair.class.getName(), true, PTYExecutor.class.getClassLoader());

	}

	@Override
	public ExecuteCheckResult willHandle(Forker forker, Command command) {
		return command.getIO().equals(PTY) ? ExecuteCheckResult.YES : ExecuteCheckResult.DONT_CARE;
	}

	@Override
	public void handle(Forker forker, DataInputStream din, final DataOutputStream dout, Command cmd)
			throws IOException {
		try {

			// Change the EUID before we fork
			int euid = -1;
			if (!StringUtils.isBlank(cmd.getRunAs())) {
				if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC_OSX) {
					euid = Integer.parseInt(cmd.getRunAs());
				}
			}

			PtyProcess ptyorig = null;
			// If Windows, and we are starting a shell, strip this commands
			List<String> arguments = cmd.getAllArguments();
			if (Platform.isWindows() && arguments.size() > 2 && arguments.get(0).equals("start")
					&& arguments.get(1).equals("/c") && arguments.get(2).equals("CMD.exe")) {
				arguments.remove(0);
				arguments.remove(0);
			}

			ptyorig = PtyProcess.exec((String[]) arguments.toArray(new String[0]), cmd.getEnvironment(),
					cmd.getDirectory().getAbsolutePath(), euid);
			final PtyProcess pty = ptyorig;

			// The JVM is now forked, so free up some resources we won't
			// actually use
			forker.setForked();
			;
			// forker.socket.close();

			InputStream in = pty.getInputStream();
			OutputStream out = pty.getOutputStream();
			final InputStream err = pty.getErrorStream();

			WinSize winSize = pty.getWinSize();
			int width = winSize == null ? 80 : winSize.ws_col;
			int height = winSize == null ? 24 : winSize.ws_row;

			dout.writeInt(States.WINDOW_SIZE);
			dout.writeInt(width);
			dout.writeInt(height);
			OutputThread outThread = new OutputThread(dout, cmd, err);
			outThread.start();

			// Take any input coming the other way
			InputThread inThread = new InputThread(out, din) {

				@Override
				public void kill() {
					pty.destroy();
				}

				@Override
				public void setWindowSize(int width, int height) {
					pty.setWinSize(new WinSize(width, height));
				}
			};
			inThread.start();
			Forker.readStreamToOutput(dout, in, States.IN);
			synchronized (dout) {
				dout.writeInt(States.END);
				dout.writeInt(pty.waitFor());
				dout.flush();
			}

			// Wait for stream other end to close
			inThread.join();
		} catch (Throwable t) {
			synchronized (dout) {
				dout.writeInt(States.FAILED);
				dout.writeUTF(t.getMessage());
			}
		}

	}

}
