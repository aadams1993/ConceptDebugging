package keycontrol;

import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;
import java.util.Timer;
import java.util.TimerTask;

public class KeyControl implements KeyEventDispatcher {
	private static final int KEY_W = 0;
	private static final int KEY_S = 1;
	private static final int KEY_A = 2;
	private static final int KEY_D = 3;
	private static final int KEY_R = 4;

	private static final long KEY_REFRESH_INTERVAL = 50;
	private static final long KEY_HELD_WAIT_PERIOD = 500;

	private final Timer[] keyHeldTimer;
	private long[] timerStart = { 0, 0, 0, 0, 0 };
	private boolean[] timerRunning = { false, false, false, false, false };

	private class KeyTimer extends TimerTask {
		private final char key;

		public KeyTimer(char key) {
			this.key = key;
		}

		@Override
		public void run() {
			int index = getKeyIndex(key);
			if (Math.abs(timerStart[index] - System.currentTimeMillis()) < KEY_HELD_WAIT_PERIOD) {
				refreshTimer(key, index);
				return;
			}
			System.out.println("Key released: " + key);
			timerRunning[index] = false;
		}
	}

	public KeyControl() {
		keyHeldTimer = new Timer[5];
		for (int i = 0; i < 5; ++i)
			keyHeldTimer[i] = new Timer();
	}

	private int getKeyIndex(char key) {
		switch (key) {
		case 'w':
			return KEY_W;
		case 's':
			return KEY_S;
		case 'a':
			return KEY_A;
		case 'd':
			return KEY_D;
		case 'r':
			return KEY_R;
		default:
			return -1;
		}
	}

	private void refreshTimer(char key, int index) {
		// Key held down
		if (timerRunning[index]) {
			keyHeldTimer[index].cancel();
			keyHeldTimer[index] = new Timer();
		}
		// Key just pressed
		else {
			timerStart[index] = System.currentTimeMillis(); 
		}
		keyHeldTimer[index].schedule(new KeyTimer(key), KEY_REFRESH_INTERVAL);
		timerRunning[index] = true;
	}

	private boolean process(char key) {
		switch (key) {
		case 'w':
			System.out.println("Forward!");
			break;
		case 's':
			System.out.println("Backward!");
			break;
		case 'a':
			System.out.println("Rotate left!");
			break;
		case 'd':
			System.out.println("Rotate right!");
			break;
		case 'r':
			System.out.println("Kick!");
			break;
		default:
			return false;
		}
		int index = getKeyIndex(key);
		refreshTimer(key, index);
		return true;
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent evt) {
		int eventType = evt.getID();
		switch (eventType) {
		case KeyEvent.KEY_PRESSED:
			char key = evt.getKeyChar();
			if (!process(key))
				return false;
			break;
		default:
			return false;
		}
		return true;
	}
}
