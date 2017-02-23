package com.laudien.p1xelfehler.batterywarner.Receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.preference.PreferenceManager;

import com.laudien.p1xelfehler.batterywarner.Activities.MainActivity.GraphFragment;
import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.NotificationBuilder;
import com.laudien.p1xelfehler.batterywarner.R;

import java.util.Calendar;

public class DischargingAlarmReceiver extends BroadcastReceiver {

    private SharedPreferences sharedPreferences;
    private int warningLow, batteryLevel;

    public static void cancelDischargingAlarm(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        long intentTime = sharedPreferences.getLong(context.getString(R.string.pref_intent_time), Contract.NO_STATE);
        if (intentTime == Contract.NO_STATE) {
            return;
        }
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent batteryIntent = new Intent(context, DischargingAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) intentTime,
                batteryIntent,
                0
        );
        alarmManager.cancel(pendingIntent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus == null) {
            return;
        }
        boolean isEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_is_enabled), context.getResources().getBoolean(R.bool.pref_is_enabled_default));
        boolean isCharging = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, Contract.NO_STATE) != 0;
        boolean warningLowEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_warning_low_enabled), context.getResources().getBoolean(R.bool.pref_warning_low_enabled_default));
        if (isCharging && isEnabled && warningLowEnabled) {
            GraphFragment.notify(context);
            batteryLevel = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, Contract.NO_STATE);
            warningLow = sharedPreferences.getInt(context.getString(R.string.pref_warning_low), context.getResources().getInteger(R.integer.pref_warning_low_default));
            if (batteryLevel <= warningLow) { // warning low
                NotificationBuilder.showNotification(context, NotificationBuilder.NOTIFICATION_ID_WARNING_LOW);
            } else {
                setDischargingAlarm(context); // set new alarm
            }
        }
    }

    public void setDischargingAlarm(Context context) {
        long currentTime = Calendar.getInstance().getTimeInMillis();
        int interval;
        if (batteryLevel <= warningLow + context.getResources().getInteger(R.integer.difference_very_short)) {
            interval = context.getResources().getInteger(R.integer.interval_very_short);
        } else if (batteryLevel <= warningLow + context.getResources().getInteger(R.integer.difference_short)) {
            interval = context.getResources().getInteger(R.integer.interval_short);
        } else if (batteryLevel <= warningLow + context.getResources().getInteger(R.integer.difference_long)) {
            interval = context.getResources().getInteger(R.integer.interval_long);
        } else {
            interval = context.getResources().getInteger(R.integer.interval_very_long);
        }
        Intent batteryIntent = new Intent(context, DischargingAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) currentTime + interval, // request code = alarm time
                batteryIntent,
                0
        );
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setExact(
                AlarmManager.RTC,
                currentTime + interval,
                pendingIntent
        );
        sharedPreferences.edit().putLong(context.getString(R.string.pref_intent_time), currentTime + interval).apply();
    }
}
