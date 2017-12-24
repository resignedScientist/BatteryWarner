package com.laudien.p1xelfehler.batterywarner.helper;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.laudien.p1xelfehler.batterywarner.services.BackgroundService;

import static com.laudien.p1xelfehler.batterywarner.services.BackgroundService.ACTION_RESET_ALL;

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

    public static void resetService(Context context) {
        Intent intent = new Intent(context, BackgroundService.class);
        intent.setAction(ACTION_RESET_ALL);
        startService(context, intent);
    }
}
