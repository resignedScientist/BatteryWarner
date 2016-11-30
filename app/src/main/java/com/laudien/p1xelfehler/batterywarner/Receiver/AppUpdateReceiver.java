package com.laudien.p1xelfehler.batterywarner.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.laudien.p1xelfehler.batterywarner.Contract;

import static com.laudien.p1xelfehler.batterywarner.Contract.SHARED_PREFS;

public class AppUpdateReceiver extends BroadcastReceiver {

    private static final String TAG = "AppUpdateReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals("android.intent.action.MY_PACKAGE_REPLACED")) return;
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        if(sharedPreferences.getBoolean(Contract.PREF_FIRST_START, true)) return; // return if intro was not finished

        Log.i(TAG, "App has been upgraded! Starting alarms if activated...");

        boolean isCharging = BatteryAlarmReceiver.isCharging(context);

        if(isCharging && !sharedPreferences.getBoolean(Contract.PREF_WARNING_HIGH_ENABLED, true)) return;
        if(!isCharging && !sharedPreferences.getBoolean(Contract.PREF_WARNING_LOW_ENABLED, true)) return;

        BatteryAlarmReceiver.cancelExistingAlarm(context);
        new BatteryAlarmReceiver().onReceive(context, null);
    }
}
