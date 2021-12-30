package hello.world;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JLabel;

@SuppressWarnings("serial")
public class HelloWorld2 extends JFrame {

	public HelloWorld2() {
		super("HelloWorld2");
		setPreferredSize(new Dimension(300, 200));
		setLayout(new BorderLayout());
		add(new JLabel("Hello World 2!", JLabel.CENTER), BorderLayout.CENTER);
	}
	
	public static void main(String[] args) {
		HelloWorld2 world = new HelloWorld2();
		world.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		world.pack();
		world.setVisible(true);
		world.setLocation(300, 300);
	}

}
