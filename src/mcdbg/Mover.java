package mcdbg;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * TODO: Add a queue to permit multiple threads waiting on the mover thread to
 * complete (will need to store the point in the movement queue it's waiting
 * on). <br/>
 * A movement class, that provides calculations for different move commands for
 * the robot.
 * 
 * @author Jakov Smelkin - movement
 * @author Alex Adams (s1046358) - threading
 */
public class Mover extends Thread {

	/**
	 * A method to call: <br/>
	 * {@link Mover#doMove(double speedX, double speedY)}, <br/>
	 * {@link Mover#doMove(double angle)} ,<br/>
	 * {@link Mover#doMoveTo(double x, double y)} , <br/>
	 * {@link Mover#doMoveToAndStop(double x, double y)} , <br/>
	 * {@link Mover#doMoveToAStar(double x, double y, boolean avoidBall)} ,<br/>
	 * {@link Mover#doMoveTowards (double x, double y)} , <br/>
	 * {@link Mover#doRotate (double angle)}
	 */
	private enum Mode {
		STOP, KICK, DELAY, MOVE_VECTOR, MOVE_ANGLE, MOVE_TO, MOVE_TO_STOP, MOVE_TO_ASTAR, MOVE_TOWARDS, ROTATE
	};

	/** Settings info class to permit queueing of movements */
	private class MoverConfig {
		public double x = 0;
		public double y = 0;
		public double angle = 0;
		public boolean avoidBall = false;
		public boolean avoidEnemy = false;
		public long milliseconds = 0;

		public Mode mode;
	};

	private boolean running = false;
	private boolean interruptMove = false;
	private boolean die = false;

	private ConcurrentLinkedQueue<MoverConfig> moveQueue = new ConcurrentLinkedQueue<MoverConfig>();
	private ReentrantLock queueLock = new ReentrantLock(true);

	private Semaphore jobSem = new Semaphore(0, true);
	private Semaphore waitSem = new Semaphore(0, true);

	final ScheduledExecutorService sleepScheduler = Executors
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

	public Mover() {
		super("mover");
	}

	/**
	 * Repeatedly tries to push the movement onto the move queue, giving up
	 * after 10 attempts
	 * 
	 * @param movement
	 *            The movement to push onto the queue
	 * @return true if the movement was successfully pushed, false otherwise
	 */
	private boolean pushMovement(MoverConfig movement) {
		int pushAttempts = 0;
		try {
			queueLock.lockInterruptibly();
		} catch (InterruptedException e) {
			return false;
		}
		// Try to push the movement 10 times before giving up
		while (!moveQueue.offer(movement) && pushAttempts < 10)
			++pushAttempts;
		queueLock.unlock();
		// If we gave up, return false to indicate it
		if (pushAttempts >= 10)
			return false;
		return true;
	}

	/**
	 * Wakes up any threads waiting on a movement queue to complete
	 */
	private void wakeUpWaitingThreads() {
		System.out.println("Mover: Waking up waiters");
		waitSem.release();
		running = false;
	}

	/**
	 * Processes a single movement
	 * 
	 * @param movement
	 *            The movement to process
	 * @throws InterruptedException
	 *             If the RobotMover thread is interrupted
	 */
	private void processMovement(MoverConfig movement)
			throws InterruptedException {
		try {
			switch (movement.mode) {
			case STOP:
				System.out.println("Stopping robot");
				break;
			case KICK:
				System.out.println("Kicking!");
				break;
			case DELAY:
				System.out.println("Waiting for " + movement.milliseconds
						+ " milliseconds");
				safeSleep(movement.milliseconds);
				break;
			case MOVE_VECTOR:
				System.out.println("Moving at speed (" + movement.x + ", "
						+ movement.y + ")");
				doMove(movement.x, movement.y);
				break;
			case MOVE_ANGLE:
				System.out.println("Moving at angle " + movement.angle
						+ " radians (" + Math.toDegrees(movement.angle)
						+ " degrees)");
				doMove(movement.angle);
				break;
			case MOVE_TO:
				System.out.println("Moving to point (" + movement.x + ", "
						+ movement.y + ")");
				doMoveTo(movement.x, movement.y);
				break;
			case MOVE_TO_STOP:
				System.out.println("Moving to point (" + movement.x + ", "
						+ movement.y + ") and stopping");
				doMoveTo(movement.x, movement.y);
				System.out.println("Stopping robot");
				break;
			case MOVE_TOWARDS:
				System.out.println("Moving towards point (" + movement.x + ", "
						+ movement.y + ")");
				doMoveTowards(movement.x, movement.y);
				break;
			case MOVE_TO_ASTAR:
				System.out.println("Moving to point (" + movement.x + ", "
						+ movement.y + ") using A*");
				doMoveToAStar(movement.x, movement.y, movement.avoidBall,
						movement.avoidEnemy);
				break;
			case ROTATE:
				System.out.println("Rotating by " + movement.angle
						+ " radians (" + Math.toDegrees(movement.angle)
						+ " degrees)");
				doRotate(movement.angle);
				break;
			default:
				System.out.println("DERP! Unknown movement mode specified");
				assert (false);
			}
		} catch (Exception e) {
			System.out.println("Error occurred executing job: ");
			e.printStackTrace();
			resetQueue();
		}
	}

