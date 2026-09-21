package com.rtsoft.growtopia;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.Map;

public class AppsFlyerManager {
    private Context baseContext;
    private volatile boolean isStoped = false;
    private volatile boolean isStarted = false;

    native void nativeOnStarted(int i);

    public AppsFlyerManager(Context context) {
        this.baseContext = context;
    }

    public String GetAppsFlyerId() { return ""; }
    public void Init(String key) {
        // The vendor SDK is optional in this launcher. Native only needs Start() to
        // finish its asynchronous initialization contract.
    }

    public void Start(boolean consent, boolean shouldStart) {
        isStoped = !consent;
        if (isStarted) return;
        isStarted = true;
        Log.i("GTAppsFlyer", "optional SDK skipped; completing native startup");
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                nativeOnStarted(0);
            } catch (Throwable error) {
                Log.e("GTAppsFlyer", "native startup callback failed", error);
            }
        });
    }
    public void LogEvent(String event, String value) {}
    public void LogEvent(String event, Map<String, Object> map) {}
    public void LogPurchase(String a, String b, String c) {}
}
