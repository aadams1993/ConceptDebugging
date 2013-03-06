package mcdbg;

public class Runner {

	public static void main(String[] args) {
		Mover mover = new Mover();
		mover.start();
		System.out.println("Mover thread started");
		try {
			for (int i = 0; i < 10; ++i) {
				int x = 10 * i, y = 100 - x;
				System.out.println("Adding move to queue: X=" + x + ", Y=" + y);
				mover.move(x, y);
				System.out.println("Adding delay to queue");
				mover.delay(500);
			}
			System.out.println("Resetting Queue...");
			mover.resetQueue();
			for (int i = 0; i < 10; ++i) {
				int x = 10 * i, y = 100 - x;
				System.out.println("Adding moveTo to queue: X=" + x + ", Y=" + y);
				mover.moveTo(x, y);
				System.out.println("Adding delay to queue");
				mover.delay(500);
			}
			System.out.println("Queued moves: " + mover.numQueuedJobs());
			System.out.println("Waiting for completion...");
			System.out.println();
			
			Thread.sleep(1000);
			System.out.println("Resetting Queue...");
			mover.resetQueue();
			
			if (mover.hasQueuedJobs())
				mover.waitForCompletion();
			System.out.println();
			System.out.println("Completed!\nKilling mover");
			mover.kill();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Completed successfully!");
	}

}
