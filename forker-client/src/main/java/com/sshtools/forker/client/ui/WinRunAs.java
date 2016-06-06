package com.sshtools.forker.client.ui;

import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.image.BufferedImage;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.UIManager;

import com.sshtools.forker.common.Userenv;
import com.sshtools.forker.common.Util;
import com.sshtools.forker.common.XAdvapi32;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinBase.PROCESS_INFORMATION;
import com.sun.jna.platform.win32.WinBase.SECURITY_ATTRIBUTES;
import com.sun.jna.platform.win32.WinBase.STARTUPINFO;
import com.sun.jna.platform.win32.WinNT.HANDLEByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

/**
 * A helper application to launch a command as another user.
 */
public class WinRunAs extends JFrame {

	private static final long serialVersionUID = 1L;

	public static int runAs(String username, String domain, char[] password, String[] command) throws IOException {

		// http://www.rgagnon.com/javadetails/java-start-process-as-another-user-using-jna.html
		// https://msdn.microsoft.com/en-us/library/windows/desktop/bb762270%28v=vs.85%29.aspx
		// https://msdn.microsoft.com/en-us/library/windows/desktop/ms682434%28v=vs.85%29.aspx
		// https://msdn.microsoft.com/en-us/library/windows/desktop/ms682431%28v=vs.85%29.aspx
		// http://stackoverflow.com/questions/4206190/win32-createprocess-when-is-create-unicode-environment-really-needed?rq=1

		/*
		 * Why the deadlock occurs -
		 * https://blogs.msdn.microsoft.com/oldnewthing/20110707-00/?p=10223/
		 */

		HANDLEByReference token = new HANDLEByReference();
		if (!XAdvapi32.INSTANCE.LogonUser(username, domain, new String(password), WinBase.LOGON32_LOGON_INTERACTIVE,
				WinBase.LOGON32_PROVIDER_DEFAULT, token)) {
			throw new IOException("Logon failed.");
		}

		try {
			PointerByReference env = new PointerByReference();
			if (!Userenv.INSTANCE.CreateEnvironmentBlock(env, token.getValue(), true)) {
				throwLastError();
			}

			try {

				char[] profileDir = new char[256];
				if (!Userenv.INSTANCE.GetUserProfileDirectoryW(token.getValue(), profileDir,
						new IntByReference(profileDir.length))) {
					throwLastError();
				}

				/* Extract the command name, and remaining arguments */
				List<String> cmdArgs = new ArrayList<String>(Arrays.asList(command));
				String cmd = cmdArgs.remove(0);
				StringBuilder bui = new StringBuilder();
				for (int i = 0; i < cmdArgs.size(); i++) {
					if (bui.length() > 0) {
						bui.append(' ');
					}
					String src = cmdArgs.get(i);
					boolean hasSpc = src.indexOf(' ') > -1;
					if (i > 0 && hasSpc)
						bui.append("'");
					bui.append(Util.escapeSingleQuotes(src));
					if (i > 0 && hasSpc)
						bui.append("'");
				}
				final HANDLEByReference pipeStdinRead = new HANDLEByReference(Kernel32.INVALID_HANDLE_VALUE);
				final HANDLEByReference pipeStdinWrite = new HANDLEByReference(Kernel32.INVALID_HANDLE_VALUE);
				final HANDLEByReference pipeStdoutRead = new HANDLEByReference(Kernel32.INVALID_HANDLE_VALUE);
				final HANDLEByReference pipeStdoutWrite = new HANDLEByReference(Kernel32.INVALID_HANDLE_VALUE);
				final HANDLEByReference pipeStderrRead = new HANDLEByReference(Kernel32.INVALID_HANDLE_VALUE);
				final HANDLEByReference pipeStderrWrite = new HANDLEByReference(Kernel32.INVALID_HANDLE_VALUE);

				/* Security attributes for the I/O pipes */
				SECURITY_ATTRIBUTES sAttr = new SECURITY_ATTRIBUTES();
				sAttr.bInheritHandle = true;

				/* Create the I/O pipes */
				try {
					if (!Kernel32.INSTANCE.CreatePipe(pipeStdoutRead, pipeStdoutWrite, sAttr, 0))
						throw new IOException("Could not open I/O pipes.");

					if (!Kernel32.INSTANCE.SetHandleInformation(pipeStdoutRead.getValue(), Kernel32.HANDLE_FLAG_INHERIT,
							0))
						throw new IOException("Could not set handle information on pipe stdout read.");

					if (!Kernel32.INSTANCE.CreatePipe(pipeStdinRead, pipeStdinWrite, sAttr, 0))
						throw new IOException("Could not open I/O pipes.");
					if (!Kernel32.INSTANCE.SetHandleInformation(pipeStdinWrite.getValue(), Kernel32.HANDLE_FLAG_INHERIT,
							0))
						throw new IOException("Could not set handle information on pipe stdin write.");

					if (!Kernel32.INSTANCE.CreatePipe(pipeStderrRead, pipeStderrWrite, sAttr, 0))
						throw new IOException("Could not open I/O pipes.");
					if (!Kernel32.INSTANCE.SetHandleInformation(pipeStderrRead.getValue(), Kernel32.HANDLE_FLAG_INHERIT,
							0))
						throw new IOException("Could not set handle information on pipe sterr read.");

					/* Startup info, pass in the pipes */
					STARTUPINFO sinfo = new STARTUPINFO();
					sinfo.dwFlags = Kernel32.STARTF_USESTDHANDLES;
					sinfo.hStdInput = pipeStdinRead.getValue();
					sinfo.hStdError = pipeStderrWrite.getValue();
					sinfo.hStdOutput = pipeStdoutWrite.getValue();
					//
					/*
					 * Process information will contain info about the spawned
					 * process
					 */
					final PROCESS_INFORMATION pinfo = new PROCESS_INFORMATION();
					
					/* Start the process as the required user */
					if (!(XAdvapi32.INSTANCE.CreateProcessWithLogonW(username, domain == null ? System.getenv("COMPUTERNAME") : domain, new String(password),
							XAdvapi32.LOGON_WITH_PROFILE, cmd, bui.toString(),
							XAdvapi32.CREATE_DEFAULT_ERROR_MODE | XAdvapi32.CREATE_UNICODE_ENVIRONMENT
									| XAdvapi32.CREATE_NO_WINDOW,
							env.getValue(), new String(profileDir), sinfo, pinfo))) {
						throwLastError();
					}

					Runtime.getRuntime().addShutdownHook(new Thread() {
						public void run() {
							Kernel32.INSTANCE.TerminateProcess(pinfo.hProcess, 1);
						}
					});

					final List<Boolean> stop = new ArrayList<>();

					try {

						//
						/*
						 * Read from the processes stdout,stderr and write it
						 * back to // our own stdout and stderr. Also read stdin
						 * and pass it to the process.
						 * 
						 * All this use of threads is pretty horrible really as
						 * is the need to peek at the pipes, but this is hard to
						 * do. Hopefully this implementation will mature. It
						 * works for now :)
						 * 
						 * See
						 * https://blogs.msdn.microsoft.com/oldnewthing/20110707
						 * -00/?p=10223/ for more background on the issues
						 */

						Thread t1 = new Thread() {
							{
								setName("PipeStdoutReadToStdout");
								setDaemon(true);
							}

							public void run() {
								readPipe(stop, pipeStdoutRead, System.out);
							}
						};
						t1.start();
						Thread t2 = new Thread() {
							{
								setName("PipeStderrReadToStderr");
								setDaemon(true);
							}

							public void run() {
								readPipe(stop, pipeStderrRead, System.err);
							}
						};
						t2.start();
						Thread t3 = new Thread() {
							{
								setName("PipeStderrReadToStderr");
								setDaemon(true);
							}

							public void run() {
								try {
									writePipe(pipeStdinWrite, System.in);
								} catch (EOFException e) {
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						};
						t3.start();

						Kernel32.INSTANCE.WaitForSingleObject(pinfo.hProcess, Integer.MAX_VALUE);
						stop.add(Boolean.TRUE);
						IntByReference exitCode = new IntByReference();
						if (Kernel32.INSTANCE.GetExitCodeProcess(pinfo.hProcess, exitCode)) {
							return exitCode.getValue();
						}
						return 1;
					} finally {
						Kernel32.INSTANCE.CloseHandle(pinfo.hProcess);
						Kernel32.INSTANCE.CloseHandle(pinfo.hThread);
					}
				} finally {
					Kernel32.INSTANCE.CloseHandle(pipeStdinRead.getValue());
					Kernel32.INSTANCE.CloseHandle(pipeStdinWrite.getValue());
					Kernel32.INSTANCE.CloseHandle(pipeStderrRead.getValue());
					Kernel32.INSTANCE.CloseHandle(pipeStderrWrite.getValue());
					Kernel32.INSTANCE.CloseHandle(pipeStdoutRead.getValue());
					Kernel32.INSTANCE.CloseHandle(pipeStdoutWrite.getValue());
				}

			} finally {
				// try {
				// Userenv.INSTANCE.DestroyEnvironmentBlock(env.getPointer());
				// } catch (Exception e) {
				// }
			}
		} finally {
			Kernel32.INSTANCE.CloseHandle(token.getValue());
		}
	}

	private static void writePipe(HANDLEByReference pipe, InputStream in) throws IOException {
		byte[] buf = new byte[1024];
		int r = 0;
		IntByReference written = new IntByReference();
		while ((r = in.read(buf, 0, buf.length)) != -1) {
			if (!Kernel32.INSTANCE.WriteFile(pipe.getValue(), buf, r, written, null)) {
				throw new EOFException();
			}
		}
	}

	private static void readPipe(List<Boolean> stop, HANDLEByReference pipe, PrintStream out) {

		IntByReference dwRead = new IntByReference();
		IntByReference totRead = new IntByReference();
		IntByReference msgRead = new IntByReference();
		byte[] buf = new byte[1024];
		while (stop.isEmpty()) {
			if (!Kernel32.INSTANCE.PeekNamedPipe(pipe.getValue(), buf, buf.length, dwRead, totRead, msgRead)) {
				break;
			}
			if (dwRead.getValue() == 0) {
				Thread.yield();
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					break;
				}
			} else {
				if (!Kernel32.INSTANCE.ReadFile(pipe.getValue(), buf, buf.length, dwRead, null)
						|| dwRead.getValue() == 0) {
					break;
				}
				out.println(new String(buf, 0, dwRead.getValue()));
			}
		}
	}

	private static void throwLastError() throws IOException {
		int err = Kernel32.INSTANCE.GetLastError();
		String txt = Kernel32Util.formatMessageFromLastErrorCode(err);
		throw new IOException(String.format("[%x] %s", err, txt));
	}

	public static void main(String[] args) throws Exception {
		String username = System.getenv("W32RUNAS_USERNAME");
		String domain = System.getenv("W32RUNAS_DOMAIN");
		char[] password = System.getenv("W32RUNAS_PASSWORD") == null ? null
				: System.getenv("W32RUNAS_PASSWORD").toCharArray();
		// if (domain == null) {
		// domain = System.getenv("COMPUTERNAME");
		// }
		int defaultOpt = 0;
		int eidx = Arrays.asList(args).indexOf("--");
		String[] command = null;
		if (eidx == -1)
			command = args;
		else {
			for (int i = 0; i < eidx; i++) {
				if (args[i].equals("--domain")) {
					domain = args[++i];
				} else if (args[i].equals("--username")) {
					username = args[++i];
				} else if (args[i].equals("--password")) {
					password = args[++i].toCharArray();
				} else if (args[i].equals("--default-cancel")) {
					defaultOpt = 1;
				}
			}
			command = new String[args.length - eidx - 1];
			System.arraycopy(args, eidx + 1, command, 0, args.length - eidx - 1);
		}
		if (command.length == 0)
			throw new IllegalArgumentException("No command specified.");

		if (username == null) {
			username = "Administrator";
		}

		if (password == null) {

			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

			// Title
			String title = System.getenv("W32RUNAS_TITLE");
			if (title == null) {
				title = "Password Required";
			}

			// Text
			String text = System.getenv("W32RUNAS_TEXT");
			if (text == null) {
				text = "<html>This application requires administrator privileges. Please<br>";
				text += String.format("enter the password for %s to continue.</html>",
						(domain == null ? System.getenv("COMPUTERNAME") : domain) + "\\" + username);
			}

			JPanel panel = new JPanel();
			JLabel label = new JLabel("Enter a password:");
			final JPasswordField pass = new JPasswordField(10);
			pass.requestFocus();
			panel.add(label);
			panel.add(pass);

			JPanel centre = new JPanel(new BorderLayout());
			centre.add(new JLabel(text), BorderLayout.CENTER);
			centre.add(panel, BorderLayout.SOUTH);

			String[] options = new String[] { "OK", "Cancel" };

			JOptionPane opt = new JOptionPane(centre, JOptionPane.PLAIN_MESSAGE, JOptionPane.YES_NO_OPTION, null,
					options, options[defaultOpt]);

			JDialog dialog = opt.createDialog(title);

			Icon icon = UIManager.getIcon("OptionPane.warningIcon");
			BufferedImage img = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(),
					BufferedImage.TYPE_INT_ARGB);
			icon.paintIcon(null, img.getGraphics(), 0, 0);
			dialog.setIconImage(img);
			dialog.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentShown(ComponentEvent e) {
					pass.requestFocus();
				}
			});
			dialog.addFocusListener(new FocusListener() {

				@Override
				public void focusLost(FocusEvent e) {
				}

				@Override
				public void focusGained(FocusEvent e) {
					pass.requestFocus();

				}
			});
			dialog.setAlwaysOnTop(true);
			dialog.setVisible(true);
			Object sel = opt.getValue();
			if (sel != null && sel.equals(options[0])) {
				password = pass.getPassword();
			}
		}

		if (password != null) {
			try {
				System.exit(runAs(username, domain, password, command));
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		} else
			System.exit(1);
	}
}
