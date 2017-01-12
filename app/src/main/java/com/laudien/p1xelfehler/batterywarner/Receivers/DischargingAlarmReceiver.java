package com.laudien.p1xelfehler.batterywarner.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.laudien.p1xelfehler.batterywarner.Contract;

public class DischargingAlarmReceiver extends BroadcastReceiver {
    Context context;
    SharedPreferences sharedPreferences;
    BatteryAlarmManager batteryAlarmManager;
    int batteryLevel;

    @Override
    public void onReceive(Context context, Intent intent) {
        batteryAlarmManager = BatteryAlarmManager.getInstance(context);
        sharedPreferences = context.getSharedPreferences(Contract.SHARED_PREFS, Context.MODE_PRIVATE);
        if (!BatteryAlarmManager.isDischargingNotificationEnabled(context, sharedPreferences)) {
            return; // return if disabled in settings
        }
        this.context = context;
        batteryAlarmManager.checkAndNotify(context);
        batteryLevel = batteryAlarmManager.getBatteryLevel();
        batteryAlarmManager.setDischargingAlarm(context); // set new alarm
    }
}
