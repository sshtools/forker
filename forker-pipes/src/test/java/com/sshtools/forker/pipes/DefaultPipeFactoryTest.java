package com.sshtools.forker.pipes;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.junit.BeforeClass;
import org.junit.Test;

import com.sshtools.forker.pipes.PipeFactory.Flag;

public class DefaultPipeFactoryTest {
	private static DefaultPipeFactory factory;

	@BeforeClass
	public static void createFactory() throws Exception {
		factory = new DefaultPipeFactory();
	}

	@Test
	public void singlePipe() throws Exception {
		String pipeName = "us_xfr";
		ServerSocket srvSock = factory.createPipeServer(pipeName, Flag.ABSTRACT);
		Thread srv = new Thread() {
			public void run() {
				System.out.println("Accepting");
				try (Socket socket = srvSock.accept()) {
					System.out.println("Transferring stream");
					socket.getInputStream().transferTo(System.out);
					System.out.println("Transferred stream");
				} catch (IOException ie) {
					ie.printStackTrace();
				}
			}
		};
		System.out.println("Starting");
		srv.start();

		System.out.println("Creating client");
		try (Socket pipe = factory.createPipe(pipeName, Flag.ABSTRACT)) {
			System.out.println("Getting output stream");
			try(OutputStream out = pipe.getOutputStream()) {
				out.write("Test".getBytes());
//				out.flush();
			}
		}

		srv.join();
	}
}
