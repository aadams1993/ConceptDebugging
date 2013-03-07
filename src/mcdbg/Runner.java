package mcdbg;

import safesleep.SafeSleep;

public class Runner {

	public static void main(String[] args) {
		Mover mover = new Mover();
		mover.start();
		System.out.println("Mover thread started");
		try {
			System.out.println("Testing for sleep deadlock");
			long timeBefore = System.currentTimeMillis();
			mover.delay(1000);
			System.out.println("Added 1000 ms delay to movement queue");
			System.out.println("Sleeping for 3 rounds of 100 ms");
			SafeSleep.sleep(100);
			SafeSleep.sleep(100);
			SafeSleep.sleep(100);
			mover.delay(1000);
			System.out.println("Added 1000 ms delay to movement queue");
			long timeElapsed = System.currentTimeMillis() - timeBefore;
			System.out.println("Done in " + timeElapsed + " ms");
			if (mover.hasQueuedJobs() || mover.isRunning())
				mover.waitForCompletion();
			
			for (int i = 0; i < 5; ++i) {
				int x = 10 * i, y = 100 - x;
				mover.move(x, y);
				mover.delay(1000);
			}
			// Wait for the mover to start some jobs
			SafeSleep.sleep(2500);
//			System.out.println("Resetting queue...");
//			mover.resetQueue();

			for (int i = 0; i < 5; ++i) {
				int x = 10 * i, y = 100 - x;
				mover.moveTo(x, y);
				mover.delay(1000);
			}
			System.out.println("Queued moves: " + mover.numQueuedJobs());
			System.out.println("Waiting for completion...");
			System.out.println();

			if (mover.hasQueuedJobs() || mover.isRunning())
				mover.waitForCompletion();
			System.out.println();
			
			System.out.println("Completed!\nKilling mover");
			mover.kill();
			System.out.println("Mover killed, joining with main");
			mover.join();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Completed successfully!");
	}

}
