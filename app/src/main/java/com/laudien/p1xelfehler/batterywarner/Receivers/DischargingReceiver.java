package com.laudien.p1xelfehler.batterywarner.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.laudien.p1xelfehler.batterywarner.Contract;

import static com.laudien.p1xelfehler.batterywarner.Contract.SHARED_PREFS;

public class DischargingReceiver extends BroadcastReceiver {
    //private static final String TAG = "DischargingReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals("android.intent.action.ACTION_POWER_DISCONNECTED")) return;
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        if (sharedPreferences.getBoolean(Contract.PREF_FIRST_START, true))
            return; // return if intro was not finished

        //Log.i(TAG, "User stopped charging!");

        // set already shown to false
        sharedPreferences.edit().putBoolean(Contract.PREF_ALREADY_NOTIFIED, false).apply();

        // send broadcast
        Intent databaseIntent = new Intent();
        databaseIntent.setAction(Contract.BROADCAST_STATUS_CHANGED);
        context.sendBroadcast(databaseIntent);

        BatteryAlarmManager.cancelExistingAlarm(context);
        new BatteryAlarmManager(context).checkBattery(true);
    }
}
