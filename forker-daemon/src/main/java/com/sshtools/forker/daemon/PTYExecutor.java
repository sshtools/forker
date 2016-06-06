package com.sshtools.forker.daemon;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
import com.sshtools.forker.common.States;
import com.sun.jna.Platform;

public class PTYExecutor implements CommandExecutor {

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
		return command.getIO() == IO.PTY ? ExecuteCheckResult.YES : ExecuteCheckResult.DONT_CARE;
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
			if (Platform.isWindows() && cmd.getArguments().size() > 2 && cmd.getArguments().get(0).equals("start")
					&& cmd.getArguments().get(1).equals("/c") && cmd.getArguments().get(2).equals("CMD.exe")) {
				cmd.getArguments().remove(0);
				cmd.getArguments().remove(0);
			}

			ptyorig = PtyProcess.exec((String[]) cmd.getArguments().toArray(new String[0]), cmd.getEnvironment(),
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
				void kill() {
					pty.destroy();
				}

				@Override
				void setWindowSize(int width, int height) {
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
