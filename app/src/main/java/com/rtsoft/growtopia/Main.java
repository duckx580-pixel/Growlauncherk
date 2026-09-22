package com.rtsoft.growtopia;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.ubisoft.bridge.JavaInterface;

import java.io.File;

public class Main extends SharedActivity {
    static {
        System.loadLibrary("growtopia");
        System.loadLibrary("zennkuy");
    }

    public static boolean OriginalKeyboard = false;
    public static boolean block_pause;
    public static HelpShiftManager helpshiftManager;
    public static Main mainApp;
    public static GLSurfaceView mygl;
    private HeightProvider heightProvider;

    public NativeAppInterface nativeAppInterface = new NativeAppInterface();
    public AppsFlyerManager appsflyerManager = new AppsFlyerManager(this);
    public IronSourceManager ironSourceManager = new IronSourceManager(this);
    public WebViewManager webViewManager = new WebViewManager(this);
    public AppReviewManager appReviewManager = new AppReviewManager(this);
    public FirebaseCrashlyticsManager firebaseCrashlyticsManager;
    public FirebaseCloudMessageManager firebaseCloudMessageManager = new FirebaseCloudMessageManager();
    public GoogleSignInHelper googleSignInHelper;
    public ZennKuyOverlay zennKuyOverlay;
    public MAFManager mafManager = new MAFManager(this);
    public UsercentricsManager usercentricsManager = null;

    public static AppReviewManager GetAppReviewManager() { return mainApp.appReviewManager; }
    public static AppsFlyerManager GetAppsflyerManager() { return mainApp.appsflyerManager; }
    public static FirebaseCloudMessageManager GetFirebaseCloudMessageManager() { return mainApp.firebaseCloudMessageManager; }
    public static FirebaseCrashlyticsManager GetFirebaseCrashlyticsManager() { return mainApp.firebaseCrashlyticsManager; }
    public static GoogleSignInHelper GetGoogleSignInHelper() { return mainApp.googleSignInHelper; }
    public static Object GetHelpShiftManager() { return helpshiftManager; }
    public static Object GetIronSourceManager() { return mainApp.ironSourceManager; }
    public static MAFManager GetMAFManager() { return mainApp.mafManager; }
    public static UsercentricsManager GetUsercentricsManager() {
        return mainApp == null ? null : mainApp.usercentricsManager;
    }
    public static WebViewManager GetWebViewManager() { return mainApp.webViewManager; }

    private void logSavePath() {
        try {
            File dir = getExternalFilesDir(null);
            File save = dir == null ? null : new File(dir, "save.dat");
            File cache = dir == null ? null : new File(dir, "cache");
            Log.i("ZennKuyPath", "pkg=" + getPackageName()
                    + " assetPkg=" + SharedActivity.PackageName
                    + " files=" + (dir == null ? "null" : dir.getAbsolutePath())
                    + " save.exists=" + (save != null && save.isFile())
                    + " save.size=" + (save != null && save.isFile() ? save.length() : 0)
                    + " cache.dir=" + (cache != null && cache.isDirectory()));
        } catch (Throwable t) {
            Log.e("ZennKuyPath", "logSavePath", t);
        }
    }

    public static native void nativeOnKey(int state, int key, int unicodeChar);

    @Override
    public String GetAppsflyerUID() { return ""; }

