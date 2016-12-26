package com.laudien.p1xelfehler.batterywarner.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.laudien.p1xelfehler.batterywarner.Contract;

import static android.content.Context.MODE_PRIVATE;
import static com.laudien.p1xelfehler.batterywarner.Contract.PREF_IS_ENABLED;
import static com.laudien.p1xelfehler.batterywarner.Contract.SHARED_PREFS;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) return;
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        if (sharedPreferences.getBoolean(Contract.PREF_FIRST_START, true))
            return; // return if intro was not finished

        boolean isChecked = sharedPreferences.getBoolean(PREF_IS_ENABLED, true);

        if (isChecked) {
            Log.i(TAG, "Finished Booting! Setting battery alarm...");
            new BatteryAlarmManager(context).checkBattery(true);
        }
    }
}
