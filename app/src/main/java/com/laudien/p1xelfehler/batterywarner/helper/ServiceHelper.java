package com.laudien.p1xelfehler.batterywarner.helper;

import android.content.Context;
import android.content.Intent;

import com.laudien.p1xelfehler.batterywarner.services.BackgroundService;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.O;
import static com.laudien.p1xelfehler.batterywarner.services.BackgroundService.ACTION_RESET_ALL;

public class ServiceHelper {
    public static void startService(Context context) {
        startService(context, getIntent(context));
    }

    public static void startService(Context context, Intent intent) {
        if (SDK_INT >= O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void resetService(Context context) {
        Intent intent = getIntent(context);
        intent.setAction(ACTION_RESET_ALL);
        startService(context, intent);
    }

    public static void restartService(Context context) {
        Intent intent = getIntent(context);
        context.stopService(intent);
        startService(context, intent);
    }

    private static Intent getIntent(Context context) {
        return new Intent(context, BackgroundService.class);
    }
}
