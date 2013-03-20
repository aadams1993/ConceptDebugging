package keycontrol;

import java.awt.KeyboardFocusManager;

import javax.swing.JFrame;

@SuppressWarnings("serial")
public class TestGUI extends JFrame {
	public static void main(String[] args) {
		KeyboardFocusManager manager = KeyboardFocusManager
				.getCurrentKeyboardFocusManager();
		manager.addKeyEventDispatcher(new KeyControl());
		for (int i = 0; i < 3; ++i) {
			TestGUI gui = new TestGUI();
			gui.setVisible(true);
			gui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		}
	}

	public TestGUI() {
		this.setSize(640, 480);
	}

}
