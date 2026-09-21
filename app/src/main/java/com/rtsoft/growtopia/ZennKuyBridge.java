package com.rtsoft.growtopia;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
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
     * Opens the engine's login URL directly in Chrome so Ubisoft's OAuth page loads
     * in a real browser — no in-app WebView, no window.open() popup issues.
     *
     * Flow:
     *  1. Engine calls LoadURLPost(url, data) → url cached in WebViewManager.sLastLoginUrl
     *  2. User taps LOGIN TOKEN → this method fires
     *  3. Chrome opens the URL; user picks Google account
     *  4. Google redirects to grow://?token=<LTOKEN>
     *  5. Main.onNewIntent extracts token → nativeOnScriptCall("nativeSignIn", token)
     */
    public static void startResolving() {
        sTokenDelivered = false;
        Activity act = Main.mainApp;
        if (act == null) {
            Log.e(TAG, "startResolving: mainApp is null");
            return;
        }

        String loginUrl = WebViewManager.sLastLoginUrl;
        if (loginUrl == null || loginUrl.isEmpty()) {
            act.runOnUiThread(() -> {
                Toast.makeText(act,
                        "No login URL captured yet.\n"
                        + "Please tap PLAY ONLINE in the game first,\n"
                        + "then tap LOGIN TOKEN again.",
                        Toast.LENGTH_LONG).show();
                try { new LoginSpoof(act).setGoogleLogs("sLastLoginUrl is null — tap Play Online first."); }
                catch (Exception ignored) {}
            });
            Log.w(TAG, "startResolving: sLastLoginUrl not captured yet; aborting");
            return;
        }

        final String finalUrl = loginUrl;
        act.runOnUiThread(() -> {
            try {
                new LoginSpoof(act).setGoogleLogs("Opening in Chrome: " + finalUrl);
                Log.d(TAG, "startResolving: opening in Chrome → " + finalUrl);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                act.startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "startResolving: " + e.getMessage());
                Toast.makeText(act, "Could not open browser: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
