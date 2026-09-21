package com.rtsoft.growtopia;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

public final class ZennKuyBridge {
    private static final String TAG = "ZennKuyBridge";
    private ZennKuyBridge() {}

    public static volatile boolean sTokenDelivered = false;

    private static DeviceSpoofer deviceSpoofer() {
        if (Main.mainApp == null) return null;
        return new DeviceSpoofer(Main.mainApp);
    }

    public static String generateMac() {
        try {
            DeviceSpoofer ds = deviceSpoofer();
            if (ds != null) {
                String mac = ds.getMac();
                if (mac != null && !mac.isEmpty()) return mac;
            }
            return DeviceSpoofer.generateMac();
        } catch (Exception e) {
            return "02:00:00:00:00:00";
        }
    }

    public static String generateRid() {
        try {
            DeviceSpoofer ds = deviceSpoofer();
            if (ds != null) {
                String rid = ds.getRid();
                if (rid != null && !rid.isEmpty()) return rid;
            }
            return DeviceSpoofer.generateRid();
        } catch (Exception e) {
            return "";
        }
    }

    public static String generateWk() {
        try {
            DeviceSpoofer ds = deviceSpoofer();
            if (ds != null) {
                String gid = ds.getGid();
                if (gid != null && !gid.isEmpty()) return gid;
            }
            return DeviceSpoofer.generateGid();
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
        final byte[] postData = WebViewManager.sLastPostData;
        act.runOnUiThread(() -> {
            try {
                new LoginSpoof(act).setGoogleLogs("Posting login via WebView: " + finalUrl);
                Log.d(TAG, "startResolving: posting to WebView → " + finalUrl);
                WebViewManager wvm = Main.GetWebViewManager();
                if (wvm != null) {
                    wvm.postOAuthDashboard(finalUrl, postData);
                } else {
                    Log.e(TAG, "startResolving: WebViewManager is null");
                    Toast.makeText(act, "Could not open login WebView.", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "startResolving: " + e.getMessage());
                Toast.makeText(act, "Could not start login: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
