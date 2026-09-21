package com.rtsoft.growtopia;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Locale;

/**
 * Lightweight in-app circular log buffer for debugging on devices without adb/logcat.
 *
 * <p>All write paths (WebViewManager, ZennKuyBridge, GoogleSignInHelper) call
 * {@link #log(String, String)} which simultaneously forwards to Android's logcat
 * AND appends to the in-memory ring so the user can view the full Google login
 * trace from inside the ZK menu (VIEW LOGS button) without needing a computer.
 *
 * <p>Thread-safe via {@code synchronized}. Max 300 entries — oldest evicted first.
 */
public final class AppLogger {

    private static final int MAX_ENTRIES = 300;
    private static final ArrayDeque<String> buffer = new ArrayDeque<>(MAX_ENTRIES + 1);
    private static final SimpleDateFormat FMT =
            new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

    private AppLogger() {}

    /** Log at DEBUG level — goes to logcat AND the in-app buffer. */
    public static void log(String tag, String message) {
        Log.d(tag, message);
        append("[D] " + tag + ": " + message);
    }

    /** Log at WARN level — goes to logcat AND the in-app buffer. */
    public static void warn(String tag, String message) {
        Log.w(tag, message);
        append("[W] " + tag + ": " + message);
    }

    /** Log at ERROR level — goes to logcat AND the in-app buffer. */
    public static void error(String tag, String message) {
        Log.e(tag, message);
        append("[E] " + tag + ": " + message);
    }

    /** Returns all buffered lines as a single string, newest at the bottom. */
    public static synchronized String getLogs() {
        if (buffer.isEmpty()) return "(no logs captured yet — trigger Google login first)";
        StringBuilder sb = new StringBuilder();
        for (String line : buffer) sb.append(line).append('\n');
        return sb.toString();
    }

    /** Clears the buffer. */
    public static synchronized void clear() {
        buffer.clear();
    }

    private static synchronized void append(String message) {
        String line = FMT.format(new Date()) + " " + message;
        buffer.addLast(line);
        if (buffer.size() > MAX_ENTRIES) buffer.removeFirst();
    }
}
