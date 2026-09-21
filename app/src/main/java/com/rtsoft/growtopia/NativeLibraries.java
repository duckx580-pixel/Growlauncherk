package com.rtsoft.growtopia;

import android.content.Context;
import android.util.Log;

import java.io.File;

/**
 * Central loader for the prebuilt Growtopia/Growlauncher native libraries.
 *
 * The engine libraries are not part of this repository, so every entry point that
 * touches JNI has to be able to tell whether they were loaded before calling into
 * native code. Loading through here keeps that state in one place and turns a
 * process-killing UnsatisfiedLinkError into a recoverable condition.
 */
public final class NativeLibraries {
    private static final String TAG = "NativeLibraries";
    public static final String GAME_LIBRARY = "growtopia";

    private static boolean attempted;
    private static boolean gameLoaded;

    private NativeLibraries() {
    }

    public static synchronized boolean loadGame() {
        if (attempted) {
            return gameLoaded;
        }
        attempted = true;
        try {
            System.loadLibrary(GAME_LIBRARY);
            gameLoaded = true;
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "lib" + GAME_LIBRARY + ".so is missing from jniLibs: " + e.getMessage());
            gameLoaded = false;
        }
        com.gentz.launcher.CrashLogger.installNativeHandler();
        return gameLoaded;
    }

    public static synchronized boolean isGameLoaded() {
        return gameLoaded;
    }

    /** Checks for the engine library without loading it, so the launcher UI can warn early. */
    public static boolean isGameLibraryPresent(Context context) {
        if (isGameLoaded()) {
            return true;
        }
        String dir = context.getApplicationInfo().nativeLibraryDir;
        return dir != null && new File(dir, "lib" + GAME_LIBRARY + ".so").exists();
    }
}
