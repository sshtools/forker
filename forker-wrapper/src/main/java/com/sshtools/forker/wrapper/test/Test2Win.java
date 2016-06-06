package com.sshtools.forker.wrapper.test;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;

import com.sshtools.forker.client.Forker;
import com.sshtools.forker.client.OSCommand;

public class Test2Win {

	public Test2Win() {
	}

	public static void main(String[] args) throws Exception {
		//OSCommand.run("c:\\windows\\system32\\cmd.exe");
//		try {
//		OSCommand.run("c:\\windows\\system32\\xcopy.exe");
//		}
//		catch(Exception e) {
//			
//		}
//		// OSCommand.elevate();
//		// try {
//		// OSCommand.run("id");
//		// }
//		// finally {
//		// OSCommand.restrict();
//		// }
//		
//		InputStream fin = Forker.readFile(new File("/windows/system32/drivers/etc/hosts"));
//		OutputStream fout = Forker.writeFile(new File("/temp-hosts1111.txt"), false);
//		IOUtils.copy(fin, fout);

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					for (int i = 0; i < 20; i++) {
						System.out.println("EXIT WAITINIG " + i);
						Thread.sleep(1000);
					}
				} catch (Exception e) {
				}
			}
		});

		for (int i = 0; i < 1000; i++) {
			System.out.println("WAITINIG " + i);
			Thread.sleep(1000);
		}

	}

}
