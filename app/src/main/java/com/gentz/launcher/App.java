package com.gentz.launcher;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.provider.Settings;
import android.os.Build;

import java.util.UUID;

public class App extends Application {
    public static App f10088p;
    private static SharedPreferences prefs;
    private static Context growtopiaContext;

    @Override
    public void onCreate() {
        super.onCreate();
        f10088p = this;
        prefs = getSharedPreferences("launcher_data", Context.MODE_PRIVATE);
        CrashLogger.install(this);
    }

    public static AssetManager a() {
        try {
            if (growtopiaContext == null) {
                growtopiaContext = f10088p.createPackageContext(
                        "com.rtsoft.growtopia", Context.CONTEXT_INCLUDE_CODE);
            }
            return growtopiaContext.getAssets();
        } catch (Exception e) {
            return f10088p.getAssets();
        }
    }

    public static String getData(String key) {
        if (f10088p == null) return "";
        if ("device_os".equals(key)) return Build.VERSION.RELEASE;
        if ("model".equals(key)) return Build.MODEL;

        if (prefs == null) {
            prefs = f10088p.getSharedPreferences("launcher_data", Context.MODE_PRIVATE);
        }
        String value = prefs.getString(key, null);
        if (value == null) {
            if ("mac".equals(key)) {
                value = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            } else if ("gid".equals(key)) {
                try {
                    value = Settings.Secure.getString(f10088p.getContentResolver(), Settings.Secure.ANDROID_ID);
                } catch (Exception e) {
                    value = UUID.randomUUID().toString();
                }
            } else {
                value = "";
            }
            prefs.edit().putString(key, value).apply();
        }
        return value;
    }
}
