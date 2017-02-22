package com.laudien.p1xelfehler.batterywarner;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.preference.PreferenceManager;

import com.laudien.p1xelfehler.batterywarner.Activities.MainActivity.GraphFragment;
import com.laudien.p1xelfehler.batterywarner.Receivers.DischargingAlarmReceiver;

import java.util.Calendar;

public class BatteryAlarmManager {
    // private static final String TAG = "BatteryAlarmManager";
    private static BatteryAlarmManager instance;
    private SharedPreferences sharedPreferences;
    private int batteryLevel, temperature, lastPercentage;

    private BatteryAlarmManager(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static BatteryAlarmManager getInstance(Context context) {
        if (instance == null) {
            instance = new BatteryAlarmManager(context);
        }
        return instance;
    }

    public static boolean isDischargingNotificationEnabled(Context context, SharedPreferences sharedPreferences) {
        // check if discharging and notification is enabled
        Intent batteryStatus = getBatteryStatus(context);
        return batteryStatus != null
                && !isCharging(batteryStatus)
                && sharedPreferences.getBoolean(context.getString(R.string.pref_is_enabled), context.getResources().getBoolean(R.bool.pref_is_enabled_default))
                && sharedPreferences.getBoolean(context.getString(R.string.pref_warning_low_enabled), context.getResources().getBoolean(R.bool.pref_warning_low_enabled_default));
    }

    public static boolean isCharging(Intent batteryStatus) {
        return batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, Contract.NO_STATE) != 0;
    }

    public static Intent getBatteryStatus(Context context) {
        return context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    public boolean isChargingTypeEnabled(Context context, Intent batteryStatus) {
        int chargingType = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, Contract.NO_STATE);
        switch (chargingType) {
            case BatteryManager.BATTERY_PLUGGED_AC: // ac charging
                if (!sharedPreferences.getBoolean(context.getString(R.string.pref_ac_enabled), context.getResources().getBoolean(R.bool.pref_ac_enabled_default))) {
                    return false;
                }
                break;
            case BatteryManager.BATTERY_PLUGGED_USB: // usb charging
                if (!sharedPreferences.getBoolean(context.getString(R.string.pref_usb_enabled), context.getResources().getBoolean(R.bool.pref_usb_enabled_default))) {
                    return false;
                }
                break;
            case BatteryManager.BATTERY_PLUGGED_WIRELESS: // wireless charging
                if (!sharedPreferences.getBoolean(context.getString(R.string.pref_wireless_enabled), context.getResources().getBoolean(R.bool.pref_wireless_enabled_default))) {
                    return false;
                }
        }
        return true;
    }

    public boolean isChargingNotificationEnabled(Context context, Intent batteryStatus) {
        return !(!BatteryAlarmManager.isCharging(batteryStatus) // if not charging
                || !sharedPreferences.getBoolean(context.getString(R.string.pref_is_enabled), context.getResources().getBoolean(R.bool.pref_is_enabled_default)) // if not enabled
                || !isChargingTypeEnabled(context, batteryStatus) // if charging type is disabled
                || (!sharedPreferences.getBoolean(context.getString(R.string.pref_graph_enabled), context.getResources().getBoolean(R.bool.pref_graph_enabled_default))
                && !sharedPreferences.getBoolean(context.getString(R.string.pref_warning_high_enabled), context.getResources().getBoolean(R.bool.pref_warning_high_enabled_default))));
    }

    public void checkAndNotify(Context context, Intent batteryStatus) {
        if (batteryStatus == null) {
            return;
        }
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        batteryLevel = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, Contract.NO_STATE);
        if (Contract.IS_PRO) {
            temperature = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, Contract.NO_STATE);
        }

        if (batteryLevel >= sharedPreferences.getInt(context.getString(R.string.pref_warning_high), context.getResources().getInteger(R.integer.pref_warning_high_default))) { // warning high
            NotificationBuilder.showNotification(context, NotificationBuilder.NOTIFICATION_ID_WARNING_HIGH);
        } else if (batteryLevel <= sharedPreferences.getInt(context.getString(R.string.pref_warning_low), context.getResources().getInteger(R.integer.pref_warning_low_default))) { // warning low
            NotificationBuilder.showNotification(context, NotificationBuilder.NOTIFICATION_ID_WARNING_LOW);
        }
    }

    public void logInDatabase(Context context) {
        boolean graphEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_graph_enabled), context.getResources().getBoolean(R.bool.pref_graph_enabled_default));
        if (Contract.IS_PRO && graphEnabled && batteryLevel != lastPercentage) {
            GraphDbHelper graphDbHelper = GraphDbHelper.getInstance(context);
            long timeNow = Calendar.getInstance().getTimeInMillis();
            graphDbHelper.addValue(timeNow, batteryLevel, temperature);
            lastPercentage = batteryLevel;
            GraphFragment.notify(context);
        }
    }

    public void setDischargingAlarm(Context context) {
        if (!isDischargingNotificationEnabled(context, sharedPreferences)) {
            return; // return if disabled in settings
        }
        long currentTime = Calendar.getInstance().getTimeInMillis();
        int interval;
        int warningLow = sharedPreferences.getInt(context.getString(R.string.pref_warning_low), context.getResources().getInteger(R.integer.pref_warning_low_default));
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

    public void cancelDischargingAlarm(Context context) {
        long intentTime = sharedPreferences.getLong(context.getString(R.string.pref_intent_time), Contract.NO_STATE);
        if (intentTime == Contract.NO_STATE) {
            return;
        }
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent batteryIntent = new Intent(context, BatteryAlarmManager.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) intentTime,
                batteryIntent,
                0
        );
        alarmManager.cancel(pendingIntent);
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }
}
