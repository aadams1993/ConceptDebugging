package mcdbg;

public class Runner {

	public static void main(String[] args) {
		Mover mover = new Mover();
		mover.start();
		System.out.println("Mover thread started");
		try {
			for (int i = 0; i < 5; ++i) {
				int x = 10 * i, y = 100 - x;
				System.out.println("Adding move to queue: X=" + x + ", Y=" + y);
				mover.move(x, y);
				System.out.println("Adding delay to queue");
				mover.delay(200);
			}
			// Wait for the mover to start some jobs
			Thread.sleep(500);
			System.out.println("Resetting queue...");
			mover.resetQueue();

			for (int i = 0; i < 5; ++i) {
				int x = 10 * i, y = 100 - x;
				System.out.println("Adding moveTo to queue: X=" + x + ", Y="
						+ y);
				mover.moveTo(x, y);
				System.out.println("Adding delay to queue");
				mover.delay(200);
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
