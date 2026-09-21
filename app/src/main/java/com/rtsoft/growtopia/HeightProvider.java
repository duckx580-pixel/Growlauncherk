package com.rtsoft.growtopia;

import android.app.Activity;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.util.Log;
import android.view.DisplayCutout;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.PopupWindow;

public class HeightProvider extends PopupWindow implements ViewTreeObserver.OnGlobalLayoutListener {
    int lastKeyboardHeight;
    private HeightListener listener;
    private Activity mActivity;
    private View parentView;
    private View rootView;

    public interface HeightListener {
        void onHeightChanged(int height);
    }

    public HeightProvider(Activity activity) {
        super(activity);
        this.lastKeyboardHeight = -1;
        this.mActivity = activity;
        FrameLayout frameLayout = new FrameLayout(activity);
        this.rootView = frameLayout;
        setContentView(frameLayout);
        setBackgroundDrawable(new ColorDrawable(0));
        setWidth(0);
        setHeight(-1);
        setSoftInputMode(21);
        setInputMethodMode(1);
    }

    private int getTopCutoutHeight() {
        View decorView = this.mActivity.getWindow().getDecorView();
        int cutoutHeight = 0;
        if (decorView == null) return 0;
        WindowInsets insets = decorView.getRootWindowInsets();
        if (insets != null && Build.VERSION.SDK_INT >= 28) {
            DisplayCutout cutout = insets.getDisplayCutout();
            if (cutout != null) {
                for (Rect rect : cutout.getBoundingRects()) {
                    if (rect.top == 0) {
                        cutoutHeight += rect.bottom;
                    }
                }
            }
        }
        return cutoutHeight;
    }

    public void OnPause() {
        this.rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        dismiss();
    }

    public void OnResume() {
        View view = this.mActivity.findViewById(android.R.id.content);
        this.parentView = view;
        view.post(() -> {
            rootView.getViewTreeObserver().addOnGlobalLayoutListener(HeightProvider.this);
            if (!isShowing() && parentView.getWindowToken() != null) {
                showAtLocation(parentView, 0, 0, 0);
            }
        });
    }

    @Override
    public void onGlobalLayout() {
        Point point = new Point();
        this.mActivity.getWindowManager().getDefaultDisplay().getSize(point);
        Rect rect = new Rect();
        this.rootView.getWindowVisibleDisplayFrame(rect);
        if (this.mActivity.getResources().getConfiguration().orientation == 1) {
            return;
        }
        int keyboardHeight = (point.y + getTopCutoutHeight()) - rect.bottom;
        if (keyboardHeight != this.lastKeyboardHeight && this.listener != null) {
            this.listener.onHeightChanged(keyboardHeight);
        }
        this.lastKeyboardHeight = keyboardHeight;
    }

    public HeightProvider setHeightListener(HeightListener listener) {
        this.listener = listener;
        return this;
    }

    public ViewTreeObserver.OnGlobalLayoutListener getGlobalLayoutListener() {
        return this;
    }
}
