package safesleep;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class Runner {
	private static final int NUM_TESTS = 10;
	private static final long TEST_LENGTH = 1000L;
	private static final long SLEEP_INTERVAL_1 = 5L;
	private static final long SLEEP_INTERVAL_2 = 10L;
	private static final long SLEEP_INTERVAL_3 = 15L;
	private static final long SLEEP_INTERVAL_4 = 20L;

	/**
	 * @param args
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public static void main(String[] args) throws InterruptedException,
			ExecutionException {
		ExecutorService executor = Executors.newCachedThreadPool();

		Control control1 = new Control(SLEEP_INTERVAL_1);
		Control control2 = new Control(SLEEP_INTERVAL_2);
		Control control3 = new Control(SLEEP_INTERVAL_3);
		Control control4 = new Control(SLEEP_INTERVAL_4);
		SafeSleeper sleeper1 = new SafeSleeper(SLEEP_INTERVAL_1);
		SafeSleeper sleeper2 = new SafeSleeper(SLEEP_INTERVAL_2);
		SafeSleeper sleeper3 = new SafeSleeper(SLEEP_INTERVAL_3);
		SafeSleeper sleeper4 = new SafeSleeper(SLEEP_INTERVAL_4);

		FutureTask<Long> task1;
		FutureTask<Long> task2;
		FutureTask<Long> task3;
		FutureTask<Long> task4;

		long control1Results = 0;
		long control2Results = 0;
		long control3Results = 0;
		long control4Results = 0;
		long sleeper1Results = 0;
		long sleeper2Results = 0;
		long sleeper3Results = 0;
		long sleeper4Results = 0;
		
		System.out.println("Benchmarking SafeSleeper...");
		System.out.println("Running control:");
		for (int i = 0; i < NUM_TESTS; ++i) {
			String idx = String.valueOf(i + 1);
			System.out.print(idx);
			for (int k = 0; k < 5 - idx.length(); ++k)
				System.out.print('.');
			if ((i + 1) % 10 == 0)
				System.out.println();
			control1.reset();
			control2.reset();
			control3.reset();
			control4.reset();

			task1 = new FutureTask<Long>(control1);
			task2 = new FutureTask<Long>(control2);
			task3 = new FutureTask<Long>(control3);
			task4 = new FutureTask<Long>(control4);
			executor.execute(task1);
			executor.execute(task2);
			executor.execute(task3);
			executor.execute(task4);

			Thread.sleep(TEST_LENGTH);

			control1.stop();
			control2.stop();
			control3.stop();
			control4.stop();

			control1Results += task1.get();
			control2Results += task2.get();
			control3Results += task3.get();
			control4Results += task4.get();
		}
		control1Results /= NUM_TESTS;
		control2Results /= NUM_TESTS;
		control3Results /= NUM_TESTS;
		control4Results /= NUM_TESTS;

		System.out.println("\nRunning SafeSleep:");

		for (int i = 0; i < NUM_TESTS; ++i) {
			String idx = String.valueOf(i + 1);
			System.out.print(idx);
			for (int k = 0; k < 5 - idx.length(); ++k)
				System.out.print('.');
			if ((i + 1) % 10 == 0)
				System.out.println();
			sleeper1.reset();
			sleeper2.reset();
			sleeper3.reset();
			sleeper4.reset();

			task1 = new FutureTask<Long>(sleeper1);
			task2 = new FutureTask<Long>(sleeper2);
			task3 = new FutureTask<Long>(sleeper3);
			task4 = new FutureTask<Long>(sleeper4);
			executor.execute(task1);
			executor.execute(task2);
			executor.execute(task3);
			executor.execute(task4);

			Thread.sleep(TEST_LENGTH);

			sleeper1.stop();
			sleeper2.stop();
			sleeper3.stop();
			sleeper4.stop();

			sleeper1Results += task1.get();
			sleeper2Results += task2.get();
			sleeper3Results += task3.get();
			sleeper4Results += task4.get();
		}
		sleeper1Results /= NUM_TESTS;
		sleeper2Results /= NUM_TESTS;
		sleeper3Results /= NUM_TESTS;
		sleeper4Results /= NUM_TESTS;

		System.out.println("\nTests completed.");
		System.out.println("Results: ");
		System.out.println("  Interval 1: " + SLEEP_INTERVAL_1 + " ms");
		System.out.println("  Interval 2: " + SLEEP_INTERVAL_2 + " ms");
		System.out.println("  Interval 3: " + SLEEP_INTERVAL_3 + " ms");
		System.out.println("  Interval 4: " + SLEEP_INTERVAL_4 + " ms");
		System.out.println();
		System.out.println("  Average runtime for control1: " + control1Results
				+ " nanoseconds");
		System.out.println("  Average runtime for control2: " + control2Results
				+ " nanoseconds");
		System.out.println("  Average runtime for control3: " + control3Results
				+ " nanoseconds");
		System.out.println("  Average runtime for control4: " + control4Results
				+ " nanoseconds");
		System.out.println();
		System.out.println("  Average runtime for sleeper1: " + sleeper1Results
				+ " nanoseconds");
		System.out.println("  Average runtime for sleeper2: " + sleeper2Results
				+ " nanoseconds");
		System.out.println("  Average runtime for sleeper3: " + sleeper3Results
				+ " nanoseconds");
		System.out.println("  Average runtime for sleeper4: " + sleeper4Results
				+ " nanoseconds");
		executor.shutdown();
	}
}

abstract class SleepTest implements Callable<Long> {
	protected volatile boolean stop = false;
	protected final long sleepPeriod;

	public SleepTest(long sleepPeriod) {
		this.sleepPeriod = sleepPeriod;
	}

	public void stop() {
		this.stop = true;
	}

	public void reset() {
		this.stop = false;
	}
}

class Control extends SleepTest {
	public Control(long sleepPeriod) {
		super(sleepPeriod);
	}

	public Long call() {
		try {
			long timeBefore = 0, timeAfter = 0, sum = 0, n = 0;
			while (!stop) {
				timeBefore = System.nanoTime();
				Thread.sleep(sleepPeriod);
				timeAfter = System.nanoTime();
				sum += (timeAfter - timeBefore);
				++n;
			}
			if (n != 0) {
				sum /= n;
				return sum;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return -1L;
	}
}

class SafeSleeper extends SleepTest {
	public SafeSleeper(long sleepPeriod) {		
		super(sleepPeriod);
	}

	public Long call() {
		try {
			long timeBefore = 0, timeAfter = 0, sum = 0, n = 0;
			while (!stop) {
				timeBefore = System.nanoTime();
				SafeSleep.sleep(sleepPeriod);
				timeAfter = System.nanoTime();
				sum += (timeAfter - timeBefore);
				++n;
			}
			if (n != 0) {
				sum /= n;
				return sum;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return -1L;
	}
}