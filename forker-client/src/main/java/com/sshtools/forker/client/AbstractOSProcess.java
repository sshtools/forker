package com.sshtools.forker.client;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.apache.commons.lang3.SystemUtils;

import com.sshtools.forker.common.Util;

/**
 * Abstract implementation for processes that use OS calls.
 */
public abstract class AbstractOSProcess extends ForkerProcess {

	@Override
	public void destroy() {
		throw new UnsupportedOperationException();
	};

	@Override
	public InputStream getErrorStream() {
		throw new UnsupportedOperationException();
	}

	@Override
	public InputStream getInputStream() {
		throw new UnsupportedOperationException();
	}

	@Override
	public OutputStream getOutputStream() {
		throw new UnsupportedOperationException();
	}

	protected String buildCommand(final ForkerBuilder builder) {
		// This will run in a shell, so we need to escape the
		// command line arguments
		StringBuilder bui = new StringBuilder();

		if (SystemUtils.IS_OS_WINDOWS) {
			if (builder.background()) {
				bui.append("start /b");

				if (builder.directory() != null) {
					bui.append(" /d\"");
					bui.append(Util.escapeDoubleQuotes(builder.directory().getAbsolutePath()));
					bui.append("\"");
				}

				if (isLessThanWindows7()) {
					// http://stackoverflow.com/questions/154075/using-the-dos-start-command-with-parameters-passed-to-the-started-program
					// < Windows 7, if the first argument is quoted its the
					// window title
					bui.append(" \"\"");
				}
			} else {
				bui.append("cmd /c");
				if (builder.directory() != null) {
					bui.append(" \"cd \"");
					bui.append(Util.escapeDoubleQuotes(builder.directory().getAbsolutePath()));
					bui.append("\" && ");
				}

			}
			for (int i = 0; i < builder.command().size(); i++) {
				if (i == 0 && !builder.background()) {
					// cmd doesnt like first argument at all :(
					bui.append(builder.command().get(i));
				} else {
					bui.append("\"");
					bui.append(Util.escapeDoubleQuotes(builder.command().get(i)));
					bui.append("\"");
				}
			}

			if (!builder.background() && builder.directory() != null) {
				bui.append("\"");
			}
		} else if (SystemUtils.IS_OS_MAC_OSX || SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_UNIX) {
			for (Map.Entry<String, String> en : builder.environment().entrySet()) {
				if (bui.length() > 0) {
					bui.append(";");
				}
				bui.append("export ");
				bui.append(en.getKey());
				bui.append("='");
				bui.append(Util.escapeSingleQuotes(en.getValue()));
				bui.append("'");
			}
			if (builder.directory() != null) {
				if (bui.length() > 0) {
					bui.append(";");
				}
				bui.append("cd '");
				bui.append(Util.escapeSingleQuotes(builder.directory().getAbsolutePath()));
				bui.append("'");
			}
			if (bui.length() > 0) {
				bui.append(";");
			}
			for (String a : builder.getAllArguments()) {
				bui.append(" '");
				bui.append(Util.escapeSingleQuotes(a));
				bui.append("'");
			}
			if (builder.background()) {
				bui.insert(0, "(");
			}
			if (builder.redirectErrorStream()) {
				bui.insert(0, "(");
			}
			if (builder.redirectErrorStream()) {
				bui.append(") 2>&1");
			}
			if (builder.background()) {
				bui.append(")&");

			}
		} else {
			throw new UnsupportedOperationException("OS not supported.");
		}

		final String string = bui.toString();
		return string;
	}

	private boolean isLessThanWindows7() {
		return SystemUtils.IS_OS_WINDOWS_95 || SystemUtils.IS_OS_WINDOWS_98 || SystemUtils.IS_OS_WINDOWS_XP
				|| SystemUtils.IS_OS_WINDOWS_2000 || SystemUtils.IS_OS_WINDOWS_ME || SystemUtils.IS_OS_WINDOWS_NT;
	}

}
