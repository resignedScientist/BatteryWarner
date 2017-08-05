package com.laudien.p1xelfehler.batterywarner.helper;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.laudien.p1xelfehler.batterywarner.services.BackgroundService;

public class ServiceHelper {
    public static void startService(Context context) {
        startService(context, new Intent(context, BackgroundService.class));
    }

    public static void startService(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    private static void stopService(Context context) {
        context.stopService(new Intent(context, BackgroundService.class));
    }

    public static void restartService(Context context) {
        stopService(context);
        startService(context);
    }
}
