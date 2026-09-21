package com.rtsoft.growtopia;

import android.app.Activity;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/** Google sign-in compatibility bridge for Growtopia 5.57. */
public class GoogleSignInHelper {
    private static final String TAG = "GoogleSignInHelper";
    static final int RC_GOOGLE_SIGNIN = 9001;
    private static final int MAX_DELIVER_RETRIES = 40;

    final Activity mainActivity;
    private final LoginSpoof spoof;
    private final Handler deliverHandler = new Handler(Looper.getMainLooper());

    public GoogleSignInHelper(Activity activity) {
        mainActivity = activity;
        spoof = new LoginSpoof(activity);
    }

    public native void OnSignIn(int code, String token);
    public void Init() {}
    public void SignOut() {}

    public void SignIn() {
        if (Main.mainApp == null) {
            Log.e(TAG, "SignIn: mainApp is null");
            return;
        }
        mainActivity.runOnUiThread(() -> {
            spoof.setGoogleLogs("Opening Growtopia Google login");
            ZennKuyBridge.startResolving();
        });
    }

    public void handleSignInResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != RC_GOOGLE_SIGNIN) return;
        Log.w(TAG, "Unexpected SDK sign-in result ignored");
    }

    public void deliverResult(int code, String token) {
        if (code == 0 && token != null && !token.isEmpty()) {
            spoof.setGoogleLogs("Google sign-in OK (token length=" + token.length() + ")");
        } else {
            spoof.setGoogleLogs("Google sign-in failed (code=" + code + ")");
        }
        deliverToGl(code, token, 0);
    }

    private void deliverToGl(int code, String token, int attempt) {
        GLSurfaceView glView = SharedActivity.mGLView;
        if (glView == null) {
            if (attempt >= MAX_DELIVER_RETRIES) {
                Log.w(TAG, "GL view never became available; dropping sign-in result");
                return;
            }
            deliverHandler.postDelayed(() -> deliverToGl(code, token, attempt + 1), 100);
            return;
        }
        glView.queueEvent(() -> {
            try {
                OnSignIn(code, token);
            } catch (UnsatisfiedLinkError error) {
                Log.w(TAG, "OnSignIn native unavailable: " + error.getMessage());
            }
        });
    }
}
