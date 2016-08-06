package com.sshtools.forker.client.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.UIManager;

/**
 * Simple Swing based helper application that asks for a password and prints it
 * on stdout.
 */
public class AskPass extends JFrame {

	private static final long serialVersionUID = 1L;

	/**
	 * Entry point
	 * 
	 * @param args
	 *            command line arguments
	 * @throws Exception on any error
	 */
	public static void main(String[] args) throws Exception {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

		// Title
		String title = System.getenv("ASKPASS_TITLE");
		if (title == null) {
			title = "Administrator Password Required";
		}

		// Text
		String text = System.getenv("ASKPASS_TEXT");
		if (text == null) {
			text = "<html>This application requires elevated privileges. Please<br>";
			text += "enter the administrator password to continue.</html>";
		}

		JPanel panel = new JPanel();
		JLabel label = new JLabel("Enter a password:");
		final JPasswordField pass = new JPasswordField(10);
		panel.add(label);
		panel.add(pass);

		JPanel centre = new JPanel(new BorderLayout());
		centre.add(new JLabel(text), BorderLayout.CENTER);
		centre.add(panel, BorderLayout.SOUTH);

		String[] options = new String[] { "OK", "Cancel" };

		JOptionPane opt = new JOptionPane(centre, JOptionPane.PLAIN_MESSAGE, JOptionPane.YES_NO_OPTION, null, options,
				options[0]);

		JDialog dialog = opt.createDialog(title);

		Icon icon = UIManager.getIcon("OptionPane.warningIcon");
		if (icon == null) {
			icon = new Icon() {

				@Override
				public void paintIcon(Component c, Graphics g, int x, int y) {
				}

				@Override
				public int getIconWidth() {
					return 16;
				}

				@Override
				public int getIconHeight() {
					return 16;
				}
			};
		}
		BufferedImage img = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
		icon.paintIcon(null, img.getGraphics(), 0, 0);
		dialog.setIconImage(img);
		dialog.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				pass.requestFocus();
			}
		});
		dialog.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
			}

			@Override
			public void focusGained(FocusEvent e) {
				pass.requestFocus();

			}
		});
		dialog.setAlwaysOnTop(true);
		dialog.setVisible(true);
		Object sel = opt.getValue();
		if (sel != null && sel.equals(options[0])) {
			char[] password = pass.getPassword();
			System.out.println(new String(password));
			System.exit(0);
		} else
			System.exit(1);

	}
}
