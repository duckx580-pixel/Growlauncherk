package com.rtsoft.growtopia;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;

public class AppGLSurfaceView extends GLSurfaceView {
    private static boolean mMultiTouchClassAvailable;
    public static AppRenderer mRenderer;
    public SharedActivity app;

    public AppGLSurfaceView(Context context, SharedActivity activity) {
        super(context);
        setEGLContextClientVersion(2);
        setSystemUiVisibility(260);
        this.app = activity;
        if (SharedActivity.m_editText != null) {
            Log.d(SharedActivity.PackageName, "Setting focus options...");
            setFocusable(true);
            setFocusableInTouchMode(true);
            requestFocus();
        }
        setEGLConfigChooser(8, 8, 8, 8, 24, 8);
        setPreserveEGLContextOnPause(true);
        AppRenderer renderer = new AppRenderer(this.app);
        try {
            setRenderer(renderer);
            mRenderer = renderer;
            setRenderMode(RENDERMODE_CONTINUOUSLY);
        } catch (Exception e) {
            Log.e(SharedActivity.PackageName, "setRenderer failed: " + e.getMessage());
        }
        try {
            WrapSharedMultiTouchInput.checkAvailable(this.app);
            mMultiTouchClassAvailable = true;
        } catch (Throwable t) {
            mMultiTouchClassAvailable = false;
        }
    }

    public static native void nativeOnTouch(int action, float x, float y, int finger);
    public static native void nativePause();
    public static native void nativeResume();

    @Override
    public void onPause() {
        super.onPause();
        if (SharedActivity.bIsShuttingDown) return;
        nativePause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (SharedActivity.bIsShuttingDown) return;
        try { setSystemUiVisibility(260); } catch (Exception e) {}
        nativeResume();
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        super.onTouchEvent(motionEvent);
        if (mMultiTouchClassAvailable) {
            return WrapSharedMultiTouchInput.OnInput(motionEvent);
        }
        if (!Main.ZennKuyRenderer.nativeOnTouch((int) motionEvent.getX(), (int) motionEvent.getY(), motionEvent.getAction())) {
            nativeOnTouch(motionEvent.getAction(), motionEvent.getX(), motionEvent.getY(), 0);
        }
        return false;
    }
}
