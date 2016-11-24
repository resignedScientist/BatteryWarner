package com.example.laudien.batterywarner.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.laudien.batterywarner.Contract;

public class AppUpdateReceiver extends BroadcastReceiver {

    private static final String TAG = "AppUpdateReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals("android.intent.action.MY_PACKAGE_REPLACED")) return;

        Log.i(TAG, "App has been upgraded! Starting alarms if activated...");

        SharedPreferences sharedPreferences = context.getSharedPreferences(Contract.SHARED_PREFS, Context.MODE_PRIVATE);
        boolean isCharging = BatteryAlarmReceiver.isCharging(context);

        if(isCharging && !sharedPreferences.getBoolean(Contract.PREF_WARNING_HIGH_ENABLED, true)) return;
        if(!isCharging && !sharedPreferences.getBoolean(Contract.PREF_WARNING_LOW_ENABLED, true)) return;

        BatteryAlarmReceiver.cancelExistingAlarm(context);
        new BatteryAlarmReceiver().onReceive(context, null);
    }
}
