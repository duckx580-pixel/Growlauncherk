package com.ubisoft.bridge;

import android.app.Activity;

public class JavaInterface {
    public static int injectActivityJava(Activity activity) {
        try {
            return NativeInterface.injectActivity(activity, 0, new String[0]);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Loading library failed: " + e);
            return 0;
        }
    }

    static {
        try {
            System.loadLibrary("ubiservices");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Loading library failed: " + e);
        }
    }
}
