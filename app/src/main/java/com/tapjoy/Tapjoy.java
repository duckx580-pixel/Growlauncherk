package com.tapjoy;

import android.app.Activity;
import android.content.Context;
import java.util.Hashtable;

public final class Tapjoy {
    public static void onActivityStart(Activity activity) {}
    public static void onActivityStop(Activity activity) {}

    public static boolean connect(Context context, String appKey) { return false; }
    public static boolean connect(Context context, String appKey, Hashtable<String, ?> flags) { return false; }
    public static boolean connect(Context context, String appKey, Hashtable<String, ?> flags, TJConnectListener listener) { return false; }

    public static TJPlacement getPlacement(String name, TJPlacement.TJPlacementListener listener) { return null; }
    public static void setActivity(Activity activity) {}
    public static void getCurrencyBalance(TJGetCurrencyBalanceListener listener) {}
    public static void setEarnedCurrencyListener(Object listener) {}
    public static void setDebugEnabled(boolean enabled) {}
    public static String getVersion() { return ""; }
}
