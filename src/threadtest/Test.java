package threadtest;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Test {
	private static final ScheduledExecutorService sleepScheduler = Executors
			.newScheduledThreadPool(1);
	
	/** Thread-safe sleep */
	private static void safeSleep(long millis) throws InterruptedException {
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
	
	public static void main(String[] args) throws InterruptedException {
		// Clear initial lag
		for (int i = 0; i < 3; ++i) {
			Test.safeSleep(50);
			Thread.sleep(50);
		}
		long startTime = System.currentTimeMillis();
		for (int i = 0; i < 10; ++i) {
			Test.safeSleep(50);
			long endTime = System.currentTimeMillis();
			System.out.println("Time passed during safeSleep: " + (endTime - startTime));
			startTime = endTime;
			Thread.sleep(50);
			endTime = System.currentTimeMillis();
			System.out.println("Time passed during sleep: " + (endTime - startTime));
			startTime = endTime;
		}
	}
}
