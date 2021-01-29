package com.sshtools.forker.examples;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;

import org.apache.commons.lang3.SystemUtils;

import com.sshtools.forker.client.DefaultNonBlockingProcessListener;
import com.sshtools.forker.client.ForkerBuilder;
import com.sshtools.forker.client.NonBlockingProcess;
import com.sshtools.forker.common.IO;

/**
 * This example demonstrates forking a non-blocking processing, reading it's
 * output and sending it input using 3 different methods.
 */
public class NonBlockingBiDir {
	public static void main(String[] args) throws Exception {
		ForkerBuilder builder = new ForkerBuilder().io(IO.NON_BLOCKING).redirectErrorStream(true);
		if (SystemUtils.IS_OS_UNIX) {
			// The unix example uses wc to count lines of standard input
			builder.command("wc", "-l");
		} else if (SystemUtils.IS_OS_WINDOWS) {
			// The windows example uses find to count lines of standard input
			builder.command("find", "/c", "/v", "");
		} else {
			throw new UnsupportedOperationException();
		}
		final NonBlockingProcess process = builder.start(new DefaultNonBlockingProcessListener() {
			@Override
			public void onStdout(NonBlockingProcess process, ByteBuffer buffer, boolean closed) {
				if (!closed) {
					byte[] bytes = new byte[buffer.remaining()];
					/* Consume bytes from buffer (so position is updated) */
					buffer.get(bytes);
					System.out.println(new String(bytes));
				}
			}

			@Override
			public boolean onStdinReady(NonBlockingProcess process, ByteBuffer buffer) {
				/**
				 * This is invoked as a results of
				 * {@link ForkerProcess#wantWrite} (see below).
				 */
				buffer.put("This is line 3\n".getBytes());
				buffer.flip();

				/**
				 * Close the STDIN stream to the process. This will cause it count the lines,
				 * output the results on stdout and exit. If you are using an
				 * {@link OutputStream} instead you can call {@link OutputStream#close()}, either
				 * will work.
				 */
				try {
					process.closeStdin(false);
				} catch (IOException e) {
					throw new IllegalStateException("Failed to close.");
				}
				
				/*
				 * Return false to indicate there is no more to write. Return
				 * true and this method will be called again when there is space
				 * available (and so on until false is returned).
				 */
				return false;
			}
		});
		
		/**
		 * You can either (1) get the output stream from the process as you
		 * usually would :-
		 * 
		 * NOTE: You have to {@link OutputStream#flush()} to send the data to
		 * the process as internally a {@link BufferdOutputStream} is being used
		 * ({@link PrintWriter} does that for you when using autoflush in this
		 * case).
		 */
		new PrintWriter(process.getOutputStream(), true).println("This is line 1");
		/*
		 * .. or (2) access the non-blocking write methods directly (no flush
		 * required) and you get to use {@link ByteBuffer}.
		 */
		process.writeStdin(ByteBuffer.wrap("This is line 2\n".getBytes()));
		
		/**
		 * .. or (3) via the listener. First, indicate that you wish some STDIN
		 * to be read. When ready,
		 * {@link NonBlockingProcessListener#onStdin(ByteBuffer)} will be
		 * called, which is where you provide the data.
		 * 
		 * In this example, when we supply this input we then close the 
		 * stream to complete the command.
		 */
		process.wantWrite();
		
		/*
		 * Not strictly required, this is just to hold up the example thread
		 * until the command is finished, your use case may or may not need to
		 * wait for the command to finish.
		 */
		System.out.println("Done: " + process.waitFor());
	}
}
