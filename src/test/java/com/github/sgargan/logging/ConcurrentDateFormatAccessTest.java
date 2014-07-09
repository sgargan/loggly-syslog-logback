package com.github.sgargan.logging;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.text.ParseException;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ConcurrentDateFormatAccessTest extends TestCase {

  @Test
  public void testConncurrentAccess() throws InterruptedException {

    int workers = 10;
    ExecutorService executorService = Executors.newFixedThreadPool(10);

    ConcurrentDateFormatAccess sut = new ConcurrentDateFormatAccess("yyyy-MM-dd'T'HH:mm:ssXXX");

    for (int x = 0; x <= workers; x++) {
      executorService.execute(new DateFormattingWorker(sut));
    }
    executorService.awaitTermination(3, TimeUnit.SECONDS);
    executorService.shutdown();
  }

  private class DateFormattingWorker implements Runnable {

    private ConcurrentDateFormatAccess formatter;

    public DateFormattingWorker(ConcurrentDateFormatAccess sut) {
      this.formatter = sut;
    }

    public void run() {
      for (int x = 1; x < 1000; x++) {
        Date date = new Date(x * 1000);
        String formatted = formatter.format(date);
        try {
          long time = formatter.parse(formatted).getTime();
          Assert.assertEquals(date.getTime(), time);
        } catch (ParseException e) {
          Assert.fail("Could not round trip convert date");
        }
      }
    }
  }


}
