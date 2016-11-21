package com.example.laudien.batterywarner.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import static android.content.Context.MODE_PRIVATE;
import static com.example.laudien.batterywarner.Contract.PREF_WARNING_LOW_ENABLED;
import static com.example.laudien.batterywarner.Contract.SHARED_PREFS;

public class DischargingReceiver extends BroadcastReceiver {
    private static final String TAG = "DischargingReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals("android.intent.action.ACTION_POWER_DISCONNECTED")) return;

        BatteryAlarmReceiver.cancelExistingAlarm(context);
        if (context.getSharedPreferences(SHARED_PREFS, MODE_PRIVATE).getBoolean(PREF_WARNING_LOW_ENABLED, true))
            BatteryAlarmReceiver.setRepeatingAlarm(context);
        Log.i(TAG, "User stopped charging!");
    }
}
