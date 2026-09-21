package com.ubisoft.bridge;

import android.app.Activity;

abstract class NativeInterface {
    public static final native int injectActivity(Activity activity, int i10, String[] strArr);
}
