package com.fico.vtb.config;

import com.fico.vtb.test.core.db.TestDB;
import com.fico.vtb.test.core.util.ConfigProperties;
import org.testng.annotations.Factory;
import org.testng.annotations.Listeners;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Peter Shubenok
 */
@Listeners({SuiteListener.class})
public class TestsFactory {

  public TestsFactory() {
    ConfigProperties.init();
    new TestDB().checkDB();
  }

  @Factory(dataProvider = "getAll", dataProviderClass = ApplicationsDataProvider.class)
  public Object[] init(String className, String path) {
    try {
      Constructor<?> c = Class.forName(className).getDeclaredConstructor(String.class);
      c.setAccessible(true);

      return new Object[]{c.newInstance(new Object[]{path})};
    } catch (NoSuchMethodException e) {
      return tryInitNoArgTest (className);
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }

    return null;
  }

  private Object[] tryInitNoArgTest(String className) {
    try {
      Constructor<?> c = Class.forName(className).getDeclaredConstructor();
      c.setAccessible(true);
      return new Object[]{c.newInstance()};
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }

    return null;
  }
}
