package com.laudien.p1xelfehler.batterywarner.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.laudien.p1xelfehler.batterywarner.Contract;

import static android.content.Context.MODE_PRIVATE;
import static com.laudien.p1xelfehler.batterywarner.Contract.SHARED_PREFS;

public class BootReceiver extends BroadcastReceiver {
    //private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) return;
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        if (sharedPreferences.getBoolean(Contract.PREF_FIRST_START, true))
            return; // return if intro was not finished

        //Log.i(TAG, "Finished Booting! Setting battery alarm...");

        sharedPreferences.edit().putBoolean(Contract.PREF_ALREADY_NOTIFIED, false).apply();
        new BatteryAlarmManager(context).checkBattery(true);
    }
}
