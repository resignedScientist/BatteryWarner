package com.example.laudien.batterywarner.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import static android.content.Context.MODE_PRIVATE;
import static com.example.laudien.batterywarner.SettingsActivity.PREF_WARNING_LOW_ENABLED;
import static com.example.laudien.batterywarner.SettingsActivity.SHARED_PREFS;

public class DischargingReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals("android.intent.action.ACTION_POWER_DISCONNECTED")) return;

        BatteryAlarmReceiver.cancelExistingAlarm(context);
        if (context.getSharedPreferences(SHARED_PREFS, MODE_PRIVATE).getBoolean(PREF_WARNING_LOW_ENABLED, true))
            BatteryAlarmReceiver.setRepeatingAlarm(context, false);
    }
}
