package mcdbg;

import java.util.concurrent.Semaphore;

public class Mover extends Thread {
	private enum Mode {
		IDLE, TASK_A, TASK_B, TASK_C
	};

	private Mode mode = Mode.IDLE;
	private boolean die = false;

	private Semaphore jobSem = new Semaphore(0, true);
	private Semaphore killSem = new Semaphore(0, true);
	private Semaphore waitSem = new Semaphore(0, true);

	public Mover() {
		super("mover");
	}
	
	@Override
	public void run() {
		System.out.println(Thread.currentThread().getName() + ": Confirmed start");
		try {
			while (!die) {
				System.out.println(Thread.currentThread().getName() + ": Acquiring jobSem");
				jobSem.acquire();
				System.out.println(Thread.currentThread().getName() + ": Acquired jobSem");
				switch (mode) {
				case IDLE:
					break;
				case TASK_A:
					doTaskA();
					break;
				case TASK_B:
					doTaskB();
					break;
				case TASK_C:
					doTaskC();
					break;
				default:
					System.out.println("DERP! Unknown movement mode specified");
					assert (false);
				}
				if (waitSem.hasQueuedThreads()) {
					System.out.println(Thread.currentThread().getName() + ": Releasing waitSem");
					waitSem.release();
					System.out.println(Thread.currentThread().getName() + ": Released waitSem");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(Thread.currentThread().getName() + ": Releasing killSem");
		killSem.release();
	}

	public void kill() {
		die = true;
		System.out.println(Thread.currentThread().getName() + ": Acquiring killSem");
		killSem.acquireUninterruptibly();
	}

	public void waitForCompletion() throws InterruptedException {
		System.out.println(Thread.currentThread().getName() + ": Acquiring waitSem");
		waitSem.acquire();
		System.out.println(Thread.currentThread().getName() + ": Acquired waitSem");
	}

	public void taskA() {
		mode = Mode.TASK_A;
		System.out.println(Thread.currentThread().getName() + ": Releasing jobSem");
		jobSem.release();
		System.out.println(Thread.currentThread().getName() + ": Released jobSem");
	}

	private void doTaskA() {
		System.out.println("Doing task A!");
	}

	public void taskB() {
		mode = Mode.TASK_B;
		System.out.println(Thread.currentThread().getName() + ": Releasing jobSem");
		jobSem.release();
		System.out.println(Thread.currentThread().getName() + ": Released jobSem");
	}

	private void doTaskB() {
		System.out.println("Doing task B!");
	}

	public void taskC() {
		mode = Mode.TASK_C;
		System.out.println(Thread.currentThread().getName() + ": Releasing jobSem");
		jobSem.release();
		System.out.println(Thread.currentThread().getName() + ": Released jobSem");
	}

	private void doTaskC() {
		System.out.println("Doing task C!");
	}
}
