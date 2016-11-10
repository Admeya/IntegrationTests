package com.fico.vtb.config;

import com.fico.vtb.test.core.db.ConnectionFactory;
import org.apache.log4j.Logger;
import org.testng.ISuite;
import org.testng.ISuiteListener;

public class SuiteListener  implements ISuiteListener {
  final static Logger logger = Logger.getLogger(SuiteListener.class);

  @Override
  public void onStart(ISuite suite) {
    logger.info("Start of suite " + suite.getName());
  }

  @Override
  public void onFinish(ISuite suite) {
    logger.info("Finish of suite " + suite.getName());
    ConnectionFactory.closeConnection();
  }
}
