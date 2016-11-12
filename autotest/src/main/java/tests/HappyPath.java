package tests;

import org.testng.annotations.Test;
import org.testng.log4testng.Logger;

public class HappyPath {
	final static Logger logger = Logger.getLogger(HappyPath.class);

	@Test(description = "method1 HappyPath1")
	public void meth1() {
		System.out.println("First method First Happy path test");
	}

	@Test(dependsOnMethods = { "meth1" }, description = "method2 HappyPath1")
	public void meth2() {
		System.out.println("Second method First Happy path test");
	}

	@Test(dependsOnMethods = { "meth2" }, description = "method3 HappyPath1")
	public void meth3() {
		System.out.println("Third method First Happy path test");
	}

	@Test(dependsOnMethods = { "meth3" }, description = "method4 HappyPath1")
	public void meth4() {
		System.out.println("Fourth method First Happy path test");
	}

	@Test(dependsOnMethods = { "meth4" }, description = "method5 HappyPath1")
	public void meth5() {
		System.out.println("Fifth method First Happy path test");
	}
}
