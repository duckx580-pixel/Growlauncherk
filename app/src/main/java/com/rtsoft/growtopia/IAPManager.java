package com.rtsoft.growtopia;

import android.app.Activity;

public class IAPManager {
    private Activity mainActivity;
    private boolean isReady = false;

    public IAPManager(Activity activity) {
        this.mainActivity = activity;
    }

    public void IAPPurchase(String str) {}
    public void RequestAIPPurchasedList() {
        try { SharedActivity.nativeSendGUIEx(45, -1, 0, 0); } catch (Throwable t) {}
    }
    public void RequestItemDetails(String str) {}
    public void ConsumeItem(String str) {}
}
