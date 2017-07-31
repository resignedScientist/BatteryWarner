package com.laudien.p1xelfehler.batterywarner.helper;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.annotation.Nullable;

import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.services.BackgroundService;

public class ServiceHelper {
    public static void startService(Context context, SharedPreferences sharedPreferences) {
        boolean isEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_is_enabled), context.getResources().getBoolean(R.bool.pref_is_enabled_default));
        if (isEnabled) {
            startService(context, new Intent(context, BackgroundService.class));
        }
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

    public static void restartDischargingService(Context context, @Nullable SharedPreferences sharedPreferences) {
        stopService(context);
        startService(context, sharedPreferences);
    }
}
