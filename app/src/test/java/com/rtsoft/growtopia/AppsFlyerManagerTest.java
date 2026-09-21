package com.rtsoft.growtopia;

import android.app.Application;
import android.os.Looper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class AppsFlyerManagerTest {
    @Test public void startupDoesNotReenterNativeController() {
        int[] callbacks = {0};
        AppsFlyerManager manager = new AppsFlyerManager(RuntimeEnvironment.getApplication()) {
            @Override void nativeOnStarted(int result) {
                assertEquals(0, result);
                callbacks[0]++;
            }
        };
        manager.Start(true, true);
        assertEquals(0, callbacks[0]);
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(0, callbacks[0]);
    }
}
