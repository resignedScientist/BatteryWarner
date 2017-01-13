package com.laudien.p1xelfehler.batterywarner.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.laudien.p1xelfehler.batterywarner.BatteryAlarmManager;
import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.Services.ChargingService;

public class AppUpdateReceiver extends BroadcastReceiver {

    //private static final String TAG = "AppUpdateReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals("android.intent.action.MY_PACKAGE_REPLACED")) return;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedPreferences.getBoolean(context.getString(R.string.pref_first_start), true))
            return; // return if intro was not finished

        Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus == null) {
            return;
        }
        BatteryAlarmManager batteryAlarmManager = BatteryAlarmManager.getInstance(context);
        boolean isCharging = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, Contract.NO_STATE) != 0;
        if (isCharging) { // charging
            context.startService(new Intent(context, ChargingService.class)); // start charging service if charging
        } else { // discharging
            batteryAlarmManager.cancelDischargingAlarm(context);
            batteryAlarmManager.setDischargingAlarm(context);
        }
        batteryAlarmManager.checkAndNotify(context, batteryStatus);
    }
}
