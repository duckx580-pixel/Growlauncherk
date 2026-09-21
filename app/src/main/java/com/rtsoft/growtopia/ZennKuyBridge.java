package com.rtsoft.growtopia;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

public final class ZennKuyBridge {
    private static final String TAG = "ZennKuyBridge";
    private ZennKuyBridge() {}

    public static volatile boolean sTokenDelivered = false;

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
     * Replays the engine's last LoadURLPost call against Ubisoft's dashboard so that
     * Ubisoft mints a session and returns a Google OAuth URL containing a valid state=
     * parameter.  The WebViewManager intercepts the accounts.google.com redirect and
     * opens it in Chrome; Chrome redirects to grow://?token=<LTOKEN> which
     * Main.onNewIntent picks up and injects into the engine.
     *
     * The raw bytes cached in WebViewManager.sLastPostData are passed directly to
     * webView.postUrl — they are never re-encoded so the engine's binary hash fields
     * are preserved exactly.
     *
     * If the engine hasn't posted to the dashboard yet (sLastPostData == null), the
     * user is told to tap Login / Play Online in the game first.
     */
    public static void startResolving() {
        sTokenDelivered = false;
        Activity act = Main.mainApp;
        if (act == null) {
            Log.e(TAG, "startResolving: mainApp is null");
            return;
        }

        byte[] postData = WebViewManager.sLastPostData;
        String dashboardUrl = WebViewManager.sLastDashboardUrl;

        if (postData == null || postData.length == 0) {
            act.runOnUiThread(() -> {
                String msg = "No engine payload captured yet.\n"
                        + "Please tap LOGIN / PLAY ONLINE in the game first,\n"
                        + "then tap START RESOLVING again.";
                Toast.makeText(act, msg, Toast.LENGTH_LONG).show();
                try {
                    new LoginSpoof(act).setGoogleLogs(
                            "sLastPostData is null — tap Login in-game first.");
                } catch (Exception ignored) {}
            });
            Log.w(TAG, "startResolving: sLastPostData not yet captured; aborting");
            return;
        }

        if (dashboardUrl == null || dashboardUrl.isEmpty()) {
            dashboardUrl = "https://login.growtopiagame.com/player/login/dashboard";
        }

        final String finalUrl = dashboardUrl;
        final byte[] finalData = postData;
        act.runOnUiThread(() -> {
            try {
                new LoginSpoof(act).setGoogleLogs(
                        "Replaying engine POST to dashboard ("
                        + finalData.length + " bytes) — waiting for Google OAuth redirect …");
                Log.d(TAG, "startResolving: POST to " + finalUrl
                        + " (" + finalData.length + " bytes)");
                Main.mainApp.webViewManager.postOAuthDashboard(finalUrl, finalData);
            } catch (Exception e) {
                Log.e(TAG, "startResolving: " + e.getMessage());
            }
        });
    }
}
