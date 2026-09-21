package com.rtsoft.growtopia;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;
import android.view.inputmethod.InputMethodManager;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class AppRenderer implements GLSurfaceView.Renderer {
    static final int MESSAGE_NONE = 0;
    static final int MESSAGE_OPEN_TEXT_BOX = 1;
    static final int MESSAGE_CLOSE_TEXT_BOX = 2;
    static final int MESSAGE_CHECK_CONNECTION = 3;
    static final int MESSAGE_SET_FPS_LIMIT = 4;
    static final int MESSAGE_SET_ACCELEROMETER_UPDATE_HZ = 5;
    static final int MESSAGE_FINISH_APP = 6;
    static final int MESSAGE_SET_VIDEO_MODE = 7;
    static final int MESSAGE_TAPJOY_GET_FEATURED_APP = 8;
    static final int MESSAGE_TAPJOY_GET_AD = 9;
    static final int MESSAGE_TAPJOY_GET_MOVIE = 10;
    static final int MESSAGE_TAPJOY_SHOW_FEATURED_APP = 11;
    static final int MESSAGE_TAPJOY_SHOW_AD = 12;
    static final int MESSAGE_TAPJOY_SHOW_MOVIE_AD = 13;
    static final int MESSAGE_IAP_PURCHASE = 14;
    static final int MESSAGE_IAP_GET_PURCHASED_LIST = 15;
    static final int MESSAGE_TAPJOY_SPEND_TAP_POINTS = 17;
    static final int MESSAGE_TAPJOY_AWARD_TAP_POINTS = 18;
    static final int MESSAGE_TAPJOY_SHOW_OFFERS = 19;
    static final int MESSAGE_HOOKED_SHOW_RATE_DIALOG = 20;
    static final int MESSAGE_ALLOW_SCREEN_DIMMING = 21;
    static final int MESSAGE_REQUEST_AD_SIZE = 22;
    static final int MESSAGE_CHARTBOOST_CACHE_INTERSTITIAL = 23;
    static final int MESSAGE_CHARTBOOST_SHOW_INTERSTITIAL = 24;
    static final int MESSAGE_CHARTBOOST_CACHE_MORE_APPS = 25;
    static final int MESSAGE_CHARTBOOST_SHOW_MORE_APPS = 26;
    static final int MESSAGE_CHARTBOOST_SETUP = 27;
    static final int MESSAGE_CHARTBOOST_NOTIFY_INSTALL = 28;
    static final int MESSAGE_FLURRY_SETUP = 31;
    static final int MESSAGE_FLURRY_ON_PAGE_VIEW = 32;
    static final int MESSAGE_FLURRY_LOG_EVENT = 33;
    static final int MESSAGE_SUSPEND_TO_HOME_SCREEN = 34;
    static final int MESSAGE_TAPJOY_INIT_MAIN = 35;
    static final int MESSAGE_TAPJOY_INIT_PAID_APP_WITH_ACTIONID = 36;
    static final int MESSAGE_TAPJOY_SET_USERID = 37;
    static final int MESSAGE_IAP_CONSUME_ITEM = 38;
    static final int MESSAGE_IAP_ITEM_DETAILS = 39;
    static final int MESSAGE_APPSFLYER_LOG_PURCHASE = 40;
    static final int MESSAGE_OPEN_TEXTBOX_SECRET = 41;
    static final int MESSAGE_FLURRY_START_TIMED_EVENT = 1001;
    static final int MESSAGE_FLURRY_STOP_TIMED_EVENT = 1002;
    static final int MESSAGE_APPSFLYER_EVENT = 1004;
    static final int MESSAGE_GETSOCIAL_EVENT = 1005;
    static final int MESSAGE_GETSOCIAL_LOGIN = 1006;
    static final int MESSAGE_GETSOCIAL_OPEN_UI = 1007;
    static final int MESSAGE_GETSOCIAL_ADD_FRIEND = 1008;
    static final int MESSAGE_GETSOCIAL_LOGOUT = 1009;
    static final int MESSAGE_TAPJOY_LOGOUT = 1010;
    static final int MESSAGE_SET_IAP_FLAG = 1011;

    static long m_gameTimer;
    static int m_timerLoopMS;
    public SharedActivity app;
    int width;
    int height;

    public AppRenderer(SharedActivity activity) {
        this.app = activity;
    }

    // Native method declarations
    private static native void nativeDone();
    private static native void nativeEmergencyMessageClear();
    private static native int nativeGetLastOSMessageParm1();
    private static native String nativeGetLastOSMessageString();
    private static native String nativeGetLastOSMessageString2();
    private static native String nativeGetLastOSMessageString3();
    private static native float nativeGetLastOSMessageX();
    private static native float nativeGetLastOSMessageY();
    private static native int nativeOSMessageGet();
    public static native void nativeRender();
    public static native void nativeResize(int width, int height, int orientation);
    public static native void nativeSetWindow(Surface surface);
    public static native void nativeUpdate();

    @Override
    public synchronized void onDrawFrame(GL10 gl) {
        try {
            if (this.app == null) return;

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            if (m_timerLoopMS != 0) {
                while (m_gameTimer > SystemClock.uptimeMillis() || m_gameTimer > SystemClock.uptimeMillis() + m_timerLoopMS + 1) {
                    SystemClock.sleep(1L);
                }
                m_gameTimer = SystemClock.uptimeMillis() + m_timerLoopMS;
            }

            if (!SharedActivity.bIsShuttingDown && Looper.myLooper() != Looper.getMainLooper()
                    && NativeLibraries.isGameLoaded()) {
                nativeUpdate();
                nativeRender();

                // ✅ CRITICAL: Call ZennKuy's native renderer every frame
                try {
                    Main.ZennKuyRenderer.nativeDrawFrame();
                } catch (UnsatisfiedLinkError e) {
                    Log.w("ZennKuy", "nativeDrawFrame failed: " + e.getMessage());
                }
            }

            // ✅ CRITICAL: Poll ZennKuy for messages (keyboard, etc)
            int iNativeGetMessageZennKuy = 0;
            try {
                iNativeGetMessageZennKuy = Main.ZennKuyRenderer.nativeGetMessageZennKuy();
            } catch (UnsatisfiedLinkError e) {
                Log.w("ZennKuy", "nativeGetMessageZennKuy failed: " + e.getMessage());
            }

            if (iNativeGetMessageZennKuy != 0) {
                InputMethodManager imm = (InputMethodManager) SharedActivity.app.getSystemService("input_method");
                if (iNativeGetMessageZennKuy == 1) {
                    imm.toggleSoftInput(2, 0);
                } else if (iNativeGetMessageZennKuy == 2) {
                    imm.hideSoftInputFromWindow(SharedActivity.mGLView.getWindowToken(), 0);
                }
            }

            // Proton OS message pump
            int msg;
            while (this.app != null && !SharedActivity.bIsShuttingDown && NativeLibraries.isGameLoaded() && (msg = nativeOSMessageGet()) != 0) {
                if (msg == MESSAGE_OPEN_TEXT_BOX || msg == MESSAGE_OPEN_TEXTBOX_SECRET) {
                    SharedActivity.passwordField = (msg == MESSAGE_OPEN_TEXTBOX_SECRET);
                    SharedActivity.m_text_max_length = nativeGetLastOSMessageParm1();
                    SharedActivity.m_text_default = nativeGetLastOSMessageString();
                    SharedActivity.m_before = nativeGetLastOSMessageString();
                    SharedActivity.updateText = true;
                    this.app.clearIngameInputBox();
                    this.app.ChangeEditBoxProperty();
                    SharedActivity.updateText = false;
                    this.app.toggle_keyboard(true);
                    this.app.mMainThreadHandler.post(this.app.mUpdateMainThread);
                } else if (msg == MESSAGE_CLOSE_TEXT_BOX) {
                    this.app.toggle_keyboard(false);
                    this.app.mMainThreadHandler.post(this.app.mUpdateMainThread);
                } else if (msg == MESSAGE_SET_FPS_LIMIT) {
                    float fps = nativeGetLastOSMessageX();
                    if (fps == 0.0f) {
                        m_timerLoopMS = 0;
                    } else {
                        m_timerLoopMS = (int) (1000.0f / fps);
                    }
                } else if (msg == MESSAGE_SET_ACCELEROMETER_UPDATE_HZ) {
                    this.app.setup_accel(nativeGetLastOSMessageX());
                } else if (msg == MESSAGE_FINISH_APP) {
                    SharedActivity.bIsShuttingDown = true;
                    this.app.mMainThreadHandler.post(this.app.mUpdateMainThread);
                } else if (msg == MESSAGE_ALLOW_SCREEN_DIMMING) {
                    if (nativeGetLastOSMessageX() != 0.0f) {
                        SharedActivity.set_allow_dimming_asap = true;
                    } else {
                        SharedActivity.set_disallow_dimming_asap = true;
                    }
                    this.app.mMainThreadHandler.post(this.app.mUpdateMainThread);
                } else {
                    // Other messages: log unhandled
                    if (msg != MESSAGE_IAP_PURCHASE && msg != MESSAGE_IAP_GET_PURCHASED_LIST &&
                            msg != MESSAGE_TAPJOY_SPEND_TAP_POINTS && msg != MESSAGE_TAPJOY_AWARD_TAP_POINTS &&
                            msg != MESSAGE_TAPJOY_SHOW_OFFERS && msg != MESSAGE_REQUEST_AD_SIZE &&
                            msg != MESSAGE_TAPJOY_GET_FEATURED_APP && msg != MESSAGE_TAPJOY_GET_AD &&
                            msg != MESSAGE_TAPJOY_GET_MOVIE && msg != MESSAGE_TAPJOY_SHOW_AD &&
                            msg != MESSAGE_TAPJOY_SHOW_FEATURED_APP && msg != MESSAGE_TAPJOY_SHOW_MOVIE_AD &&
                            msg != MESSAGE_CHARTBOOST_SETUP && msg != MESSAGE_CHARTBOOST_CACHE_INTERSTITIAL &&
                            msg != MESSAGE_CHARTBOOST_SHOW_INTERSTITIAL && msg != MESSAGE_FLURRY_SETUP &&
                            msg != MESSAGE_APPSFLYER_EVENT && msg != MESSAGE_SUSPEND_TO_HOME_SCREEN &&
                            msg != MESSAGE_TAPJOY_INIT_MAIN && msg != MESSAGE_TAPJOY_LOGOUT &&
                            msg != MESSAGE_SET_IAP_FLAG && msg != MESSAGE_IAP_CONSUME_ITEM &&
                            msg != MESSAGE_IAP_ITEM_DETAILS && msg != MESSAGE_APPSFLYER_LOG_PURCHASE) {
                        Log.v("AppRenderer", "Unhandled OS message " + msg);
                    }
                }
            }
        } catch (Throwable t) {
            Log.e("AppRenderer", "onDrawFrame error: " + t.getMessage());
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int w, int h) {
        GLES20.glViewport(0, 0, w, h);
        int orientation = 1;
        if (Main.app.getResources().getConfiguration().orientation == 2) {
            int rotation = Main.app.getWindowManager().getDefaultDisplay().getRotation();
            if (rotation == 1) orientation = 3;
            else if (rotation == 3) orientation = 4;
        }
        nativeResize(w, h, orientation);
        this.width = w;
        this.height = h;
        nativeSetWindow(SharedActivity.mGLView.getHolder().getSurface());

        // ✅ CRITICAL: Notify ZennKuy about surface dimension changes
        try {
            Main.ZennKuyRenderer.nativeSurfaceChanged(w, h);
            Log.d(SharedActivity.PackageName, "ZennKuy surface changed: " + w + "x" + h);
        } catch (UnsatisfiedLinkError e) {
            Log.w(SharedActivity.PackageName, "ZennKuy nativeSurfaceChanged failed: " + e.getMessage());
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        nativeSetWindow(SharedActivity.mGLView.getHolder().getSurface());
        if (SharedActivity.m_advertiserID.equals("")) {
            new Thread(() -> {
                try {
                    com.google.android.gms.ads.identifier.AdvertisingIdClient.Info info =
                            com.google.android.gms.ads.identifier.AdvertisingIdClient.getAdvertisingIdInfo(SharedActivity.app);
                    if (info != null) {
                        SharedActivity.m_advertiserID = info.getId();
                        SharedActivity.m_limitAdTracking = info.isLimitAdTrackingEnabled();
                    }
                } catch (Throwable t) {
                    android.util.Log.d(SharedActivity.PackageName, "AID fetch failed: " + t.getMessage());
                }
            }).start();
        }
        // Keep the native controller transition identical to the working launcher.
        // Growtopia advances from the menu after its consent/authentication callbacks;
        // forcing OnlineGameController here races that transition and leaves it stuck.
    }
}
