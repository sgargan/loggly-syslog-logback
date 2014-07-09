package com.github.sgargan.logging;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.util.LevelToSyslogSeverity;
import ch.qos.logback.contrib.jackson.JacksonJsonFormatter;
import ch.qos.logback.contrib.json.classic.JsonLayout;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.net.SyslogAppenderBase;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LogglySyslogAppender extends SyslogAppenderBase<ILoggingEvent> {

  private String apiToken;

  private String tags;

  private String appName = "default";

  private SyslogTcpStream syslog;

  private LogglySyslogLayout layout;

  private Lock reconnectLock = new ReentrantLock();

  private long lastReconnect = 0;
  private int reconnectBackoff = 15000;

  SyslogStreamFactory factory = new SyslogStreamFactory() {
    @Override
    public SyslogTcpStream createStream(String host, int port) throws IOException {
      return new SyslogTcpStream(host, port);
    }
  };

  @Override
  public Layout<ILoggingEvent> buildLayout() {
    layout = new LogglySyslogLayout();
    layout.setContext(getContext());
    layout.start();
    return layout;
  }

  public void start() {
    int errorCount = 0;
    if (getFacility() == null) {
      addError("The Facility option is mandatory");
      errorCount++;
    }

    try {
      syslog = factory.createStream(getSyslogHost(), getPort());
      lastReconnect = System.currentTimeMillis();
    } catch (SocketException e) {
      addWarn("Failed to bind to a random datagram socket. Will try to reconnect later.", e);
    } catch (UnknownHostException e) {
      addError("Could not create SyslogWriter", e);
      errorCount++;
    } catch (IOException e) {
      addError("Could not create SyslogWriter", e);
      errorCount++;
    }
    buildLayout();

    if (errorCount == 0) {
      super.start();
    }
  }


  @Override
  protected void append(ILoggingEvent eventObject) {
    if (!isStarted()) {
      return;
    }

    try {
      sendEvent(eventObject);
    } catch (SocketException e) {
      addWarn("Failed to send diagram to " + getSyslogHost(), e);
      reconnect(eventObject);
    } catch (IOException ioe) {
      addError("Failed to send diagram to " + getSyslogHost(), ioe);
    }
  }

  private void sendEvent(ILoggingEvent eventObject) throws IOException {
    String msg = layout.doLayout(eventObject);
    if (msg == null) {
      return;
    }
    syslog.write(msg.getBytes());
    syslog.flush();
  }

  private void reconnect(ILoggingEvent eventObject) {
    try {
      reconnectLock.lock();

      if (System.currentTimeMillis() - lastReconnect < reconnectBackoff) {
        // only reconnect once every reconnectBackoff milliseconds
        return;
      }

      try {
        syslog = factory.createStream(getSyslogHost(), getPort());
        sendEvent(eventObject);
        lastReconnect = System.currentTimeMillis();
        System.err.println("Reconnect succeeded");
      } catch (Exception e) {
        System.err.println("Reconnect failed " + e.getMessage());
      }

    } finally {
      reconnectLock.unlock();
    }
  }

  @Override
  public int getSeverityForEvent(Object eventObject) {
    ILoggingEvent event = (ILoggingEvent) eventObject;
    return LevelToSyslogSeverity.convert(event);
  }

  public void setApiToken(String apiToken) {
    this.apiToken = apiToken;
  }

  public void setAppName(String appName) {
    this.appName = appName;
  }

  public void setTags(String tags) {
    if (tags != null) {
      StringBuilder t = new StringBuilder();
      for (String tag : tags.split(",")) {
        t.append("tag=\"").append(tag).append("\" ");
      }
      this.tags = t.toString();
    }
  }

  public void setReconnectBackoff(int reconnectBackoff) {
    this.reconnectBackoff = reconnectBackoff;
  }

  public interface SyslogStreamFactory {

    public SyslogTcpStream createStream(String host, int port) throws IOException;
  }

  public class LogglySyslogLayout extends PatternLayout {

    private String rfc3339Format = "yyyy-MM-dd'T'HH:mm:ssXXX";

    private ConcurrentDateFormatAccess syslogFormat;
    private String localHostName;
    private int facility;
    private String pid;

    JsonLayout jsonLayout;

    public void start() {
      int errorCount = 0;

      String facilityStr = getFacility();
      if (facilityStr == null) {
        addError("was expecting a facility string as an option");
        return;
      }

      facility = SyslogAppenderBase.facilityStringToint(facilityStr);
      localHostName = getLocalHostname();

      try {
        // hours should be in 0-23, see also http://jira.qos.ch/browse/LBCLASSIC-48
        syslogFormat = new ConcurrentDateFormatAccess(rfc3339Format, new DateFormatSymbols(Locale.US));
      } catch (IllegalArgumentException e) {
        addError("Could not instantiate SimpleDateFormat", e);
        errorCount++;
      }

      jsonLayout = new JsonLayout();
      jsonLayout.setIncludeTimestamp(false);
      jsonLayout.setIncludeContextName(false);
      JacksonJsonFormatter jsonFormatter = new JacksonJsonFormatter();
      jsonFormatter.setPrettyPrint(true);
      jsonLayout.setJsonFormatter(jsonFormatter);
      jsonLayout.start();

      pid = getPID();

      if (errorCount == 0) {
        super.start();
      }
    }


    @Override
    public String doLayout(ILoggingEvent event) {
      StringBuilder sb = new StringBuilder();
      createSyslogHeader(event, sb);
      sb.append(jsonLayout.doLayout(event));
      sb.append("\n");
      return sb.toString();
    }

    private String getPID() {
      String pidStr = ManagementFactory.getRuntimeMXBean().getName();
      if (pidStr != null) {
        return pidStr.split("@")[0];
      }
      return "-1";
    }

    private void createSyslogHeader(ILoggingEvent event, StringBuilder sb) {
      sb.append("<");
      sb.append(facility + LevelToSyslogSeverity.convert(event));
      sb.append(">1 ");
      sb.append(computeTimeStampString(event.getTimeStamp()));
      sb.append(' ');
      sb.append(localHostName);
      sb.append(' ');
      sb.append(appName);
      sb.append(' ');
      sb.append(pid);

      sb.append(" [").append(apiToken).append("@41058 ").append(tags).append("] ");
    }

    /**
     * This method gets the network name of the machine we are running on.
     * Returns "UNKNOWN_LOCALHOST" in the unlikely case where the host name
     * cannot be found.
     *
     * @return String the name of the local host
     */
    public String getLocalHostname() {
      try {
        InetAddress addr = InetAddress.getLocalHost();
        return addr.getHostName();
      } catch (UnknownHostException uhe) {
        addError("Could not determine local host name", uhe);
        return "UNKNOWN_LOCALHOST";
      }
    }

    String computeTimeStampString(long now) {
        return syslogFormat.format(new Date(now));
    }
  }
}
