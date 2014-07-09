loggly-syslog-logback
=====================

A modification of the standard Logback syslog appender that forwards events to loggly http://loggly.com 

Building
====================

Building requires a recent version of maven. http://maven.apache.org

```bash
mvn clean install
```
To build a jar that contains all the required dependencies in one jar use the with-dependencies profile.
```bash
mvn clean install -Pwith-dependencies
```

Configuring
====================
The following xml snippet will configure the appender, just add it to your logback.xml. You will need to add your loggly api key. You can provide arbitrary tags here that will be added to each event and aid filtering in the loggly ui. The port and syslog host should be good as provided

```xml
<appender name="LOGGLY" class="com.github.sgargan.logging.LogglySyslogAppender">
    <syslogHost>logs-01.loggly.com</syslogHost>
    <tags>appId,dev</tags>
    <apiToken>your-loggly-api-key</apiToken>
    <appName>appId</appName>
    <port>514</port>
    <facility>LOCAL6</facility>
</appender>

```

Licence
============
Eclipse Public License v1.0
