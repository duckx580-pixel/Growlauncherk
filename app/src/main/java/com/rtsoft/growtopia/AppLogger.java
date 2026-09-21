package com.rtsoft.growtopia;

import android.content.Context;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Locale;

/**
 * Lightweight in-app circular log buffer for debugging on devices without adb/logcat.
 * Also writes to a persistent file so logs survive app restarts.
 */
public final class AppLogger {

    private static final int MAX_ENTRIES = 300;
    private static final ArrayDeque<String> buffer = new ArrayDeque<>(MAX_ENTRIES + 1);
    private static final SimpleDateFormat FMT =
            new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

    private static volatile File logFile = null;

    private AppLogger() {}

    /**
     * Call once from Main.onCreate so file logging is ready before any other call.
     * Safe to call multiple times.
     */
    public static void initFileLog(Context context) {
        if (logFile != null) return;
        try {
            File f = new File(context.getApplicationContext().getFilesDir(), "debug_login.log");
            // Rotate: keep at most 512 KB
            if (f.exists() && f.length() > 512 * 1024) {
                //noinspection ResultOfMethodCallIgnored
                f.delete();
            }
            logFile = f;
            log("AppLogger", "File log initialized: " + f.getAbsolutePath());
        } catch (Exception e) {
            Log.e("AppLogger", "initFileLog failed: " + e.getMessage());
        }
    }

    /** Log at DEBUG level — goes to logcat, in-app buffer, and persistent file. */
    public static void log(String tag, String message) {
        Log.d(tag, message);
        append("[D] " + tag + ": " + message);
    }

    /** Log at WARN level. */
    public static void warn(String tag, String message) {
        Log.w(tag, message);
        append("[W] " + tag + ": " + message);
    }

    /** Log at ERROR level. */
    public static void error(String tag, String message) {
        Log.e(tag, message);
        append("[E] " + tag + ": " + message);
    }

    /**
     * Returns all buffered lines as a single string (newest at bottom),
     * prefixed with the persistent file path so the user knows where to find it.
     */
    public static synchronized String getLogs() {
        StringBuilder sb = new StringBuilder();
        if (logFile != null) {
            sb.append("// File: ").append(logFile.getAbsolutePath()).append("\n\n");
        }
        if (buffer.isEmpty()) {
            sb.append("(no logs captured yet — trigger Google login first)");
        } else {
            for (String line : buffer) sb.append(line).append('\n');
        }
        return sb.toString();
    }

    /** Clears both the in-memory buffer and the persistent log file. */
    public static synchronized void clear() {
        buffer.clear();
        if (logFile != null && logFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            logFile.delete();
        }
    }

    private static synchronized void append(String message) {
        String line = FMT.format(new Date()) + " " + message;
        buffer.addLast(line);
        if (buffer.size() > MAX_ENTRIES) buffer.removeFirst();
        writeToFile(line);
    }

    private static void writeToFile(String line) {
        File f = logFile;
        if (f == null) return;
        try (BufferedWriter w = new BufferedWriter(new FileWriter(f, true))) {
            w.write(line);
            w.newLine();
        } catch (IOException ignored) {}
    }
}
