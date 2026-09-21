package com.rtsoft.growtopia;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.Vibrator;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.anzu.sdk.Anzu;
import com.tapjoy.Tapjoy;
import com.tapjoy.TJConnectListener;
import com.tapjoy.TJGetCurrencyBalanceListener;
import com.tapjoy.TJPlacement;
import com.tapjoy.TJPlacementVideoListener;

import com.gentz.launcher.App;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Locale;

public class SharedActivity extends AppCompatActivity implements SensorEventListener, TJGetCurrencyBalanceListener, TJPlacementVideoListener {
    public static boolean HookedEnabled = false;
    public static boolean IAPEnabled = false;
    static final int MESSAGE_OPEN_TEXTBOX_SECRET = 41;
    static final int MESSAGE_TYPE_ADD_COMPONENT = 18;
    static final int MESSAGE_TYPE_APP_VERSION = 47;
    static final int MESSAGE_TYPE_CALL_COMPONENT_FUNCTION_BY_NAME = 14;
    static final int MESSAGE_TYPE_CALL_ENTITY_FUNCTION = 13;
    static final int MESSAGE_TYPE_CALL_ENTITY_FUNCTION_RECURSIVELY = 40;
    static final int MESSAGE_TYPE_CALL_STATIC_FUNCTION = 46;
    static final int MESSAGE_TYPE_GUI_ACCELEROMETER = 4;
    static final int MESSAGE_TYPE_GUI_CHAR = 6;
    static final int MESSAGE_TYPE_GUI_CHAR_RAW = 23;
    static final int MESSAGE_TYPE_GUI_CLICK_END = 1;
    static final int MESSAGE_TYPE_GUI_CLICK_MOVE = 2;
    static final int MESSAGE_TYPE_GUI_CLICK_MOVE_RAW = 3;
    static final int MESSAGE_TYPE_GUI_CLICK_START = 0;
    static final int MESSAGE_TYPE_GUI_COPY = 9;
    static final int MESSAGE_TYPE_GUI_JOYPAD = 38;
    static final int MESSAGE_TYPE_GUI_JOYPAD_BUTTONS = 37;
    static final int MESSAGE_TYPE_GUI_JOYPAD_CONNECT = 39;
    static final int MESSAGE_TYPE_GUI_KEYBWD_CURSORPOS = 8;
    static final int MESSAGE_TYPE_GUI_KEYBWD_STRING = 7;
    static final int MESSAGE_TYPE_GUI_PASTE = 10;
    static final int MESSAGE_TYPE_GUI_TOGGLE_FULLSCREEN = 11;
    static final int MESSAGE_TYPE_GUI_TRACKBALL = 5;
    static final int MESSAGE_TYPE_HW_KEYBOARD_INPUT_ENDING = 43;
    static final int MESSAGE_TYPE_HW_KEYBOARD_INPUT_STARTING = 44;
    static final int MESSAGE_TYPE_HW_TOUCH_KEYBOARD_WILL_HIDE = 42;
    static final int MESSAGE_TYPE_HW_TOUCH_KEYBOARD_WILL_SHOW = 41;
    static final int MESSAGE_TYPE_IAP_ITEM_INFO_RESULT = 54;
    static final int MESSAGE_TYPE_IAP_ITEM_STATE = 29;
    static final int MESSAGE_TYPE_IAP_PURCHASED_LIST_STATE = 45;
    static final int MESSAGE_TYPE_IAP_RESULT = 28;
    static final int MESSAGE_TYPE_OS_CONNECTION_CHECKED = 19;
    static final int MESSAGE_TYPE_PLAY_MUSIC = 20;
    static final int MESSAGE_TYPE_PLAY_SOUND = 15;
    static final int MESSAGE_TYPE_PRELOAD_SOUND = 22;
    static final int MESSAGE_TYPE_REMOVE_COMPONENT = 17;
    static final int MESSAGE_TYPE_SET_ENTITY_VARIANT = 12;
    static final int MESSAGE_TYPE_SET_SOUND_ENABLED = 24;
    static final int MESSAGE_TYPE_TAPJOY_AD_READY = 25;
    static final int MESSAGE_TYPE_TAPJOY_AWARD_TAP_POINTS_RETURN = 34;
    static final int MESSAGE_TYPE_TAPJOY_AWARD_TAP_POINTS_RETURN_ERROR = 35;
    static final int MESSAGE_TYPE_TAPJOY_EARNED_TAP_POINTS = 36;
    static final int MESSAGE_TYPE_TAPJOY_FEATURED_APP_READY = 26;
    static final int MESSAGE_TYPE_TAPJOY_MOVIE_AD_READY = 27;
    static final int MESSAGE_TYPE_TAPJOY_SPEND_TAP_POINTS_RETURN = 32;
    static final int MESSAGE_TYPE_TAPJOY_SPEND_TAP_POINTS_RETURN_ERROR = 33;
    static final int MESSAGE_TYPE_TAPJOY_TAP_POINTS_RETURN = 30;
    static final int MESSAGE_TYPE_TAPJOY_TAP_POINTS_RETURN_ERROR = 31;
    static final int MESSAGE_TYPE_UNKNOWN = 21;
    static final int MESSAGE_TYPE_VIBRATE = 16;
    static final int MESSAGE_USER = 1000;
    /** The game package the engine and its assets belong to — never the launcher's own id. */
    public static final String GROWTOPIA_PACKAGE = "com.rtsoft.growtopia";
    public static String PackageName = GROWTOPIA_PACKAGE;
    /** Version of the bundled libgrowtopia.so; only a fallback for sendVersionDetails(). */
    public static String GameVersionName = com.gentz.launcher.LauncherConfig.GROWTOPIA_VERSION;
    static final int RC_REQUEST = 10001;
    static final int RESULT_BILLING_UNAVAILABLE = 3;
    static final int RESULT_DEVELOPER_ERROR = 5;
    static final int RESULT_ERROR = 6;
    static final int RESULT_ITEM_UNAVAILABLE = 4;
    static final int RESULT_OK = 0;
    static final int RESULT_OK_ALREADY_PURCHASED = 7;
    static final int RESULT_SERVICE_UNAVAILABLE = 2;
    static final int RESULT_USER_CANCELED = 1;
    static final int VIRTUAL_DPAD_BUTTON_DOWN = 500039;
    static final int VIRTUAL_DPAD_BUTTON_LEFT = 500036;
    static final int VIRTUAL_DPAD_BUTTON_RIGHT = 500038;
    static final int VIRTUAL_DPAD_BUTTON_UP = 500037;
    static final int VIRTUAL_DPAD_LBUTTON = 500042;
    static final int VIRTUAL_DPAD_RBUTTON = 500043;
    static final int VIRTUAL_DPAD_SELECT = 500040;
    static final int VIRTUAL_DPAD_START = 500041;
    static final int VIRTUAL_KEY_BACK = 500000;
    static final int VIRTUAL_KEY_DIR_CENTER = 500008;
    static final int VIRTUAL_KEY_DIR_DOWN = 500005;
    static final int VIRTUAL_KEY_DIR_LEFT = 500006;
    static final int VIRTUAL_KEY_DIR_RIGHT = 500007;
    static final int VIRTUAL_KEY_DIR_UP = 500004;
    static final int VIRTUAL_KEY_HOME = 500002;
    static final int VIRTUAL_KEY_PROPERTIES = 500001;
    static final int VIRTUAL_KEY_SEARCH = 500003;
    static final int VIRTUAL_KEY_SHIFT = 500011;
    static final int VIRTUAL_KEY_TRACKBALL_DOWN = 500035;
    static final int VIRTUAL_KEY_VOLUME_DOWN = 500010;
    static final int VIRTUAL_KEY_VOLUME_UP = 500009;
    private static float accelHzSave = 0.0f;
    public static int adBannerHeight = 0;
    public static int adBannerWidth = 0;
    public static RelativeLayout adLinearLayout = null;
    public static View adView = null;
    public static int apiVersion = 0;
    public static SharedActivity app = null;
    public static boolean bIsShuttingDown = false;
    private static String currentMusicPath = null;
    public static String dllname = "rtsomething";
    public static boolean isKeyboardExist = false;
    public static AppGLSurfaceView mGLView = null;
    public static Button m_CancelButton = null;
    public static Button m_DoneButton = null;
    public static int m_KeyBoardHeight = 0;
    public static String m_advertiserID = "";
    public static String m_before = "";
    public static EditText m_editText = null;
    public static RelativeLayout m_editTextRoot = null;
    public static boolean m_focusOffKeyboard = false;
    public static boolean m_focusOnKeyboard = false;
    private static float m_lastMusicVol = 1.0f;
    public static boolean m_limitAdTracking = false;
    public static String m_text_default = "";
    public static int m_text_max_length = 168;
    public static int maxLength = -1;
    public static boolean passwordField = false;
    public static boolean run_hooked = false;
    public static boolean securityEnabled = false;
    private static Sensor sensor = null;
    private static SensorManager sensorManager = null;
    public static boolean set_allow_dimming_asap = false;
    public static boolean set_disallow_dimming_asap = false;
    public static String tapBannerSize = "";
    public static int tapjoy_ad_show;
    public static int tempNum;
    public static boolean updateText;
    public static boolean update_display_ad;

