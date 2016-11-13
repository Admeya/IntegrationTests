package tests;

public class ProjectHelper {

	public static void sleepTimeout() {
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}
}
