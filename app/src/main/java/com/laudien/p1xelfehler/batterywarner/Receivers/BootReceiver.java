package com.laudien.p1xelfehler.batterywarner.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;

import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.Services.ChargingService;

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

        Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus != null) {
            sharedPreferences.edit().putBoolean(Contract.PREF_ALREADY_NOTIFIED, false).apply();
            boolean isCharging = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, Contract.NO_STATE) != 0;
            if (isCharging) {
                sharedPreferences.edit().putBoolean(Contract.PREF_RESET_GRAPH, true).apply();
                context.startService(new Intent(context, ChargingService.class)); // start charging service if charging
            } else
                new BatteryAlarmManager(context).checkBattery(true);
        }
    }
}
