package com.gentz.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Writes crash reports to getExternalFilesDir("crashes") so they can be retrieved without adb.
 *
 * Java crashes are caught by the uncaught exception handler. Native crashes are caught by the
 * signal handler in libgtcrash.so, which writes signal details and a symbolized backtrace to
 * {@link #nativeReport()} from inside the dying process; that file is folded into the report on
 * the next launcher start. A breadcrumb written before the game activity starts also detects
 * process deaths the signal handler cannot see, and the logcat ring buffer is dumped when the
 * device allows it (many OEM builds deny READ_LOGS to apps).
 */
public final class CrashLogger {
    private static final String TAG = "CrashLogger";
    private static final String PREFS = "crash_logger";
    private static final String KEY_PENDING = "pending_launch";
    private static final int MAX_REPORTS = 10;

    private static Context appContext;
    private static boolean libraryLoaded;
    private static boolean nativeHandlerInstalled;

    private CrashLogger() {
    }

    public static void install(Context context) {
        appContext = context.getApplicationContext();
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                StringWriter stack = new StringWriter();
                throwable.printStackTrace(new PrintWriter(stack));
                write("java-crash", "Thread: " + thread.getName() + "\n\n" + stack);
            } catch (Throwable t) {
                Log.e(TAG, "failed to write crash report", t);
            }
            if (previous != null) {
                previous.uncaughtException(thread, throwable);
            }
        });
        installNativeHandler();
    }

    /**
     * Installs the signal handler. Call again after loading libraries that install their own
     * handlers (the engine bundles Crashlytics) so ours stays in the chain.
     */
    public static void installNativeHandler() {
        if (appContext == null) {
            return;
        }
        try {
            if (!libraryLoaded) {
                System.loadLibrary("gtcrash");
                libraryLoaded = true;
            }
            nativeHandlerInstalled = installNativeHandler(nativeReport().getAbsolutePath());
        } catch (Throwable t) {
            Log.w(TAG, "native crash handler unavailable", t);
        }
    }

    public static boolean isNativeHandlerInstalled() {
        return nativeHandlerInstalled;
    }

    private static native boolean installNativeHandler(String reportPath);

    private static native void nativeSelfTest();

    /** Crashes the process on purpose so the report pipeline can be verified on a device. */
    public static void selfTestCrash() {
        markLaunchStarted();
        installNativeHandler();
        nativeSelfTest();
    }

    private static File nativeReport() {
        return new File(reportDir(), "native-signal.txt");
    }

    private static String consumeNativeReport() {
        File report = nativeReport();
        if (!report.exists()) {
            return "No native signal report: the process was killed without a fatal signal "
                    + "(out of memory, ANR kill or an abort before the handler was installed).\n";
        }
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new java.io.FileReader(report))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line).append('\n');
            }
        } catch (Exception e) {
            out.append("cannot read native report: ").append(e).append('\n');
        }
        if (!report.delete()) {
            Log.w(TAG, "cannot delete " + report);
        }
        return out.toString();
    }

    /** Marks that the game activity is about to start, so a native crash can be detected later. */
    public static void markLaunchStarted() {
        // commit(), not apply(): the process can die before an async write reaches disk.
        prefs().edit().putString(KEY_PENDING, timestamp()).commit();
    }

    public static void markLaunchFinished() {
        prefs().edit().remove(KEY_PENDING).apply();
    }

    /**
     * @return the report for a native crash during the previous run, or null if it exited cleanly.
     */
    public static File consumePendingNativeCrash() {
        String started = prefs().getString(KEY_PENDING, null);
        boolean signalled = nativeReport().exists();
        if (started == null && !signalled) {
            return null;
        }
        markLaunchFinished();
        return write("native-crash", "Game launched at "
                + (started == null ? "an unrecorded time" : started)
                + " and the process died without returning.\n\n"
                + consumeNativeReport()
                + "\nlogcat:\n" + readLogcat());
    }

    public static File latestReport() {
        File[] reports = reports();
        if (reports.length == 0) {
            return null;
        }
        File newest = reports[0];
        for (File report : reports) {
            if (report.lastModified() > newest.lastModified()) {
                newest = report;
            }
        }
        return newest;
    }

    private static File write(String kind, String body) {
        File report = new File(reportDir(), kind + "-" + timestamp() + ".txt");
        try (FileWriter writer = new FileWriter(report)) {
            writer.write(deviceInfo());
            writer.write("\n");
            writer.write(body);
        } catch (Exception e) {
            Log.e(TAG, "cannot write " + report, e);
            return null;
        }
        prune();
        return report;
    }

    private static String deviceInfo() {
        return "Growlauncher " + com.gentz.launcher.LauncherConfig.LAUNCHER_VERSION
                + " (Growtopia " + com.gentz.launcher.LauncherConfig.GROWTOPIA_VERSION + ")\n"
                + "Time: " + timestamp() + "\n"
                + "Device: " + Build.MANUFACTURER + " " + Build.MODEL + "\n"
                + "Android: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")\n"
                + "ABIs: " + android.text.TextUtils.join(", ", Build.SUPPORTED_ABIS) + "\n"
                + "Engine loaded: " + com.rtsoft.growtopia.NativeLibraries.isGameLoaded()
                + "\n"
                + "Native handler: " + nativeHandlerInstalled + "\n";
    }

    private static String readLogcat() {
        StringBuilder out = new StringBuilder();
        try {
            Process process = Runtime.getRuntime().exec(
                    new String[]{"logcat", "-d", "-v", "threadtime", "-t", "600"});
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    out.append(line).append('\n');
                }
            }
        } catch (Exception e) {
            out.append("logcat unavailable: ").append(e);
        }
        return out.toString();
    }

    private static void prune() {
        File[] reports = reports();
        if (reports.length <= MAX_REPORTS) {
            return;
        }
        java.util.Arrays.sort(reports, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
        for (int i = 0; i < reports.length - MAX_REPORTS; i++) {
            if (!reports[i].delete()) {
                Log.w(TAG, "cannot delete " + reports[i]);
            }
        }
    }

    /** The finished reports, excluding the scratch file the signal handler writes into. */
    private static File[] reports() {
        File[] files = reportDir().listFiles(
                (dir, name) -> !name.equals(nativeReport().getName()));
        return files == null ? new File[0] : files;
    }

    private static File reportDir() {
        File dir = new File(appContext.getExternalFilesDir(null), "crashes");
        if (!dir.exists() && !dir.mkdirs()) {
            Log.w(TAG, "cannot create " + dir);
        }
        return dir;
    }

    private static SharedPreferences prefs() {
        return appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String timestamp() {
        return new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(new Date());
    }
}
