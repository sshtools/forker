package com.sshtools.forker.client.ui;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

public class AskPass extends JFrame {

	public static void main(String[] args) {
		
		// Title
		String title = System.getenv("ASKPASS_TITLE");
		if(title == null) {
			title = "Administrator Password Required";
		}
		
		// Text
		String text = System.getenv("ASKPASS_TEXT");
		if(text == null) {
			text = "<html>This application requires elevated privileges. Please<br>";
			text += "enter the administrator password to continue.</html>";
		}
		
		JPanel panel = new JPanel();
		JLabel label = new JLabel("Enter a password:");
		JPasswordField pass = new JPasswordField(10);
		panel.add(label);
		panel.add(pass);

		JPanel centre = new JPanel(new BorderLayout());
		centre.add(new JLabel(text), BorderLayout.CENTER);
		centre.add(panel, BorderLayout.SOUTH);
		
		String[] options = new String[] { "OK", "Cancel" };
		int option = JOptionPane.showOptionDialog(null, centre, title, JOptionPane.NO_OPTION,
				JOptionPane.PLAIN_MESSAGE, null, options, options[1]);
		if (option == 0) // pressing OK button
		{
			char[] password = pass.getPassword();
			System.out.println(new String(password));
		}
	}
}
