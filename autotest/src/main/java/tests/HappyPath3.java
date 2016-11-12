package tests;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.log4testng.Logger;

public class HappyPath3 {
	final static Logger logger = Logger.getLogger(HappyPath3.class);

	@BeforeClass
	public void Init() {

	}

	@Test(description = "method1 HappyPath3")
	public void meth1() {
		System.out.println("First method Third Happy path test");
	}

	@Test(dependsOnMethods = { "meth1" }, description = "method2 HappyPath3")
	public void meth2() {
		System.out.println("Second method Third Happy path test");
	}

	@Test(dependsOnMethods = { "meth2" }, description = "method3 HappyPath3")
	public void meth3() {
		System.out.println("Third method Third Happy path test");
	}

	@Test(dependsOnMethods = { "meth3" }, description = "method4 HappyPath3")
	public void meth4() {
		System.out.println("Fourth method Third Happy path test");
	}

	@Test(dependsOnMethods = { "meth4" }, description = "method5 HappyPath3")
	public void meth5() {
		System.out.println("Fifth method Third Happy path test");
	}
}
