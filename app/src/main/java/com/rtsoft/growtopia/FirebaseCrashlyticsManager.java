package com.rtsoft.growtopia;

import android.content.Context;
import android.util.Log;

public class FirebaseCrashlyticsManager {
    public FirebaseCrashlyticsManager(Context context) {
    }

    public void SetUserConsent(boolean consent) {}

    public void RecordException(String str, int i) {
        Log.e("FirebaseCrashlytics", str + i);
    }
}
