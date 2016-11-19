package com.example.laudien.batterywarner.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import static android.content.Context.MODE_PRIVATE;
import static com.example.laudien.batterywarner.Activities.SettingsActivity.PREF_WARNING_HIGH_ENABLED;
import static com.example.laudien.batterywarner.Activities.SettingsActivity.SHARED_PREFS;

public class ChargingReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals("android.intent.action.ACTION_POWER_CONNECTED")) return;

        BatteryAlarmReceiver.cancelExistingAlarm(context);
        if (context.getSharedPreferences(SHARED_PREFS, MODE_PRIVATE).getBoolean(PREF_WARNING_HIGH_ENABLED, true))
            BatteryAlarmReceiver.setRepeatingAlarm(context, true);
    }
}
