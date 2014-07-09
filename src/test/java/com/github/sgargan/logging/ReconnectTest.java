package com.github.sgargan.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.net.SocketException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReconnectTest {

  @Test
  public void testReconnect() throws Exception {
    SyslogTcpStream mock = mock(SyslogTcpStream.class);

    LogglySyslogAppender.SyslogStreamFactory mockFactory = mock(LogglySyslogAppender.SyslogStreamFactory.class);
    when(mockFactory.createStream(anyString(), anyInt())).thenReturn(mock);

    LogglySyslogAppender appender = new LogglySyslogAppender();
    appender.setSyslogHost("logs-01.loggly.com");
    appender.setPort(514);
    appender.setReconnectBackoff(50);
    appender.factory = mockFactory;
    appender.setFacility("USER");
    appender.setContext(mock(Context.class));
    appender.start();

    doAnswer(new Answer<Object>() {
      int failures = 5;
      public Object answer(InvocationOnMock invocation) throws SocketException {
        if( --failures >= 0) {
          throw new SocketException("Connection error");
        } else if (failures < -10) {
          failures = 5;
        }
        return null;
      }
    }).when(mock).write(any(byte[].class));


    ILoggingEvent event = mock(ILoggingEvent.class);
    when(event.getLevel()).thenReturn(Level.INFO);

    for(int x = 0; x < 50; x ++) {
      appender.append(event);
      Thread.sleep(250);
    }

  }
}
