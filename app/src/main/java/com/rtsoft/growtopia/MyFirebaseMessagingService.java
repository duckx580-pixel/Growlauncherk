package com.rtsoft.growtopia;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class MyFirebaseMessagingService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