    public int getBottomCutoutHeight() {
        android.view.WindowInsets rootWindowInsets = getWindow().getDecorView().getRootWindowInsets();
        if (rootWindowInsets == null || Build.VERSION.SDK_INT < 30) return 0;
        return rootWindowInsets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout()).bottom;
    }

    public void OnKeyboardHeightChanged(int height) {
        if (this.webViewManager.IsVisible()) {
            this.webViewManager.MoveView(height);
            return;
        }
        SharedActivity.m_KeyBoardHeight = height;
        boolean keyboardOpen = height > getBottomCutoutHeight();
        if (keyboardOpen && !SharedActivity.m_editText.isFocused()) {
            UpdateEditBoxInView(true, false);
        } else if (!keyboardOpen && SharedActivity.m_editText.isFocused()) {
            SharedActivity.nativeOnInputText(SharedActivity.m_editText.getText().toString());
            if (!SharedActivity.passwordField) SharedActivity.nativeOnKey(1, 500000, 0);
            SharedActivity.nativeCancelBtnPressed();
            UpdateEditBoxInView(false, false);
        }
        if (SharedActivity.m_editText.isFocused()) UpdateEditBoxRootViewPosition();
    }

    public void hideKeyboard(Activity activity) {
        View view = activity.findViewById(android.R.id.content);
        if (view != null) {
            ((InputMethodManager) activity.getSystemService("input_method")).hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 1 && googleSignInHelper != null) {
            googleSignInHelper.handleSignInResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onBackPressed() {
        if (webViewManager != null && webViewManager.IsVisible()) {
            AppLogger.log("Main", "Back pressed — dismissing WebView");
            webViewManager.HideWebView();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleGrowIntent(intent);
    }

    /**
     * Handles grow:// deep links that Chrome fires after Ubisoft's Google OAuth callback.
     * Ubisoft redirects to grow://?token=<LTOKEN> (or grow://?info=<LTOKEN>).
     * We extract the token and pass it to the engine via nativeOnScriptCall so login completes.
     */
    private void handleGrowIntent(android.content.Intent intent) {
        if (intent == null) return;
        android.net.Uri data = intent.getData();
        if (data == null || !"grow".equalsIgnoreCase(data.getScheme())) return;

        String token = data.getQueryParameter("token");
        if (token == null || token.isEmpty()) token = data.getQueryParameter("info");
        if (token == null || token.isEmpty()) {
            AppLogger.warn("GrowDeepLink", "grow:// intent had no token/info param: " + data);
            return;
        }

        final String finalToken = token;
        AppLogger.log("GrowDeepLink", "Received token via grow:// (len=" + finalToken.length() + ")");
        ZennKuyBridge.sTokenDelivered = true;
        ZennKuyBridge.sTokenDeliveredAt = System.currentTimeMillis();
        webViewManager.HideWebView();
        runOnUiThread(() -> Toast.makeText(this,
                "Token received — verifying login…", Toast.LENGTH_SHORT).show());
        if (zennKuyOverlay != null) zennKuyOverlay.showVerifyingBanner();

        // Persist ltoken so the engine's 30-second retry (checktoken → ltoken shortcut) can
        // reuse it without asking the user to pick their Google account again.
        try {
            LoginSpoof spoof = new LoginSpoof(this);
            spoof.setLtoken(finalToken);
            spoof.setEnabled(true);
            AppLogger.log("GrowDeepLink", "ltoken persisted to LoginSpoof");
        } catch (Throwable t) {
            AppLogger.warn("GrowDeepLink", "LoginSpoof persist failed: " + t.getMessage());
        }

        // Deliver via ZennKuy's nativeBypassLogin — ZennKuy ingests the ltoken and completes the
        // game session handshake internally, without triggering the growtopia engine's own
        // /google/native/callback HTTP verification round-trip that Ubisoft rate-limits.
        AppLogger.log("GrowDeepLink", "Delivering token via nativeBypassLogin (len=" + finalToken.length() + ")");
        try {
            ZennKuyRenderer.nativeBypassLogin(finalToken);
            AppLogger.log("GrowDeepLink", "nativeBypassLogin OK");
            // Wake the engine's connection loop on the GL thread so it dispatches the login
            // packet immediately instead of waiting for the 30-second checktoken timeout.
            if (SharedActivity.mGLView != null) {
                SharedActivity.mGLView.queueEvent(() -> {
                    try {
                        ZennKuyRenderer.nativeForcedOnlineMode(true);
                        AppLogger.log("GrowDeepLink", "nativeForcedOnlineMode(true) fired on GL thread");
                    } catch (Throwable ex) {
                        AppLogger.warn("GrowDeepLink", "nativeForcedOnlineMode threw: " + ex.getMessage());
                    }
                });
            } else {
                AppLogger.warn("GrowDeepLink", "mGLView null — nativeForcedOnlineMode skipped");
            }
        } catch (Throwable t) {
            AppLogger.error("GrowDeepLink", "nativeBypassLogin threw: " + t.getMessage());
        }
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        int h = config.screenHeightDp, w = config.screenWidthDp;
        if (h > w) { config.screenHeightDp = w; config.screenWidthDp = h; }
        super.onConfigurationChanged(config);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mainApp = this;
        googleSignInHelper = new GoogleSignInHelper(this);
        helpshiftManager = new HelpShiftManager(this);
        SharedActivity.dllname = "growtopia";
        this.BASE64_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArv12FD/xxuAJ3/B8Jgx78985UN/FitcQD5C21eIS5D+98yr7dy9sw8R2fSTFZKExBZVAfatgDH7s6fb9vfHi43szfpdXs3ZL2hsa7DeCWRyVSTD6o/i14vgwInv1S/dgLAwQth3PDXWF+zYXOlL+umOt9K9eqQo5CZhkwl9JAmMHlazvbhSGAldV5QsdY3pK5wmg/w2873abgYsGdI3B9wL75kgZW9tV2O6efiIbXlevktGOMup3Ql2H4Rcpa3ZeDtGl+YTQbEUQTYiYBDtFGCyqksXeM6+kCnaF97Ss5wA0w5ID9WJLkziXI4iGBMRd0a7s+vVniwpx771oGcJxewIDAQAB";
        SharedActivity.securityEnabled = false;
        SharedActivity.IAPEnabled = true;
        SharedActivity.HookedEnabled = false;
        SharedActivity.PackageName = SharedActivity.GROWTOPIA_PACKAGE;
        NativeLibraries.loadGame();
        super.onCreate(savedInstanceState);
        if (isFinishing()) return;
        Configuration configuration = getResources().getConfiguration();
        if (configuration.screenHeightDp > configuration.screenWidthDp) {
            int oldHeight = configuration.screenHeightDp;
            configuration.screenHeightDp = configuration.screenWidthDp;
            configuration.screenWidthDp = oldHeight;
            getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());
        }
        AppLogger.initFileLog(this);
        JavaInterface.injectActivityJava(this);
        handleGrowIntent(getIntent());
        this.heightProvider = new HeightProvider(this).setHeightListener(this::OnKeyboardHeightChanged);
        this.firebaseCrashlyticsManager = new FirebaseCrashlyticsManager(this);
        this.ironSourceManager.OnCreate();
        this.appReviewManager.OnCreate();
        this.zennKuyOverlay = new ZennKuyOverlay(this);
        this.zennKuyOverlay.attachTo(mViewGroup);
        this.usercentricsManager = new UsercentricsManager(this);
        getWindow().addFlags(128);
        logSavePath();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (this.heightProvider != null) this.heightProvider.OnPause();
        this.ironSourceManager.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.heightProvider != null) this.heightProvider.OnResume();
        this.ironSourceManager.onResume();
        logSavePath();
    }

    @Override public void onStart() { super.onStart(); }

    @Override
    public void onDestroy() {
        if (usercentricsManager != null) usercentricsManager.destroy();
        super.onDestroy();
    }

    @Override
    public void onStop() {
        com.gentz.launcher.CrashLogger.markLaunchFinished();
        super.onStop();
    }


    // ZennKuy Renderer - Static inner class for native rendering and message handling
    public static class ZennKuyRenderer {
        public static native void nativeDrawFrame();
        public static native int nativeGetMessageZennKuy();
        public static native void nativeSurfaceChanged(int width, int height);
        public static native boolean nativeOnTouch(int x, int y, int action);
        public static native void nativeForcedOnlineMode(boolean enabled);
        public static native void nativeBypassLogin(String token);
    }

}
