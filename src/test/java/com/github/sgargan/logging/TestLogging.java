package com.github.sgargan.logging;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

@Ignore
public class TestLogging {

  @Test
  public void testLogging() throws Exception {

    Logger logger = LoggerFactory.getLogger(TestLogging.class);

    while(true){
      logger.info("At the bong the time will be {} .........Bong!", new Date() );
      logger.warn("here is a warning", new RuntimeException("Something went boink", new RuntimeException("nested", new RuntimeException("way down in here"))));
      logger.error("here is an error", new RuntimeException("Something went boink"));
      Thread.sleep(5000);
    }

  }
}
