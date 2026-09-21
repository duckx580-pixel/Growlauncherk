package com.ubisoft.bridge;

import com.rtsoft.growtopia.Main;

/**
 * Safe wrapper for libubiservices.so which is absent on arm64 builds.
 * The static initialiser swallows UnsatisfiedLinkError so the launcher
 * continues without crashing; injectActivity is likewise guarded.
 */
public abstract class a {
    static {
        try {
            System.loadLibrary("ubiservices");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Loading library failed: " + e);
        }
    }

    public static void a(Main main) {
        try {
            NativeInterface.injectActivity(main, 0, new String[0]);
        } catch (UnsatisfiedLinkError ignored) {}
    }
}