    public RelativeLayout mViewGroup;
    public TJPlacement offerwallPlacement;
    public TJPlacement tapjoyAdPlacementForSub01;
    public TJPlacement tapjoyAdPlacementForTV;
    public IAPManager iapManager = new IAPManager(null);
    public boolean is_demo = false;
    public String BASE64_PUBLIC_KEY = "this will be set in your app's Main.java";
    public MediaPlayer _music = null;
    private MusicFadeOutThread musicFadeOutThread = null;
    public SoundPool _sounds = new SoundPool(8, 3, 0);

    final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());
    final Runnable mUpdateMainThread = new Runnable() {
        @Override
        public void run() {
            if (!bIsShuttingDown) {
                SharedActivity.this.updateResultsInUi();
            } else {
                app.finish();
                Process.killProcess(Process.myPid());
            }
        }
    };

    // === Native method declarations (JNI bridge) ===
    public static native void nativeCancelBtnPressed();
    public static native int nativeGetChatString();
    public static native float nativeGetEditBoxOffset();
    public static native float nativeGetScreenHeight();
    public static native float nativeGetScreenWidth();
    public static native void nativeInitActivity(Activity activity);
    public static native void nativeOnAccelerometerUpdate(float f1, float f2, float f3);
    public static native void nativeOnInputText(String str);
    public static native void nativeOnKey(int state, int virtualKey, int unicodeChar);
    public static native void nativeOnTrackball(float x, float y);
    public static native void nativeSendGUIEx(int msgType, int parm1, int parm2, int finger);
    public static native void nativeSendGUIStringEx(int msgType, int parm1, int parm2, int parm3, String str);
    public static native void nativeUpdateConsoleLogPos(float pos);
    public static native void appOnAdInteractionFailed(String placement, String reason);

    // === Static helper methods called by native engine via JNI ===
    public static void HandleAchievement(String str) {
        Log.v("Achievement", "Unlocked value: " + str);
        app.FireAchievement(str);
    }

    public static void LaunchURL(String str) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setData(Uri.parse(str));
        try {
            app.startActivity(intent);
        } catch (ActivityNotFoundException unused) {
            Log.v("LaunchURL", "Couldn't find activity to launch URL!");
        }
    }

    public static void _OpenCSTS(String uid, String country, String language, boolean payer, String playerId, String env, String misc) {
        Intent intent = new Intent(app.getApplicationContext(), CSTSWebViewActivity.class);
        intent.putExtra("cstsuid", uid);
        intent.putExtra("country", country);
        intent.putExtra("language", language);
        intent.putExtra("payer", payer);
        intent.putExtra("ingameplayerid", playerId);
        intent.putExtra("environment", env);
        intent.putExtra("misc", misc);
        app.startActivity(intent);
    }

    public static void create_dir_recursively(String base, String path) {
        new File(base + "/" + path).mkdirs();
    }

    public static String get_Appsflyer_UID() {
        return app.GetAppsflyerUID();
    }

    public static String get_advertisingIdentifier() {
        return App.getData("gid");
    }

    public static String get_apkFileName() {
        // PackageName is com.rtsoft.growtopia: this launcher ships only override assets
        // (StartScreen, WorldUI, items.dat, menu.json) and mounts the installed Growtopia
        // APK for the ~2900 GameData files, exactly like the real Growlauncher does.
        // Fall back to our own bundled assets only if Growtopia is not installed.
        try {
            return app.getPackageManager().getApplicationInfo(PackageName, 0).sourceDir;
        } catch (PackageManager.NameNotFoundException e) {
            try {
                return app.getPackageManager().getApplicationInfo(app.getPackageName(), 0).sourceDir;
            } catch (PackageManager.NameNotFoundException e2) {
                throw new RuntimeException("Unable to locate assets, aborting...", e2);
            }
        }
    }

    public static String get_cantSupportTrees() {
        return "4322";
    }

    public static String get_clipboard() {
        try {
            ClipboardManager cm = (ClipboardManager) app.getSystemService("clipboard");
            if (cm != null && cm.getText() != null) {
                return cm.getText().toString();
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    public static String get_deviceID() {
        String data = App.getData("mac");
        return "35" + (data.length() % 10) + (data.hashCode() % 10) + ((data.hashCode() % 10) + data.length()) + ((data.hashCode() % 10) + data.hashCode()) + ((data.length() % 10) + data.length()) + ((data.hashCode() * 2) % 10) + ((data.hashCode() * 5) % 10) + (data.hashCode() % 10) + ((data.hashCode() / 2) % 10) + ((data.hashCode() * 6) % 10) + (data.hashCode() % 10) + ((data.hashCode() * 2) % 10) + ((data.hashCode() / 3) % 10);
    }

    public static String get_device_model() {
        return Build.MODEL;
    }

    public static String get_device_os() {
        return Build.VERSION.RELEASE;
    }

    public static String get_docdir() {
        if (App.f10088p != null) {
            File f = App.f10088p.getExternalFilesDir(null);
            return f != null ? f.getAbsolutePath() : "";
        }
        return app.getExternalFilesDir(null).getAbsolutePath();
    }

    public static String get_externaldir() {
        if (App.f10088p != null) {
            File f = App.f10088p.getExternalFilesDir(null);
            return f != null ? f.getAbsolutePath() : "";
        }
        File f = app.getExternalFilesDir(null);
        return f != null ? f.getAbsolutePath() : "";
    }

    public static String get_getNetworkType() {
        try {
            ConnectivityManager cm = (ConnectivityManager) app.getSystemService("connectivity");
            if (cm.getNetworkInfo(1) != null && cm.getNetworkInfo(1).isConnected()) return "wifi";
            if (cm.getNetworkInfo(0) != null && cm.getNetworkInfo(0).isConnected()) return "mobile";
        } catch (Exception e) {
            Log.d("DeviceNetwork", "" + e.getMessage());
        }
        return "none";
    }

    public static String get_language() {
        Locale locale = Locale.getDefault();
        String country = locale.getCountry();
        if (country.isEmpty()) {
            country = "US"; // UbiServices requires BCP 47 format: "en-US", not "en"
        }
        return locale.getLanguage().toLowerCase() + "-" + country.toUpperCase();
    }

    public static String get_macAddress() {
        return App.getData("mac");
    }

    public static String get_region() {
        Locale locale = Locale.getDefault();
        return (locale.getLanguage() + "_" + locale.getCountry()).toLowerCase();
    }

    public static int is_app_installed(String pkg) {
        return 0;
    }

    public static void makeToastUI(Activity activity, String str) {
        if (activity == null) return;
        activity.runOnUiThread(() -> {
            Toast toast = Toast.makeText(activity, str, Toast.LENGTH_LONG);
            toast.setGravity(17, 0, 0);
            toast.show();
        });
    }

    // === Music methods ===
    public static void music_fadeout(int duration) {
        synchronized (SharedActivity.class) {
            MediaPlayer mp = app._music;
            if (mp != null && mp.isPlaying()) {
                if (duration <= 0) {
                    music_stop();
                } else {
                    MusicFadeOutThread t = app.musicFadeOutThread;
                    if (t == null || !t.isAlive()) {
                        app.musicFadeOutThread = new MusicFadeOutThread(duration);
                        app.musicFadeOutThread.start();
                    }
                }
            }
        }
    }

    public static int music_get_pos() {
        synchronized (SharedActivity.class) {
            MediaPlayer mp = app._music;
            if (mp != null) {
                try { return mp.getCurrentPosition(); } catch (Exception e) {}
            }
        }
        return 0;
    }

    public static boolean music_is_playing() {
        synchronized (SharedActivity.class) {
            MediaPlayer mp = app._music;
            if (mp != null) {
                try { return mp.isPlaying(); } catch (Exception e) {}
            }
        }
        return false;
    }

    public static void music_play(String path, boolean looping) {
        synchronized (SharedActivity.class) {
            try {
                if (app._music != null) {
                    app._music.reset();
                } else {
                    app._music = new MediaPlayer();
                }
                if (path.charAt(0) == '/') {
                    FileInputStream fis = new FileInputStream(path);
                    app._music.setDataSource(fis.getFD());
                    fis.close();
                } else {
                    AssetFileDescriptor afd = App.a().openFd(path);
                    app._music.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                    afd.close();
                }
                app._music.setLooping(looping);
                app._music.setVolume(m_lastMusicVol, m_lastMusicVol);
                app._music.prepare();
                app._music.start();
                currentMusicPath = path;
            } catch (Exception e) {
                Log.e(PackageName, "music_play error: " + e.getMessage());
            }
        }
    }

    public static void music_set_pos(int pos) {
        synchronized (SharedActivity.class) {
            MediaPlayer mp = app._music;
            if (mp != null) {
                try { mp.seekTo(pos); } catch (Exception e) {}
            }
        }
    }

    public static void music_set_volume(float vol) {
        synchronized (SharedActivity.class) {
            m_lastMusicVol = vol;
            MediaPlayer mp = app._music;
            if (mp != null) {
                try { mp.setVolume(vol, vol); } catch (Exception e) {}
            }
        }
    }

    public static void music_stop() {
        synchronized (SharedActivity.class) {
            MusicFadeOutThread t = app.musicFadeOutThread;
            if (t != null && t.isAlive()) {
                t.interrupt();
                try { t.join(); } catch (InterruptedException e) {}
            }
            MediaPlayer mp = app._music;
            if (mp != null) {
                try { mp.stop(); } catch (Exception e) {}
            }
        }
    }

    // === Sound methods ===
    public static void sound_destroy() {
        synchronized (SharedActivity.class) {
            if (app._sounds != null) {
                app._sounds.release();
                app._sounds = null;
            }
        }
    }

    public static void sound_init() {
        synchronized (SharedActivity.class) {
            if (app._sounds == null) {
                app._sounds = new SoundPool(8, 3, 0);
            }
        }
    }

    public static void sound_kill(int id) {
        app._sounds.unload(id);
    }

    public static int sound_load(String path) {
        if (path.charAt(0) == '/') {
            return app._sounds.load(path, 1);
        }
        try {
            AssetFileDescriptor afd = App.a().openFd(path);
            return app._sounds.load(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength(), 1);
        } catch (IOException e) {
            Log.d("Can't load sound", path);
            return 0;
        }
    }

    public static int sound_play(int id, float leftVol, float rightVol, int priority, int loop, float rate) {
        synchronized (SharedActivity.class) {
            try {
                return app._sounds.play(id, leftVol, rightVol, priority, loop, rate);
            } catch (Exception e) {
                Log.e(PackageName, "PlaySound error: " + e.getMessage());
                return 0;
            }
        }
    }

    public static void sound_set_rate(int streamId, float rate) {
        app._sounds.setRate(streamId, rate);
    }

    public static void sound_set_vol(int streamId, float left, float right) {
        app._sounds.setVolume(streamId, left, right);
    }

    public static void sound_stop(int streamId) {
        app._sounds.stop(streamId);
    }

    public static void vibrate(int ms) {
        synchronized (SharedActivity.class) {
            try {
                ((Vibrator) app.getSystemService("vibrator")).vibrate(ms);
            } catch (Exception e) {}
        }
    }

    public static void setViewVisibility(View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                setViewVisibility(vg.getChildAt(i), visible);
            }
        }
    }

    // === Instance methods ===
    public String GetAppsflyerUID() { return ""; }

    public void FireAchievement(String str) {
        Log.v("Achievement", "Firing: " + str);
    }

    public int TranslateKeycodeToProtonVirtualKey(int keycode) {
        if (keycode == 0) return VIRTUAL_KEY_SHIFT;
        if (keycode == 4) return VIRTUAL_KEY_BACK;
        if (keycode == 82) return VIRTUAL_KEY_PROPERTIES;
        if (keycode == 84) return VIRTUAL_KEY_SEARCH;
        switch (keycode) {
            case 19: return VIRTUAL_KEY_DIR_UP;
            case 20: return VIRTUAL_KEY_DIR_DOWN;
            case 21: return VIRTUAL_KEY_DIR_LEFT;
            case 22: return VIRTUAL_KEY_DIR_RIGHT;
            case 23: return VIRTUAL_KEY_DIR_CENTER;
            case 24: return VIRTUAL_KEY_VOLUME_UP;
            case 25: return VIRTUAL_KEY_VOLUME_DOWN;
            default: return keycode;
        }
    }

    public void toggle_keyboard(final boolean show) {
        runOnUiThread(() -> {
            final InputMethodManager imm = (InputMethodManager) getSystemService("input_method");
            if (show) {
                clearIngameInputBox();
                UpdateEditBoxInView(true, false);
                m_editText.post(() -> {
                    if (!imm.showSoftInput(m_editText, InputMethodManager.SHOW_IMPLICIT)) {
                        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                    }
                });
                m_focusOnKeyboard = true;
            } else {
                imm.hideSoftInputFromWindow(m_editText.getWindowToken(), 0);
                UpdateEditBoxInView(false, false);
                m_focusOnKeyboard = false;
            }
        });
    }

    public void UpdateEditBoxInView(boolean show, boolean clear) {
        setViewVisibility(m_editTextRoot, show);
        if (show) {
            m_editText.setText(m_text_default);
            m_editText.setSelection(m_editText.getText().length());
            maxLength = -1;
            UpdateRelativeElementsPosition();
            m_editText.setFocusableInTouchMode(true);
            m_editText.requestFocus();
        } else {
            if (clear) {
                m_editText.setText("");
                m_editText.setSelection(m_editText.getText().length());
                nativeOnInputText("");
            } else {
                nativeOnInputText(m_editText.getText().toString());
            }
            nativeOnKey(1, VIRTUAL_KEY_BACK, 0);
            m_editText.setFocusable(false);
        }
    }

    public void UpdateEditBoxRootViewPosition() {
        m_editText.measure(0, 0);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(-1, m_editText.getMeasuredHeight());
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lp.setMargins(0, 0, 0, m_KeyBoardHeight);
        m_editTextRoot.setLayoutParams(lp);
    }

    private void UpdateRelativeElementsPosition() {
        float screenWidth = (int) nativeGetScreenWidth();
        int btnWidth = (int) (0.12f * screenWidth);
        m_editText.measure(0, 0);
        int height = m_editText.getMeasuredHeight();
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams((int) (screenWidth * 0.7f), height);
        lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        lp.setMargins((int) nativeGetEditBoxOffset(), 0, 0, 0);
        m_editText.setLayoutParams(lp);
        m_editText.setSelection(m_editText.getText().length());

        RelativeLayout.LayoutParams doneLp = new RelativeLayout.LayoutParams(btnWidth, height);
        doneLp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        doneLp.setMargins(0, 0, btnWidth, 0);
        m_DoneButton.setLayoutParams(doneLp);
        m_DoneButton.setBackgroundColor(0);
        m_DoneButton.setTextColor(Color.parseColor("#5c5ac7"));
        m_DoneButton.setText("Done");

        RelativeLayout.LayoutParams cancelLp = new RelativeLayout.LayoutParams(btnWidth, height);
        cancelLp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        cancelLp.setMargins(0, 0, 0, 0);
        m_CancelButton.setLayoutParams(cancelLp);
        m_CancelButton.setBackgroundColor(0);
        m_CancelButton.setTextColor(Color.parseColor("#5c5ac7"));
        m_CancelButton.setText("Cancel");
    }

    public void ChangeEditBoxProperty() {
        runOnUiThread(() -> {
            if (passwordField) {
                m_editText.setInputType(524417);
                m_editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(18)});
            } else {
                m_editText.setInputType(524433);
                m_editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10000000)});
            }
        });
    }

    public boolean isAcceptableTextLength(int len) {
        int ml = maxLength;
        if (ml >= 120) return false;
        if (ml == 119) {
            m_editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(ml + 1)});
            return true;
        }
        m_editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10000)});
        return true;
    }

    public void clearIngameInputBox() {
        runOnUiThread(() -> {
            m_before = m_text_default;
            m_editText.setText(m_text_default);
            m_editText.setSelection(m_editText.getText().length());
        });
    }

    public void setup_accel(float hz) {
        accelHzSave = hz;
        if (sensorManager == null) {
            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        }
        if (hz == 0.0f) {
            sensorManager.unregisterListener(this);
        } else {
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (sensor != null) {
                sensorManager.registerListener(this, sensor, (int) (1000000.0f / hz));
            }
        }
    }

    /**
     * Reports the CLIENT GAME version to the engine (GUI message 47).
     *
     * This must be the Growtopia version, not the launcher version: the engine
     * compares it against the version the server requires, and a mismatch leaves
     * the session stuck once the player tries to go online. Both the official
     * 5.55 client and the real Growlauncher read it from the installed
     * com.rtsoft.growtopia package, so do the same and only fall back to the
     * bundled engine version when Growtopia is not installed.
     */
    public void sendVersionDetails() {
        if (!NativeLibraries.isGameLoaded()) return;
        String version;
        try {
            version = getPackageManager().getPackageInfo(GROWTOPIA_PACKAGE, 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(PackageName, "Cannot load App Version, using bundled engine version");
            version = GameVersionName;
        }
        try {
            nativeSendGUIStringEx(47, 0, 0, 0, version);
        } catch (Throwable t) {
            Log.e(PackageName, "sendVersionDetails error: " + t.getMessage());
        }
    }

    public void videoComplete() { nativeSendGUIStringEx(27, 2, 0, 0, ""); }
    public void videoReady() { nativeSendGUIStringEx(27, 1, 0, 0, ""); }
    public void videoStart() {}

    public void earnedTapPoints(int points) { nativeSendGUIStringEx(36, points, 0, 0, ""); }
    public void getAwardPointsResponse(String str, int points) { nativeSendGUIStringEx(34, points, 0, 0, str); }
    public void getAwardPointsResponseFailed(String str) { nativeSendGUIStringEx(35, 0, 0, 0, str); }
    public void getSpendPointsResponse(String str, int points) { nativeSendGUIStringEx(32, points, 0, 0, str); }
    public void getSpendPointsResponseFailed(String str) { nativeSendGUIStringEx(33, 0, 0, 0, str); }
    public void getUpdatePoints(String str, int points) { nativeSendGUIStringEx(30, points, 0, 0, str); }
    public void getUpdatePointsFailed(String str) { nativeSendGUIStringEx(31, 0, 0, 0, str); }

    public void getDisplayAdResponse(View view) {
        adView = view;
        nativeSendGUIEx(25, 1, 0, 0);
    }
    public void getDisplayAdResponseFailed(String str) { nativeSendGUIEx(25, 0, 0, 0); }
    public void getFullScreenAdResponse() {}
    public void getFullScreenAdResponseFailed(int code) {}

    public void makeToastUI(String str) {
        makeToastUI(this, str);
    }

    public void onApplsFlyerLogEvent(String event, String value) {}
    public void onApplsFlyerLogPurchase(String a, String b, String c) {}

    private void updateResultsInUi() {
        if (mGLView == null) return;
        if (set_allow_dimming_asap) {
            set_allow_dimming_asap = false;
            mGLView.setKeepScreenOn(false);
        }
        if (set_disallow_dimming_asap) {
            set_disallow_dimming_asap = false;
            mGLView.setKeepScreenOn(true);
        }
        if (m_focusOnKeyboard) { m_focusOnKeyboard = false; }
        if (m_focusOffKeyboard) {
            m_focusOffKeyboard = false;
            mGLView.requestFocus();
        }
        if (update_display_ad) {
            update_display_ad = false;
            adLinearLayout.removeAllViews();
            if (tapjoy_ad_show == 1) {
                adLinearLayout.addView(adView);
            }
        }
    }

    // === Lifecycle ===
    @Override
    public void onCreate(Bundle savedInstanceState) {
        app = this;
        if (!NativeLibraries.loadGame()) {
            super.onCreate(savedInstanceState);
            Log.e(PackageName, "lib" + NativeLibraries.GAME_LIBRARY + ".so is missing, cannot start the game");
            Toast.makeText(this, "Growtopia engine library is missing from this build.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        nativeInitActivity(this);
        apiVersion = Build.VERSION.SDK_INT;
        Log.d(PackageName, "API Level: " + apiVersion);
        SharedMultiTouchInput.init(this);
        super.onCreate(savedInstanceState);

        mGLView = new AppGLSurfaceView(this, this);
        Window window = getWindow();
        window.setFlags(1024, 1024);

        mViewGroup = new RelativeLayout(this);
        mViewGroup.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
        RelativeLayout.LayoutParams glParams = new RelativeLayout.LayoutParams(-1, -1);
        glParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        mGLView.setLayoutParams(glParams);
        mViewGroup.addView(mGLView);

        // Immersive fullscreen
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                window.setDecorFitsSystemWindows(false);
                View decorView = window.getDecorView();
                if (decorView != null) {
                    WindowInsetsController controller = decorView.getWindowInsetsController();
                    if (controller != null) {
                        controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                        controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                    }
                }
            } else {
                window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            }
            window.setStatusBarColor(0);
            window.setNavigationBarColor(0);
        } catch (Exception e) {
            Log.e("Growtopia", "Immersive fullscreen setup failed: " + e.getMessage());
        }

        setContentView(mViewGroup);
        CreateEditBox();
        AddEditBoxListeners();
        mGLView.requestFocus();
        setVolumeControlStream(3);

        adLinearLayout = new RelativeLayout(this);
        update_display_ad = false;
        run_hooked = false;
        tapjoy_ad_show = 0;

        Anzu.SetContext(this);
        clearIngameInputBox();
        sendVersionDetails();
    }

    private void CreateEditBox() {
        m_editText = new EditText(this);
        m_editText.setText("");
        m_editText.setSelection(m_editText.getText().length());
        m_editText.setImeOptions(-1845493760);
        m_editText.setImeActionLabel("DONE", 6);
        m_editText.setInputType(524433);
        m_editText.setGravity(80);
        m_editText.setMaxLines(3);
        m_editText.setBackgroundColor(-1);
        m_editText.setTextColor(-16777216);
        try { m_editText.setTextIsSelectable(true); } catch (NoSuchMethodError e) {}
        CreateEditBoxBG();
        UpdateEditBoxInView(false, true);
    }

    private void CreateEditBoxBG() {
        m_editTextRoot = new RelativeLayout(this);
        m_DoneButton = new Button(this);
        m_CancelButton = new Button(this);
        mViewGroup.addView(m_editTextRoot);
        m_editTextRoot.addView(m_editText);
        m_editTextRoot.addView(m_DoneButton);
        m_editTextRoot.addView(m_CancelButton);
        m_editText.measure(0, 0);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(-1, m_editText.getMeasuredHeight());
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lp.setMargins(0, 0, 0, m_KeyBoardHeight);
        m_editTextRoot.setBackgroundColor(Color.parseColor("#e5e5e7"));
        m_editTextRoot.setLayoutParams(lp);
        m_DoneButton.setOnClickListener(v -> {
            ((InputMethodManager) app.getSystemService("input_method")).hideSoftInputFromWindow(mGLView.getWindowToken(), 0);
            nativeOnKey(1, 13, 13);
            nativeOnInputText(m_editText.getText().toString());
            mGLView.requestFocus();
        });
        m_CancelButton.setOnClickListener(v -> {
            nativeOnInputText(m_editText.getText().toString());
            nativeCancelBtnPressed();
            toggle_keyboard(false);
        });
    }

    private void AddEditBoxListeners() {
        m_editText.setOnFocusChangeListener((v, hasFocus) -> {});
        try {
            m_editText.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    isKeyboardExist = false;
                    nativeOnKey(1, 0, 13);
                    nativeOnKey(0, 0, 13);
                    m_editText.setText("");
                    m_editText.setSelection(m_editText.getText().length());
                    return true;
                }
                return false;
            });
        } catch (NoClassDefFoundError e) {}
        try {
            m_editText.setOnEditorActionListener((tv, actionId, event) -> {
                if (actionId == 3 || actionId == 6) {
                    ((InputMethodManager) app.getSystemService("input_method")).hideSoftInputFromWindow(mGLView.getWindowToken(), 0);
                    nativeOnInputText(m_editText.getText().toString());
                    nativeOnKey(1, 13, 13);
                    mGLView.requestFocus();
                    return true;
                }
                return false;
            });
        } catch (NoClassDefFoundError e) {}
        m_editText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (updateText) return;
                int chatMax = nativeGetChatString();
                maxLength = chatMax;
                if (chatMax != -1) {
                    if (s.length() - m_before.length() < 0 && maxLength == 120) {
                        maxLength = maxLength - 1;
                    }
                    if (!isAcceptableTextLength(s.length())) return;
                }
                for (int i = 0; i < m_before.length(); i++) {
                    nativeOnKey(1, 67, 0);
                }
                nativeOnInputText("");
                for (int i = 0; i < s.length(); i++) {
                    char c = s.charAt(i);
                    nativeOnKey(1, 0, c);
                    nativeOnKey(0, 0, c);
                }
                m_before = s.toString();
            }
        });
    }

    @Override
    public void onDestroy() {
        Log.d(PackageName, "Destroying...");
        super.onDestroy();
    }

    @Override
    public void onPause() {
        synchronized (this) {
            InputMethodManager imm = (InputMethodManager) getSystemService("input_method");
            if (mGLView != null) imm.hideSoftInputFromWindow(mGLView.getWindowToken(), 0);
            if (m_editText != null) {
                imm.hideSoftInputFromWindow(m_editText.getWindowToken(), 0);
                m_editText.setText("");
                if (NativeLibraries.isGameLoaded()) UpdateEditBoxInView(false, false);
            }
            float savedHz = accelHzSave;
            setup_accel(0.0f);
            accelHzSave = savedHz;
            if (mGLView != null) mGLView.onPause();
            super.onPause();
        }
    }

    @Override
    public void onResume() {
        synchronized (this) {
            music_set_volume(m_lastMusicVol);
            if (mGLView != null) mGLView.onResume();
            if (NativeLibraries.isGameLoaded()) setup_accel(accelHzSave);
            super.onResume();
            if (iapManager != null) iapManager.RequestAIPPurchasedList();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Tapjoy.onActivityStart(this);
    }

    @Override
    public void onStop() {
        Tapjoy.onActivityStop(this);
        super.onStop();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!NativeLibraries.isGameLoaded()) return super.onKeyDown(keyCode, event);
        if (keyCode == 67) return true;
        if (event.getRepeatCount() > 0) return super.onKeyDown(keyCode, event);
        if (event.isAltPressed() && keyCode == 4) {
            nativeOnKey(1, VIRTUAL_DPAD_BUTTON_RIGHT, event.getUnicodeChar());
            return true;
        }
        if (keyCode == 4) {
            nativeOnKey(1, VIRTUAL_KEY_BACK, event.getUnicodeChar());
            return true;
        }
        nativeOnKey(1, TranslateKeycodeToProtonVirtualKey(keyCode), (char) event.getUnicodeChar());
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (!NativeLibraries.isGameLoaded()) return super.onKeyUp(keyCode, event);
        if (keyCode == 67) return true;
        if (event.isAltPressed() && keyCode == 4) {
            nativeOnKey(0, VIRTUAL_DPAD_BUTTON_RIGHT, event.getUnicodeChar());
            return true;
        }
        if (keyCode == 4) {
            nativeOnKey(0, VIRTUAL_KEY_BACK, event.getUnicodeChar());
            return true;
        }
        nativeOnKey(0, TranslateKeycodeToProtonVirtualKey(keyCode), (char) event.getUnicodeChar());
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
        return super.onKeyMultiple(keyCode, count, event);
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            nativeOnTrackball(event.getX(), event.getY());
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            nativeOnKey(1, VIRTUAL_KEY_TRACKBALL_DOWN, VIRTUAL_KEY_TRACKBALL_DOWN);
            return false;
        }
        return false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!NativeLibraries.isGameLoaded()) return;
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float[] v = event.values;
            if (v.length >= 3) {
                nativeOnAccelerometerUpdate(v[0], v[1], v[2]);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // === Inner classes ===
    public static class MusicFadeOutThread extends Thread {
        private final int m_duration;
        public MusicFadeOutThread(int duration) { this.m_duration = duration; }
        @Override
        public void run() {
            int steps = m_duration / 100;
            int remaining = steps;
            while (remaining > 0) {
                synchronized (app._music) {
                    float factor = (float) remaining / steps;
                    app._music.setVolume(m_lastMusicVol * factor, m_lastMusicVol * factor);
                    remaining--;
                }
                try { Thread.sleep(100L); } catch (InterruptedException e) { return; }
            }
            synchronized (app._music) {
                app._music.stop();
                app._music.setVolume(m_lastMusicVol, m_lastMusicVol);
            }
        }
    }

    // Tapjoy/offerwall stubs
    public void requestOfferwall(String name) {}
    public void requestOfferwallAndShow(String name) {}
    public void requestPlacement(String name) {}
    public void requestPlacementAndShow(String name) {}
    public void onConnectToTapjoy(String appId) {
        java.util.Hashtable<String, String> flags = new java.util.Hashtable<>();
        flags.put("TJC_OPTION_ENABLE_LOGGING", "false");
        flags.put("TJC_OPTION_DISABLE_ANDROID_ID_AS_ANALYTICS_ID", "true");
        Tapjoy.connect(getApplicationContext(), appId, flags, new TJConnectListener() {
            @Override
            public void onConnectSuccess() {
                android.util.Log.d(PackageName, "Tapjoy connect success");
            }
            @Override
            public void onConnectFailure() {
                android.util.Log.d(PackageName, "Tapjoy connect failed");
            }
        });
    }
    public void onVideoComplete(TJPlacement p) {}
    public void onVideoError(TJPlacement p, String err) {}
    public void onVideoStart(TJPlacement p) {}
    public void onGetCurrencyBalanceResponse(String currency, int balance) {}
    public void onGetCurrencyBalanceResponseFailure(String err) {}
}
