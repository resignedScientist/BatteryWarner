package com.laudien.p1xelfehler.batterywarner.helper;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.services.BackgroundService;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.O;

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

    public static void restartService(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean firstStart = sharedPreferences.getBoolean(context.getString(R.string.pref_first_start), true);
        if (firstStart) {
            return;
        }
        Intent intent = getIntent(context);
        context.stopService(intent);
        startService(context, intent);
    }

    private static Intent getIntent(Context context) {
        return new Intent(context, BackgroundService.class);
    }
}
