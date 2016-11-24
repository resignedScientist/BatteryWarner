package com.example.laudien.batterywarner.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import static android.content.Context.MODE_PRIVATE;
import static com.example.laudien.batterywarner.Contract.PREF_IS_ENABLED;
import static com.example.laudien.batterywarner.Contract.SHARED_PREFS;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) return;

        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        boolean isChecked = sharedPreferences.getBoolean(PREF_IS_ENABLED, true);

        if (isChecked) {
            new BatteryAlarmReceiver().onReceive(context, intent);
            Log.i(TAG, "Finished Booting! Setting battery alarm...");
        }
    }
}
