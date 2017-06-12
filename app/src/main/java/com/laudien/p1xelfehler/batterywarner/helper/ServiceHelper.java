package com.laudien.p1xelfehler.batterywarner.helper;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ServiceHelper {
    public static void startForegroundService(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }
}
