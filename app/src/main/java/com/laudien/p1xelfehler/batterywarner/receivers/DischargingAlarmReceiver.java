package com.laudien.p1xelfehler.batterywarner.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper;

import java.util.Calendar;

import static android.content.Context.MODE_PRIVATE;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.KITKAT;

/**
 * A BroadcastReceiver called by the AlarmManager of the system.
 * It sets itself in shorter and shorter time distances depending on the difference to the
 * low battery warning percentage the user set in the settings.
 * Triggers the battery low warning notification if the battery has reached the warning percentage.
 */
public class DischargingAlarmReceiver extends BroadcastReceiver {
    private final String TAG = getClass().getSimpleName();
    private SharedPreferences sharedPreferences;
    private int warningLow, batteryLevel;

    /**
     * Cancels the existing discharging alarm.
     *
     * @param context An instance of the Context class.
     */
    public static void cancelDischargingAlarm(Context context) {
        SharedPreferences temporaryPrefs = context.getSharedPreferences(context.getString(R.string.prefs_temporary), MODE_PRIVATE);
        long intentTime = temporaryPrefs.getLong(context.getString(R.string.pref_intent_time), -1);
        if (intentTime == -1) {
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
        Intent batteryStatus = context.getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus == null) {
            return;
        }
        boolean isEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_is_enabled), context.getResources().getBoolean(R.bool.pref_is_enabled_default));
        boolean isCharging = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) != 0;
        boolean warningLowEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_warning_low_enabled), context.getResources().getBoolean(R.bool.pref_warning_low_enabled_default));
        if (!isCharging && isEnabled && warningLowEnabled) {
            batteryLevel = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
            warningLow = sharedPreferences.getInt(context.getString(R.string.pref_warning_low), context.getResources().getInteger(R.integer.pref_warning_low_default));
            if (batteryLevel <= warningLow) { // warning low
                NotificationHelper.showNotification(context, NotificationHelper.ID_WARNING_LOW);
                Log.d(TAG, "Discharging notification triggered!");
            } else {
                setDischargingAlarm(context); // set new alarm
            }
        }
    }

    private void setDischargingAlarm(Context context) {
        boolean dischargingServiceEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_discharging_service_enabled), context.getResources().getBoolean(R.bool.pref_discharging_service_enabled_default));
        if (!dischargingServiceEnabled) {
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
            long triggerTime = currentTime + interval;
            Intent batteryIntent = new Intent(context, DischargingAlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    (int) triggerTime, // request code = alarm time
                    batteryIntent,
                    0
            );
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (SDK_INT >= KITKAT) {
                alarmManager.setExact(
                        AlarmManager.RTC,
                        triggerTime,
                        pendingIntent
                );
            } else {
                alarmManager.set(
                        AlarmManager.RTC,
                        triggerTime,
                        pendingIntent
                );
            }
            SharedPreferences temporaryPrefs = context.getSharedPreferences(context.getString(R.string.prefs_temporary), MODE_PRIVATE);
            temporaryPrefs
                    .edit()
                    .putLong(context.getString(R.string.pref_intent_time), triggerTime)
                    .apply();
            Log.d(TAG, "Discharging alarm set to: " + triggerTime);
        }
    }
}