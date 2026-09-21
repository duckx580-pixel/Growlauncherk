package com.rtsoft.growtopia;

import android.util.Log;

/**
 * ZennKuy Injector - Helper class for initializing ZennKuy native hooks
 * 
 * This class provides convenient methods for:
 * - Forcing OnlineGameController mode
 * - Bypassing login/consent dialog
 * - General initialization
 */
public class ZennKuyInjector {
    private static final String TAG = "ZennKuyInjector";
    
    /**
     * Force online game controller mode
     * This ensures the game initializes in online mode without needing manual login flow
     */
    public static void forceOnlineGameController() {
        try {
            Log.d(TAG, "Forcing OnlineGameController via ZennKuy...");
            Main.ZennKuyRenderer.nativeForcedOnlineMode(true);
            Log.d(TAG, "OnlineGameController forced successfully");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to force OnlineGameController: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Exception in forceOnlineGameController: " + e.getMessage());
        }
    }
    
    /**
     * Bypass login dialog with authentication token
     * @param authToken - Authentication token (or null for default bypass token)
     */
    public static void bypassLoginDialog(String authToken) {
        try {
            Log.d(TAG, "Bypassing login dialog via ZennKuy...");
            String token = authToken != null ? authToken : "zennkuy_bypass";
            Main.ZennKuyRenderer.nativeBypassLogin(token);
            Log.d(TAG, "Login bypassed successfully");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to bypass login: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Exception in bypassLoginDialog: " + e.getMessage());
        }
    }
    
    /**
     * Initialize ZennKuy with all hooks
     * - Forces online mode
     * - Bypasses login dialog
     * 
     * Call this from Main.onCreate() to activate all ZennKuy features
     */
    public static void initializeZennKuy() {
        try {
            Log.d(TAG, "Initializing ZennKuy hooks...");
            forceOnlineGameController();
            bypassLoginDialog(null);
            Log.d(TAG, "ZennKuy initialization complete");
        } catch (Exception e) {
            Log.e(TAG, "ZennKuy initialization failed", e);
        }
    }
}
