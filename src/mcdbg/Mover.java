package mcdbg;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Mover extends Thread {
	private enum Mode {
		STOP, DELAY, MOVE_VECTOR, MOVE_TO
	};

	private boolean running = false;
	private boolean interruptMove = false;
	private boolean die = false;

	private class MoverConfig {
		public double x = 0;
		public double y = 0;

		public long milliseconds = 0;

		public Mode mode;
	};

	private ConcurrentLinkedQueue<MoverConfig> moveQueue = new ConcurrentLinkedQueue<MoverConfig>();
	private Semaphore queueSem = new Semaphore(1, true);

	private Semaphore jobSem = new Semaphore(0, true);
	private Semaphore killSem = new Semaphore(0, true);
	private Semaphore waitSem = new Semaphore(0, true);

	/** Thread-safe sleep scheduler */
	private final ScheduledExecutorService sleepScheduler = Executors
			.newScheduledThreadPool(1);

	/** Thread-safe sleep */
	private void safeSleep(long millis) throws InterruptedException {
		final Semaphore sleepSem = new Semaphore(0, true);
		// Schedule a wake-up after the specified time
		sleepScheduler.schedule(new Runnable() {
			@Override
			public void run() {
				sleepSem.release();
			}
		}, millis, TimeUnit.MILLISECONDS);
		// Wait for the wake-up
		sleepSem.acquire();
	}

	private boolean pushMovement(MoverConfig movement) {
		int pushAttempts = 0;
		try {
			queueSem.acquire();
		} catch (InterruptedException e) {
			return false;
		}
		// Try to push the movement 10 times before giving up
		while (!moveQueue.offer(movement) && pushAttempts < 10)
			++pushAttempts;
		queueSem.release();
		// If we gave up, return false to indicate it
		if (pushAttempts >= 10)
			return false;
		return true;
	}

	public Mover() {
		super("mover");
	}

	@Override
	public void run() {
		// safeSleep requires some initial use to prevent lag spikes on the
		// first few runs - Thread.sleep has similar problems but less
		// noticeable
		try {
			for (int i = 0; i < 3; ++i)
				safeSleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		try {
			while (!die) {
				// Temporarily block queue changes while retrieving the next job
				queueSem.acquire();
				// Wait for next movement operation
				jobSem.acquire();
				// Clear the movement interrupt flag for the new movement
				interruptMove = false;
				// Set the running flag to true for busy-waiting
				running = true;

				if (!moveQueue.isEmpty() && !die) {
					MoverConfig movement = moveQueue.poll();
					// Queue will not change from here on, so release the
					// semaphore
					queueSem.release();
					assert (movement != null) : "moveQueue.poll() returned null when non-empty";

					if (movement.mode == null) {
						System.out.println("Mover is idle");
						continue;
					}

					switch (movement.mode) {
					case STOP:
						System.out.println("Stopping robot");
						break;
					case DELAY:
						System.out.println("Waiting for "
								+ movement.milliseconds + " milliseconds");
						safeSleep(movement.milliseconds);
						break;
					case MOVE_VECTOR:
						System.out.println("Moving at speed (" + movement.x
								+ ", " + movement.y + ")");
						doMove(movement.x, movement.y);
						break;
					case MOVE_TO:
						System.out.println("Moving to point (" + movement.x
								+ ", " + movement.y + ")");
						doMoveTo(movement.x, movement.y);
						break;
					default:
						System.out
								.println("DERP! Unknown movement mode specified");
						assert (false);
					}
				} else {
					queueSem.release();
				}
				
				// If we just did the last move in the queue, wake up the waiters
				if (moveQueue.isEmpty()) {
					// Only wake up the waiting threads if there are waiting
					// threads to wake up
					System.out.println("Waking up waiters");
					while (waitSem.hasQueuedThreads())
						waitSem.release();
					running = false;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		// Signal that robot is stopped and safe to disconnect
		// Only if there is actually any threads waiting
		while (killSem.hasQueuedThreads())
			killSem.release();
	}

	public void kill() {
		die = true;
		interruptMove = true;
		// Trigger one final run for the mover thread to clean up waiters, stop
		// the robot and clear the robot's buffer
		jobSem.release();
		// Wait for the thread to die
		killSem.acquireUninterruptibly();
	}

	public void waitForCompletion() throws InterruptedException {
		waitSem.acquire();
	}

	public void interruptMove() {
		interruptMove = true;
	}
	
	public void resetQueue() throws InterruptedException {
		if (moveQueue.isEmpty())
			return;
		interruptMove();

		// Block changes in the queue until the queue is finished
		// resetting
		queueSem.acquire();
		// Acquire the permits for queued jobs to cancel the execution for them
		jobSem.acquire(moveQueue.size());
		moveQueue.clear();
		// Reactivate the movement thread
		queueSem.release();
	}

	public boolean isRunning() {
		return running;
	}

	public boolean hasQueuedJobs() {
		// Semaphore not required since isEmpty is constant runtime and the
		// queue is thread-safe
		return !moveQueue.isEmpty();
	}

	public int numQueuedJobs() {
		// Get a lock on the queue to prevent changes while determining how many
		// jobs there are
		try {
			queueSem.acquire();
			int result = moveQueue.size();
			queueSem.release();
			return result;
		} catch (InterruptedException e) {
			return 0;
		}
	}

	public synchronized boolean move(double speedX, double speedY) {
		MoverConfig movement = new MoverConfig();
		movement.x = speedX;
		movement.y = speedY;
		movement.mode = Mode.MOVE_VECTOR;

		if (!pushMovement(movement))
			return false;

		// Let the mover know it has a new job
		jobSem.release();
		return true;
	}

	private void doMove(double speedX, double speedY) {
		System.out.println("Doing move: X=" + speedX + ", Y=" + speedY);
	}

	public synchronized boolean moveTo(double x, double y) {
		MoverConfig movement = new MoverConfig();
		movement.x = x;
		movement.y = y;
		movement.mode = Mode.MOVE_TO;

		if (!pushMovement(movement))
			return false;

		// Let the mover know it has a new job
		jobSem.release();
		return true;
	}

	private void doMoveTo(double x, double y) {
		System.out.println("Doing moveTo: X=" + x + ", Y=" + y);
		for (int i = 0; i < 50; ++i) {
			try {
				safeSleep(42);
			} catch (InterruptedException e) {
				System.out.println("Failed to sleep");
				e.printStackTrace();
			}
		}
	}

	public synchronized boolean delay(long milliseconds) {
		MoverConfig movement = new MoverConfig();
		movement.milliseconds = milliseconds;
		movement.mode = Mode.DELAY;

		if (!pushMovement(movement))
			return false;

		// Let the mover know it has a new job
		jobSem.release();
		return true;
	}
}
