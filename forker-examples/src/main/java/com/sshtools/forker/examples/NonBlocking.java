package com.sshtools.forker.examples;

import java.nio.ByteBuffer;

import com.sshtools.forker.client.DefaultNonBlockingProcessListener;
import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.client.NonBlockingProcess;
import com.sshtools.forker.common.IO;
import com.sshtools.forker.common.OS;

/**
 * Simple non-blocking I/O example that reads the output of a command.
 */
public class NonBlocking {
	public static void main(String[] args) throws Exception {
		ForkerBuilder builder = new ForkerBuilder().io(IO.NON_BLOCKING).redirectErrorStream(true);
		if (OS.isUnix()) {
			// The unix example tries to list the root directory
			builder.command("ls", "-al", "/");
		} else {
			builder.command("DIR", "C:\\");
		}
		NonBlockingProcess process = new ForkerBuilder("ls", "-al", "/").io(IO.NON_BLOCKING).redirectErrorStream(true)
				.start(new DefaultNonBlockingProcessListener() {
					@Override
					public void onStdout(NonBlockingProcess process, ByteBuffer buffer, boolean closed) {
						if (!closed) {
							byte[] bytes = new byte[buffer.remaining()];
							/* Consume bytes from buffer (so position is updated) */
							buffer.get(bytes);
							System.out.println(new String(bytes));
						}
					}
				});
		/*
		 * Not strictly required, this is just to hold up the example thread until the
		 * command is finished, your use case may or may not need to wait for the
		 * command to finish.
		 */
		System.out.println("Done: " + process.waitFor());
	}
}
