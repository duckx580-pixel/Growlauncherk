package com.anzu.sdk;

import android.app.Activity;
import android.content.Context;
import android.view.View;

public class Anzu {
    public static void SetContext(Context context) {}
    public static void init(Activity activity, String gameId, AnzuInitListener listener) {}
    public static void startSession() {}
    public static void stopSession() {}
    public static void pause() {}
    public static void resume() {}
    public static View getAdView() { return null; }

    public interface AnzuInitListener {
        void onSuccess();
        void onError(String error);
    }
}
