package com.laudien.p1xelfehler.batterywarner.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.laudien.p1xelfehler.batterywarner.Contract;

import static com.laudien.p1xelfehler.batterywarner.Contract.SHARED_PREFS;

public class DischargingReceiver extends BroadcastReceiver {
    private static final String TAG = "DischargingReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals("android.intent.action.ACTION_POWER_DISCONNECTED")) return;
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        if(!sharedPreferences.getBoolean(Contract.PREF_FIRST_START, false)) return; // return if intro was not finished

        Log.i(TAG, "User stopped charging!");

        BatteryAlarmReceiver.cancelExistingAlarm(context);
        if (context.getSharedPreferences(Contract.SHARED_PREFS, Context.MODE_PRIVATE).getBoolean(Contract.PREF_WARNING_LOW_ENABLED, true))
            new BatteryAlarmReceiver().onReceive(context, intent);
    }
}
