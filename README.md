mailable-log
============

## Getting Started

Add mailable-log using Gradle:

```groovy
dependencies {
    compile 'com.lukekorth:mailable_log:0.1.2'
}
```

Maven:

```xml
<dependency>
    <groupId>com.lukekorth</groupId>
    <artifactId>mailable_log</artifactId>
    <version>0.1.2</version>
</dependency>
```

## Usage

### Initializing

Initialization should be done as soon as possible, preferably in `Application#onCreate`.

```java
MailableLog.init(context, BuildConfig.DEBUG);
```

### Logging

Use `LoggerFactory` to get a logger instance. Logs can be written at several different levels.

```java
Logger logger = LoggerFactory.getLogger(TAG);
logger.error("message");
```

### Emailing the log

`MailableLog#buildEmailIntent` must not be called on the main thread.

```java
MailableLog.buildEmailIntent(context, "to-address@example.com", "Email Subject", "file-name.log", prependedStringInLog);
```

### Clearing the log

To clear the existing log:

```java
MailableLog.clearLog(context);
```

## License

mailable-log is open source and available under the MIT license. See the [LICENSE](LICENSE) file for more info.
