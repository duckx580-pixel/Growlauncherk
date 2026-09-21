package com.rtsoft.growtopia;

import android.content.Context;
import java.util.concurrent.atomic.AtomicBoolean;

public class IronSourceManager {
    private Context baseContext;
    private String lastShownPlacementName = "";
    boolean isIronsourceInitialized = false;
    boolean isRewardedVideoPlaying = false;
    boolean isRewardedVideoLoadingStarted = false;
    private String encID = "";
    private String encIP = "";
    private final AtomicBoolean isThreadRunning = new AtomicBoolean(false);
    private final AtomicBoolean isRewarded = new AtomicBoolean(false);

    public IronSourceManager(Context context) {
        this.baseContext = context;
    }

    public static native void onAdClosed(String str);
    public static native void pauseAnzu();
    public static native void resumeAnzu();
    public static native void sendPingToServer();

    public boolean IsAdActive() { return this.isThreadRunning.get(); }
    public boolean IsShowingAd() { return false; }
    public boolean ShowRewardedAd(String str) { return false; }
    public void Init() {}
    public void LoadRewardedAd() {}
    public void OnCreate() {}
    public void UpdatePing() {}
    public void onPause() {}
    public void onResume() {}
    public void SetDynamicUserID(String str) {}
    public void SetUserAgeType(int type) {}
    public void SetUserConsent(boolean consent) {}
    public void SendAdFailedEvent(String str, String str2) {}
    public void SetCustomFields(String str, String str2) {}
    public void onInitializationComplete() {}
}
