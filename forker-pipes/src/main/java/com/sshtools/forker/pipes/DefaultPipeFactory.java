package com.sshtools.forker.pipes;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.SystemUtils;

public class DefaultPipeFactory implements PipeFactory {

	@Override
	public ServerSocket createPipeServer(String name, Flag... flags) throws IOException {
		ServerSocket serverSocket = serverSocket(name);
		serverSocket.bind(null);
		return serverSocket;
	}

	@Override
	public Socket createPipe(String name, Flag... flags) throws IOException {
		Socket socket = socket(name);
		socket.connect(null);
		return socket;
	}

	protected Socket socket(String name, Flag... flags) throws IOException {
		if (SystemUtils.IS_OS_UNIX)
			return new UnixDomainSocket(toUnixName(name, flags), flags);
		else if (SystemUtils.IS_OS_WINDOWS)
			return new NamedPipeSocket(toWindowsName(name, flags), flags);
		throw new UnsupportedOperationException();
	}

	protected ServerSocket serverSocket(String name, Flag... flags) throws IOException {
		if (SystemUtils.IS_OS_UNIX)
			return new UnixDomainServerSocket(toUnixName(name, flags), flags);
		else if (SystemUtils.IS_OS_WINDOWS)
			return new NamedPipeServerSocket(toWindowsName(name, flags), flags);
		throw new UnsupportedOperationException();
	}

	protected String validateName(String name) {
		for (char c : name.toCharArray()) {
			if (!Character.isAlphabetic(c) && !Character.isDigit(c) && c != '_' && c != '.' && c != '-')
				throw new IllegalArgumentException("Invalidate pipe name.");
		}
		return name;
	}

	protected String toUnixName(String name, Flag... flags) {
		List<Flag> flagsList = Arrays.asList(flags);
		if (flagsList.contains(Flag.CONCRETE))
			return "/tmp/" + validateName(name);
		else
			return "\0/tmp/" + validateName(name);
	}

	protected String toWindowsName(String name, Flag... flags) {
		List<Flag> flagsList = Arrays.asList(flags);
		if (flagsList.contains(Flag.CONCRETE) && !flagsList.contains(Flag.ABSTRACT))
			throw new UnsupportedOperationException("Windows does not support concrete file names for pipes.");

		if (flagsList.contains(Flag.REMOTE)) {
			String[] parts = name.split(":");
			if (parts.length != 2)
				throw new IllegalArgumentException("For a remote pipe, the name must be in the format host:name");
			return "\\\\" + parts[0] + "\\pipe\\" + validateName(parts[1]);
		} else {
			return "\\\\.\\pipe\\" + validateName(name);
		}
	}
}
