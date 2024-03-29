package com.sshtools.forker.pipes;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

import com.sshtools.forker.common.OS;
import com.sun.jna.Platform;

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
		if (OS.isUnix())
			return new UnixDomainSocket(toUnixName(name, flags), flags);
		else if (Platform.isWindows())
			return new NamedPipeSocket(toWindowsName(name, flags), flags);
		throw new UnsupportedOperationException();
	}

	protected ServerSocket serverSocket(String name, Flag... flags) throws IOException {
		if (OS.isUnix())
			return new UnixDomainServerSocket(toUnixName(name, flags), flags);
		else if (Platform.isWindows())
			return new NamedPipeServerSocket(toWindowsName(name, flags), flags);
		throw new UnsupportedOperationException();
	}

	protected String validateName(String name, Flag[] flags) {
		for (char c : name.toCharArray()) {
			if (!Character.isAlphabetic(c) && !Character.isDigit(c) && c != '_' && c != '.' && c != '-' && (!Platform.isWindows() || c != '\\'))
				throw new IllegalArgumentException(String.format("Invalid pipe name. %s", name));
		}
		return name;
	}

	protected String toUnixName(String name, Flag... flags) {
		List<Flag> flagsList = Arrays.asList(flags);
		if (flagsList.contains(Flag.CONCRETE))
			return "/tmp/" + validateName(name, flags);
		else
			return "\0/tmp/" + validateName(name, flags);
	}

	protected String toWindowsName(String name, Flag... flags) {
		List<Flag> flagsList = Arrays.asList(flags);
		if (flagsList.contains(Flag.CONCRETE) && !flagsList.contains(Flag.ABSTRACT))
			throw new UnsupportedOperationException("Windows does not support concrete file names for pipes.");

		if (flagsList.contains(Flag.REMOTE)) {
			String[] parts = name.split(":");
			if (parts.length != 2)
				throw new IllegalArgumentException("For a remote pipe, the name must be in the format host:name");
			return "\\\\" + parts[0] + "\\pipe\\" + validateName(parts[1], flags);
		} else {
			return "\\\\.\\pipe\\" + validateName(name, flags);
		}
	}
}
