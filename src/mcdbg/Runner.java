package mcdbg;

public class Runner {

	public static void main(String[] args) {
		Mover mover = new Mover();
		mover.start();
		System.out.println("Mover thread started");
		try {
			System.out.println("Calling taskA");
			mover.taskA();
			System.out.println("Calling wait");
			mover.waitForCompletion();
			System.out.println("Calling taskB");
			mover.taskB();
			System.out.println("Calling wait");
			mover.waitForCompletion();
			System.out.println("Calling taskC");
			mover.taskC();
			System.out.println("Calling wait");
			mover.waitForCompletion();
			System.out.println("Calling kill");
			mover.kill();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
