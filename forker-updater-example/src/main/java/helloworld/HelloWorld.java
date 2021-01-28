package helloworld;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JLabel;

@SuppressWarnings("serial")
public class HelloWorld extends JFrame {

	public HelloWorld() {
		super("HelloWorld");
		setPreferredSize(new Dimension(300, 200));
		setLayout(new BorderLayout());
		add(new JLabel("Hello World!", JLabel.CENTER), BorderLayout.CENTER);
	}
	
	public static void main(String[] args) {
		HelloWorld world = new HelloWorld();
		world.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		world.pack();
		world.setVisible(true);
		world.setLocation(200, 200);
	}

}
