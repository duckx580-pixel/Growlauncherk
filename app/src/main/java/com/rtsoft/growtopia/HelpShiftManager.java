package com.rtsoft.growtopia;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import java.util.HashMap;

public class HelpShiftManager {
    private Context baseContext;

    public HelpShiftManager(Context context) {
        this.baseContext = context;
    }

    public boolean HandleDeeplink(Intent intent) {
        Uri data = intent.getData();
        if (data == null) return false;
        String host = data.getHost();
        if (host == null || !host.contains("helpshift")) return false;
        return true;
    }

    public void Init() {}
    public void SetLanguage(String lang) {}
    public void ShowConversation(HashMap<String, Object> config) {}
    public void ShowFAQs(HashMap<String, Object> config) {}

    public String getDeviceInfo() {
        return "android version:" + Build.VERSION.RELEASE + "(" + Build.VERSION.INCREMENTAL +
               ");\nandroid API Level:" + Build.VERSION.SDK_INT +
               ";\ndevice:" + Build.DEVICE + ";\nmodel:" + Build.MODEL;
    }

    public static void SetConfigValue(HashMap<String, Object> map, String key, String type, Object value) {}
}
