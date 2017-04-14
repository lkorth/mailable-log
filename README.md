mailable-log
============

## Getting Started

Add mailable-log as a dependency:

```groovy
dependencies {
    compile 'com.lukekorth:mailable_log:0.1.6'
}
```

Add the provider inside the application tag in your `AndroidManifest`:

```xml
<provider
    android:name="android.support.v4.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

For those using Gradle `${applicationId}` can be left alone, however Maven users should replace
`${applicationId}` with the app's package name.

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
