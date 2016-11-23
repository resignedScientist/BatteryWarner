package com.example.laudien.batterywarner.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.laudien.batterywarner.Contract;

public class DischargingReceiver extends BroadcastReceiver {
    private static final String TAG = "DischargingReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals("android.intent.action.ACTION_POWER_DISCONNECTED")) return;
        Log.i(TAG, "User stopped charging!");

        BatteryAlarmReceiver.cancelExistingAlarm(context);
        if (context.getSharedPreferences(Contract.SHARED_PREFS, Context.MODE_PRIVATE).getBoolean(Contract.PREF_WARNING_LOW_ENABLED, true))
            BatteryAlarmReceiver.setRepeatingAlarm(context, false);
    }
}
