package com.rtsoft.growtopia;

import android.app.Activity;
import android.util.Log;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class ZennKuyBridge {
    private static final String TAG = "ZennKuyBridge";
    private ZennKuyBridge() {}

    public static volatile boolean sTokenDelivered = false;

    // Ubisoft's login dashboard mints the session and produces a Google OAuth URL
    // that includes the required state= parameter. We never open Google OAuth directly.
    private static final String DASHBOARD_URL =
            "https://login.growtopiagame.com/player/login/dashboard";

    private static LoginSpoof spoof() {
        if (Main.mainApp == null) return null;
        return new LoginSpoof(Main.mainApp);
    }

    public static String generateMac() {
        try {
            LoginSpoof s = spoof();
            return s != null ? s.generateMac() : "02:00:00:00:00:00";
        } catch (Exception e) {
            return "02:00:00:00:00:00";
        }
    }

    public static String generateRid() {
        try {
            LoginSpoof s = spoof();
            return s != null ? s.generateRid() : "";
        } catch (Exception e) {
            return "";
        }
    }

    public static String generateWk() {
        try {
            LoginSpoof s = spoof();
            return s != null ? s.generateWk() : "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Posts to Growtopia's login dashboard so Ubisoft mints a session and returns
     * a Google OAuth URL that includes a valid state= parameter. The WebViewManager
     * intercepts the resulting accounts.google.com redirect and opens it in Chrome.
     * Chrome later redirects to grow://?token=<LTOKEN> which Main.onNewIntent handles.
     */
    public static void startResolving() {
        sTokenDelivered = false;
        Activity act = Main.mainApp;
        if (act == null) {
            Log.e(TAG, "startResolving: mainApp is null");
            return;
        }
        act.runOnUiThread(() -> {
            try {
                LoginSpoof s = new LoginSpoof(act);
                String mac = s.getMac(); if (mac.isEmpty()) mac = s.generateMac();
                String rid = s.getRid(); if (rid.isEmpty()) rid = s.generateRid();
                String wk  = s.getWk();  if (wk.isEmpty())  wk  = s.generateWk();
                Locale locale = Locale.getDefault();
                String country = locale.getCountry().isEmpty() ? "US" : locale.getCountry();
                String postBody = "platformID=4&deviceVersion=0"
                        + "&mac=" + URLEncoder.encode(mac, "UTF-8")
                        + "&rid=" + URLEncoder.encode(rid, "UTF-8")
                        + "&wk="  + URLEncoder.encode(wk,  "UTF-8")
                        + "&lmode=1"
                        + "&country=" + URLEncoder.encode(locale.getLanguage() + "-" + country, "UTF-8")
                        + "&hash=0";
                byte[] postData = postBody.getBytes(StandardCharsets.UTF_8);
                s.setGoogleLogs("Posting to dashboard to obtain state= …");
                Log.d(TAG, "startResolving: posting to " + DASHBOARD_URL);
                Main.mainApp.webViewManager.postOAuthDashboard(DASHBOARD_URL, postData);
            } catch (Exception e) {
                Log.e(TAG, "startResolving: " + e.getMessage());
            }
        });
    }
}
