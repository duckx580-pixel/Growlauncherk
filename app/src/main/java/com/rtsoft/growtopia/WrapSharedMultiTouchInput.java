package com.rtsoft.growtopia;

import android.view.MotionEvent;

class WrapSharedMultiTouchInput {
    private SharedMultiTouchInput mInstance;

    public static boolean OnInput(MotionEvent event) {
        return SharedMultiTouchInput.OnInput(event);
    }

    public static void checkAvailable(SharedActivity activity) {
        SharedMultiTouchInput.init(activity);
    }
}
