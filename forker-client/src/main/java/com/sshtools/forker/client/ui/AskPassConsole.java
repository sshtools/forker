package com.sshtools.forker.client.ui;

import java.io.BufferedReader;
import java.io.Console;
import java.io.InputStreamReader;

public class AskPassConsole {

	public static void main(String[] args) throws Exception  {
		Console console = System.console();
		if(console == null)
			System.err.println("WARNING: Not on a console, password will be visible");
		
		// Title
		String title = System.getenv("ASKPASS_TITLE");
		if(title == null) {
			title = "Administrator Password Required";
		}
		
		// Text
		String text = System.getenv("ASKPASS_TEXT");
		if(text == null) {
			text = "This application requires elevated privileges. Please\n";
			text += "enter the administrator password to continue.";
		}
		
		System.err.println(title);
		System.err.println();
		System.err.println(text);
		System.err.println();
		
		System.err.print("Enter a password:");
		String pw = null;
		if(console == null) {
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			pw = br.readLine();
		}
		else {
			char[] c = console.readPassword("");
			pw = c == null ? null : new String(c);
		}
		if(pw != null)
			System.out.println(pw);
	}
}
