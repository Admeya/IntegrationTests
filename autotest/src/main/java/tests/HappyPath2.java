package tests;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.log4testng.Logger;

public class HappyPath2 {
	final static Logger logger = Logger.getLogger(HappyPath2.class);

	@BeforeClass
	public void Init() {
		
	}

	@Test(description = "method1 HappyPath2")
	public void meth1() {
		System.out.println("First method Second Happy path test");
		ProjectHelper.sleepTimeout();
	}

	@Test(dependsOnMethods = { "meth1" }, description = "method2 HappyPath2")
	public void meth2() {
		System.out.println("Second method Second Happy path test");
		ProjectHelper.sleepTimeout();
	}

	@Test(dependsOnMethods = { "meth2" }, description = "method3 HappyPath2")
	public void meth3() {
		System.out.println("Third method Second Happy path test");
		ProjectHelper.sleepTimeout();
	}

	@Test(dependsOnMethods = { "meth3" }, description = "method4 HappyPath2")
	public void meth4() {
		System.out.println("Fourth method Second Happy path test");
		ProjectHelper.sleepTimeout();
	}

	@Test(dependsOnMethods = { "meth4" }, description = "method5 HappyPath2")
	public void meth5() {
		System.out.println("Fifth method Second Happy path test");
		ProjectHelper.sleepTimeout();
	}
}