	/**
	 * Runner for our movement
	 * 
	 * @see Thread#run()
	 */
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
				// Wait for next movement operation
				jobSem.acquire();
				// Clear the movement interrupt flag for the new movement
				interruptMove = false;
				// Set the running flag to true for busy-waiting
				running = true;

				queueLock.lockInterruptibly();
				if (!moveQueue.isEmpty() && !die) {
					MoverConfig movement = moveQueue.poll();
					queueLock.unlock();
					assert (movement != null) : "moveQueue.poll() returned null when non-empty";
					assert (movement.mode != null) : "invalid movement generated";

					processMovement(movement);
				} else {
					queueLock.unlock();
				}

				// If we just did the last move in the queue, wake up the
				// waiting threads
				if (moveQueue.isEmpty())
					wakeUpWaitingThreads();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			wakeUpWaitingThreads();
		}
		// Stop the robot when the movement thread has been told to exit
		System.out.println("Stopping robot");
		// Kill the thread pool that manages thread-safe sleeping
		sleepScheduler.shutdown();
	}

	/**
	 * Tells the move thread to stop executing, and clears the movement queue to
	 * ensure the mover thread dies quickly
	 * 
	 * @throws InterruptedException
	 */
	public void kill() throws InterruptedException {
		die = true;
		resetQueue();
		jobSem.release();
	}

	/**
	 * Triggers an interrupt in movement
	 */
	public void interruptMove() {
		interruptMove = true;
	}

	/**
	 * Resets the queue of movements to allow for an immediate change in planned
	 * movements <br/>
	 * NOTE: This does not interrupt an active movement
	 * 
	 * @throws InterruptedException
	 */
	public void resetQueue() throws InterruptedException {
		// Block changes in the queue until the queue is finished
		// resetting
		queueLock.lockInterruptibly();
		// Reset the job semaphore since there will be no more queued jobs
		jobSem.drainPermits();
		if (moveQueue.isEmpty()) {
			queueLock.unlock();
			return;
		}

		moveQueue.clear();
		queueLock.unlock();
	}

	/**
	 * Checks if the mover is running jobs
	 * 
	 * @return true if the mover is doing something, false otherwise
	 */
	public boolean isRunning() {
		return running;
	}

	/**
	 * Checks if the mover has queued jobs, not including the one currently
	 * running
	 * 
	 * @return true if there are queued jobs, false otherwise
	 */
	public boolean hasQueuedJobs() {
		try {
			queueLock.lockInterruptibly();
			boolean result = !moveQueue.isEmpty();
			queueLock.unlock();
			return result;
		} catch (InterruptedException e) {
			// InterruptedException can only occur if the thread has been
			// interrupted - therefore there can't be jobs waiting
			return false;
		}
	}

	/**
	 * @return The number of jobs currently queued, not including the one
	 *         currently running
	 */
	public int numQueuedJobs() {
		// Get a lock on the queue to prevent changes while determining how many
		// jobs there are
		try {
			queueLock.lockInterruptibly();
			int result = moveQueue.size();
			queueLock.unlock();
			return result;
		} catch (InterruptedException e) {
			return 0;
		}
	}

	/**
	 * Waits for the movement to complete before returning
	 */
	public void waitForCompletion() throws InterruptedException {
		waitSem.acquire();
	}

	/**
	 * A general move function as seen from the position of the robot.<br/>
	 * Speeds take values between -100 and 100.<br/>
	 * NOTE: this movement will complete almost immediately
	 * 
	 * @param speedX
	 *            Speed right (for positive values) or left (for negative ones).
	 * @param speedY
	 *            Speed forward (for positive values) or backward (for negative
	 *            ones).
	 * @return true if the move was successfully queued, false otherwise
	 */
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

	/**
	 * Internal method to execute a call to move(speedX, speedY)
	 * 
	 * @param speedX
	 * @param speedY
	 * @see #move(double speedX, double speedY)
	 */
	private void doMove(double speedX, double speedY) {
		System.out.println("Doing move(" + speedX + ", " + speedY + ")");
	}

	/**
	 * A general move function where you specify a clockwise angle from the
	 * front of the robot to move at.<br/>
	 * NOTE: this movement will complete almost immediately
	 * 
	 * @param angle
	 *            Angle, in radians (0 to 2*PI)
	 * @return true if the move was successfully queued, false otherwise
	 */
	public synchronized boolean move(double angle) {
		MoverConfig movement = new MoverConfig();
		movement.angle = angle;
		movement.mode = Mode.MOVE_ANGLE;

		if (!pushMovement(movement))
			return false;

		// Let the mover know it has a new job
		jobSem.release();
		return true;
	}

	/**
	 * Internal method to execute a call to move(angle)
	 * 
	 * @param angle
	 * @see #move(double angle)
	 */
	private void doMove(double angle) {
		System.out.println("Doing move(" + angle + ")");
	}

	/**
	 * Moves to a point on a video stream (within a certain margin). Does not
	 * stop when reaches the point.
	 * 
	 * @param x
	 *            Move to position x units down from top left corner of the
	 *            video feed
	 * @param y
	 *            Move to position y units right from top left corner of the
	 *            video feed
	 * @return true if the move was successfully queued, false otherwise
	 * 
	 * @see #moveToAndStop(double x, double y)
	 * @see #moveTowards(double x, double y)
	 * @see #waitForCompletion()
	 */
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

	/**
	 * Internal method to execute a call to moveTo(x, y)
	 * 
	 * @param x
	 * @param y
	 * @see #moveTo(double x, double y)
	 */
	private void doMoveTo(double x, double y) {
		System.out.println("Doing doMoveTo(" + x + ", " + y + ")");
		int i = 0;
		while (i < 20 && !interruptMove) {
			// Not to send unnecessary commands
			// 42 because it's The Answer to the Ultimate Question of Life, the
			// Universe, and Everything
			try {
				safeSleep(42);
			} catch (InterruptedException e) {
				System.out.println("Failed to sleep");
				e.printStackTrace();
			}
			System.out.print("   ");
			doMoveTowards(x, y);
			i++;
		}
	}

	/**
	 * Moves to a point on a video stream (within a certain margin) and stops.
	 * 
	 * @param x
	 *            Move to position x units down from top left corner of the
	 *            video feed
	 * @param y
	 *            Move to position y units right from top left corner of the
	 *            video feed
	 * @return true if the move was successfully queued, false otherwise
	 * 
	 * @see #moveTo(double, double)
	 * @see #waitForCompletion()
	 */
	public synchronized boolean moveToAndStop(double x, double y) {
		MoverConfig movement = new MoverConfig();
		movement.x = x;
		movement.y = y;
		movement.mode = Mode.MOVE_TO_STOP;

		if (!pushMovement(movement))
			return false;

		// Let the mover know it has a new job
		jobSem.release();
		return true;
	}

	/**
	 * Starts moving to the direction of the point<br/>
	 * NOTE: this movement will complete almost immediately
	 * 
	 * @param x
	 *            Move to position x units down from top left corner of the
	 *            video feed
	 * @param y
	 *            Move to position y units right from top left corner of the
	 *            video feed
	 * @return true if the move was successfully queued, false otherwise
	 * @see #moveTo(double, double)
	 */
	public synchronized boolean moveTowards(double x, double y) {
		MoverConfig movement = new MoverConfig();
		movement.x = x;
		movement.y = y;
		movement.mode = Mode.MOVE_TOWARDS;

		if (!pushMovement(movement))
			return false;

		// Let the mover know it has a new job
		jobSem.release();
		return true;
	}

	/**
	 * Internal method to execute a call to moveTowards(x, y)
	 * 
	 * @param x
	 * @param y
	 * @see #moveTowards(double x, double y)
	 */
	private void doMoveTowards(double x, double y) {
		System.out.println("Doing moveTowards(" + x + ", " + y + ")");
	}

	/**
	 * Move to a point (x,y) while avoiding point enemy robot and optionally the
	 * ball. Should go in an arc by default.
	 * 
	 * @param x
	 *            Point in the X axis to move to.
	 * @param y
	 *            Point in the Y axis to move to.
	 * @param avoidBall
	 *            Should A* avoid the ball.
	 * @return true if the move was successfully queued, false otherwise
	 * 
	 * @see #waitForCompletion()
	 */
	public synchronized boolean moveToAStar(double x, double y,
			boolean avoidBall, boolean avoidEnemy) {
		MoverConfig movement = new MoverConfig();
		movement.x = x;
		movement.y = y;
		movement.avoidBall = avoidBall;
		movement.avoidEnemy = avoidEnemy;
		movement.mode = Mode.MOVE_TO_ASTAR;

		if (!pushMovement(movement))
			return false;

		// Let the mover know it has a new job
		jobSem.release();
		return true;
	}

	/**
	 * Internal method to execute a call to moveTowards(x, y)
	 * 
	 * @param x
	 *            Point in the X axis to move to
	 * @param y
	 *            Point in the Y axis to move to
	 * 
	 * @see #moveToAStar(double x, double y)
	 */
	private void doMoveToAStar(double x, double y, boolean avoidball,
			boolean avoidenemy) {
		System.out.println("Doing moveToAStar(" + x + ", " + y + ", "
				+ avoidball + ", " + avoidenemy + ")");
		int i = 0;
		while (i < 10 && !interruptMove) {
			System.out.print("  ");
			doMoveTo(x, y);
			i++;
		}
	}

	/**
	 * Calls robot controller to rotate the robot by an angle <br/>
	 * 
	 * @param angleRad
	 *            clockwise angle to rotate (in Radians)
	 * @return true if the move was successfully queued, false otherwise
	 * 
	 * @see #waitForCompletion()
	 */
	public synchronized boolean rotate(double angleRad) {
		MoverConfig movement = new MoverConfig();
		movement.angle = angleRad;
		movement.mode = Mode.ROTATE;

		if (!pushMovement(movement))
			return false;

		// Let the mover know it has a new job
		jobSem.release();
		return true;
	}

	/**
	 * Internal method to execute a call to rotate(angle)
	 * 
	 * @param angleRad
	 *            clockwise angle to rotate (in Radians)
	 */
	private void doRotate(double angleRad) {
		System.out.println("Doing rotate(" + angleRad + ")");
		try {
			safeSleep(5 * (long) Math.toDegrees(angleRad));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Stops the robot
	 * 
	 * @return true if the move was successfully queued, false otherwise
	 * 
	 * @see #waitForCompletion()
	 */
	public synchronized boolean stopRobot() {
		MoverConfig movement = new MoverConfig();
		movement.mode = Mode.STOP;

		if (!pushMovement(movement))
			return false;

		// Let the mover know it has a new job
		jobSem.release();
		return true;
	}

	/**
	 * Makes the robot kick
	 * 
	 * @return true if the move was successfully queued, false otherwise
	 * 
	 * @see #waitForCompletion()
	 */
	public synchronized boolean kick() {
		MoverConfig movement = new MoverConfig();
		movement.mode = Mode.KICK;

		if (!pushMovement(movement))
			return false;

		// Let the mover know it has a new job
		jobSem.release();
		return true;
	}

	/**
	 * Makes the mover perform a delay job, where it will sleep for the
	 * specified period <br/>
	 * WARNING: Once a delay job has started, it cannot be interrupted
	 * 
	 * @param milliseconds
	 *            The time in milliseconds to sleep for
	 * 
	 * @return true if the delay was successfully queued, false otherwise
	 */
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
