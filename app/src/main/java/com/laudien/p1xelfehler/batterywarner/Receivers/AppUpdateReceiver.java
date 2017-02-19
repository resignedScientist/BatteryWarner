package com.laudien.p1xelfehler.batterywarner.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.laudien.p1xelfehler.batterywarner.BatteryAlarmManager;
import com.laudien.p1xelfehler.batterywarner.NotificationBuilder;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.RootChecker;
import com.laudien.p1xelfehler.batterywarner.Services.ChargingService;

public class AppUpdateReceiver extends BroadcastReceiver {

    //private static final String TAG = "AppUpdateReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals("android.intent.action.MY_PACKAGE_REPLACED")) return;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedPreferences.getBoolean(context.getString(R.string.pref_first_start), true))
            return; // return if intro was not finished

        Intent batteryStatus = BatteryAlarmManager.getBatteryStatus(context);
        if (batteryStatus == null) {
            return;
        }
        BatteryAlarmManager batteryAlarmManager = BatteryAlarmManager.getInstance(context);
        boolean isCharging = BatteryAlarmManager.isCharging(batteryStatus);
        if (isCharging) { // charging
            ChargingService.startService(context);
        } else { // discharging
            batteryAlarmManager.cancelDischargingAlarm(context);
            batteryAlarmManager.setDischargingAlarm(context);
        }
        batteryAlarmManager.checkAndNotify(context, batteryStatus);

        String pref_stop_charging = context.getString(R.string.pref_stop_charging);
        if (sharedPreferences.getBoolean(pref_stop_charging, false)) {
            if (!RootChecker.isDeviceRooted()) {
                sharedPreferences.edit().putBoolean(pref_stop_charging, false).apply();
                new NotificationBuilder(context).showNotification("Please grant root access again!", 0, true, false);
            }
        }
    }
}
