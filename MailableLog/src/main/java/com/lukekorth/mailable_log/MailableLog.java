package com.lukekorth.mailable_log;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Looper;
import android.support.v4.content.FileProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;

/**
 * MailableLog handles the initialization of logging and also provides a method to build
 * an {@link android.content.Intent} that points to the log so it may be sent via email or other means.
 *
 * The {@link #init(android.content.Context, boolean)} method should be called from the
 * {@link android.app.Application#onCreate()} method to setup the log.
 */
public class MailableLog {

    /**
     * The default pattern for writing to the log. To write your own pattern
     * see <a href="http://logback.qos.ch/manual/layouts.html#ClassicPatternLayout">Logback's documentation</a>.
     */
    public static final String DEFAULT_PATTERN = "%date{MMM dd | HH:mm:ss.SSS} %highlight(%-5level) %-25([%logger{36}]) %msg%n";

    /**
     * Called to initialize logging. Should be called in {@link android.app.Application#onCreate()}
     *
     * @param context
     * @param writeToLogcat If {@code true} log messages will be written to logcat as well as
     *                      to disk. If {@code false} log messages will only be written to disk.
     *                      Apps should only ever write to logcat in development.
     */
    public static void init(Context context, boolean writeToLogcat) {
        init(context, writeToLogcat, DEFAULT_PATTERN);
    }

    /**
     * Called to initialize logging. Should be called in {@link android.app.Application#onCreate()}
     *
     * @param context
     * @param writeToLogcat If {@code true} log messages will be written to logcat as well as
     *                      to disk. If {@code false} log messages will only be written to disk.
     *                      Apps should only ever write to logcat in development.
     * @param pattern The log pattern to use. The default is {@link #DEFAULT_PATTERN}
     */
    public static void init(Context context, boolean writeToLogcat, String pattern) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.reset();

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern(pattern);
        encoder.start();

        FileAppender<ILoggingEvent> fileAppender = new FileAppender<ILoggingEvent>();
        fileAppender.setContext(loggerContext);
        fileAppender.setFile(getFilePath(context));
        fileAppender.setAppend(true);
        fileAppender.setEncoder(encoder);
        fileAppender.start();

        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger)
                LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.addAppender(fileAppender);

        if (writeToLogcat) {
            LogcatAppender logcatAppender = new LogcatAppender();
            logcatAppender.setContext(loggerContext);
            logcatAppender.setEncoder(encoder);
            logcatAppender.start();

            root.addAppender(logcatAppender);
        }
    }

    /**
     * Get the path to the log file
     *
     * @param context
     * @return A {@code String} representation of the path of the log file
     */
    public static String getFilePath(Context context) {
        return context.getFileStreamPath("mailable.log").getAbsolutePath();
    }

    /**
     * Called to build an Intent that can be used to send an email.
     * Upon receiving the {@link android.content.Intent} from
     * {@link #buildEmailIntent(android.content.Context, String, String, String, String)}
     * you can call {@code context.startActivity(intent)} to display a chooser to the user.
     *
     * <b>Must be called on the background thread</b>, a {@link java.lang.IllegalThreadStateException}
     * will be thrown if it is called on the main thread.
     *
     * @param context
     * @param emailAddress The email address the log should be sent to
     * @param emailSubject The email subject
     * @param fileName The filename for the log to be attached to the email
     * @param metaData A {@code String} that will be prepended to the log. New lines should be
     *                 seperated with a "\n"
     * @return A chooser {@link android.content.Intent} that can be used
     *         to call {@link android.content.Context#startActivity(android.content.Intent)}
     * @throws IOException When MailableLog fails to read or write the log file
     */
    public static Intent buildEmailIntent(Context context, String emailAddress, String emailSubject,
                                        String fileName, String metaData) throws IOException {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalThreadStateException("MailableLog#buildEmailIntent cannot be called" +
                    "from the main thread");
        }

        File file = getFile(context, fileName);
        file.createNewFile();
        GZIPOutputStream gos = new GZIPOutputStream(new BufferedOutputStream(new PrintStream(file)));

        if (metaData != null) {
            gos.write(metaData.getBytes());
        }

        gos.write(getLog(context).getBytes());
        gos.close();

        Uri fileUri = FileProvider.getUriForFile(context, "com.lukekorth.mailable_log.fileprovider", file);

        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("text/plain");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{emailAddress});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, emailSubject);
        emailIntent.putExtra(Intent.EXTRA_STREAM, fileUri);

        // grant read permissions for all apps that can handle the intent
        List<ResolveInfo> infoList = context.getPackageManager()
                .queryIntentActivities(emailIntent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : infoList) {
            context.grantUriPermission(resolveInfo.activityInfo.packageName, fileUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        return Intent.createChooser(emailIntent, context.getString(R.string.send_email_via));
    }

    private static File getFile(Context context, String fileName) {
        File mailableLogsDir  = new File(context.getFilesDir(), "mailable_logs");
        mailableLogsDir.mkdir();
        return new File(mailableLogsDir, fileName + ".gz");
    }

    private static String getLog(Context context) throws IOException {
        StringBuilder response = new StringBuilder();
        InputStream in = new FileInputStream(getFilePath(context));
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        String line = reader.readLine();
        String currentTag;
        String lastTag = null;
        while(line != null) {
            try {
                currentTag = line.substring(line.indexOf("["), line.indexOf("]") + 1);
                if (!currentTag.equals(lastTag)) {
                    lastTag = currentTag;
                    response.append("\n");
                }
            } catch (StringIndexOutOfBoundsException e) {
            }

            response.append(line + "\n");
            line = reader.readLine();
        }

        try {
            in.close();
        } catch (IOException e) {}

        return response.toString();
    }

}
