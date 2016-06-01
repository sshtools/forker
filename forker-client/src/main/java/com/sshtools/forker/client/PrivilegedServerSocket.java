package com.sshtools.forker.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;

public class PrivilegedServerSocket extends ServerSocket {

	public PrivilegedServerSocket() throws IOException {
		// TODO Auto-generated constructor stub
	}

	public PrivilegedServerSocket(int port) throws IOException {
		super(port);
		// TODO Auto-generated constructor stub
	}

	public PrivilegedServerSocket(int port, int backlog) throws IOException {
		super(port, backlog);
		// TODO Auto-generated constructor stub
	}

	public PrivilegedServerSocket(int port, int backlog, InetAddress bindAddr) throws IOException {
		super(port, backlog, bindAddr);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void bind(SocketAddress endpoint) throws IOException {
		// TODO Auto-generated method stub
		super.bind(endpoint);
	}

	@Override
	public void bind(SocketAddress endpoint, int backlog) throws IOException {
		// TODO Auto-generated method stub
		super.bind(endpoint, backlog);
	}

	@Override
	public int getLocalPort() {
		// TODO Auto-generated method stub
		return super.getLocalPort();
	}

	@Override
	public SocketAddress getLocalSocketAddress() {
		// TODO Auto-generated method stub
		return super.getLocalSocketAddress();
	}

	@Override
	public Socket accept() throws IOException {
		// TODO Auto-generated method stub
		return super.accept();
	}

	@Override
	public boolean isBound() {
		// TODO Auto-generated method stub
		return super.isBound();
	}

	@Override
	public boolean isClosed() {
		// TODO Auto-generated method stub
		return super.isClosed();
	}

	@Override
	public synchronized void setSoTimeout(int timeout) throws SocketException {
		// TODO Auto-generated method stub
		super.setSoTimeout(timeout);
	}

	@Override
	public synchronized int getSoTimeout() throws IOException {
		// TODO Auto-generated method stub
		return super.getSoTimeout();
	}

}
